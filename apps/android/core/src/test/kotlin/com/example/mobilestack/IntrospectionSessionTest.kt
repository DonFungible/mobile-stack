package com.example.mobilestack

import com.example.mobilestack.introspection.IntrospectionAction
import com.example.mobilestack.introspection.IntrospectionNode
import com.example.mobilestack.introspection.IntrospectionSession
import com.example.mobilestack.introspection.objectSchema
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.Test

class IntrospectionSessionTest {
    @Serializable
    private data class SetInput(val value: Int)

    private class Leaf : IntrospectionNode {
        var count = 0
        override val name = "Leaf"
        override val state get() = buildJsonObject { put("count", count) }
        override val actions
            get() = listOf(
                IntrospectionAction("Increment") { count += 1 },
                IntrospectionAction.withInput("Set", SetInput.serializer(), objectSchema("value" to "integer")) {
                    count = it.value
                },
            )
    }

    private class Container : IntrospectionNode {
        val leaf = Leaf()
        var closed = false
        override val name = "Container"
        override val state = JsonObject(emptyMap())
        override val actions get() = listOf(
            IntrospectionAction("close") {
                closed = true
            },
            IntrospectionAction("hidden") {},
        )
        override val lentActions get() = actions.take(1)
        override val child get() = if (closed) null else leaf
    }

    private fun session(root: Container = Container()) = IntrospectionSession({ root })

    private fun IntrospectionSession.json(request: String) = Json.parseToJsonElement(handle(request)).jsonObject

    @Test
    fun `ls describes the deepest node with lent actions`() {
        val node = session().json("""{"id":1,"command":"ls"}""").getValue("node").jsonObject

        assertEquals("Leaf", node.getValue("name").jsonPrimitive.content)
        assertEquals(listOf("Container", "Leaf"), node.getValue("path").jsonArray.map { it.jsonPrimitive.content })
        assertEquals("""{"count":0}""", node.getValue("state").toString())
        assertEquals(
            listOf("Increment", "Set", "close"),
            node.getValue("actions").jsonArray.map { it.jsonObject.getValue("title").jsonPrimitive.content },
        )
        assertEquals(
            """{"type":"object","properties":{"value":{"type":"integer"}},"required":["value"]}""",
            node.getValue("actions").jsonArray[1].jsonObject.getValue("input").toString(),
        )
    }

    @Test
    fun `do runs an action and answers with fresh state`() {
        val answer = session().json("""{"id":7,"command":"do","action":"Set","payload":{"value":5}}""")

        assertEquals(7, answer.getValue("id").jsonPrimitive.content.toInt())
        assertEquals("""{"count":5}""", answer.getValue("node").jsonObject.getValue("state").toString())
    }

    @Test
    fun `an action that is not lent is refused with what is available`() {
        val error = session().json("""{"id":1,"command":"do","action":"hidden"}""").getValue("error").jsonObject

        assertEquals("unknown_action", error.getValue("code").jsonPrimitive.content)
        assertEquals(
            listOf("Increment", "Set", "close"),
            error.getValue("available").jsonArray.map { it.jsonPrimitive.content },
        )
    }

    @Test
    fun `a malformed payload never reaches the node`() {
        for (payload in listOf("""{"value":"five"}""", "{}", "null")) {
            val root = Container()
            val answer = session(root).json("""{"id":1,"command":"do","action":"Set","payload":$payload}""")

            assertEquals("invalid_payload", answer.getValue("error").jsonObject.getValue("code").jsonPrimitive.content)
            assertEquals(0, root.leaf.count)
        }
    }

    @Test
    fun `focus follows the tree as it changes`() {
        val session = session()
        session.handle("do close")
        assertTrue(session.handle("ls").startsWith("Container\n"))
    }

    @Test
    fun `the plain form reads like a terminal`() {
        val session = session()
        assertEquals("", session.handle("do Increment"))
        assertEquals(
            "Leaf\n  state   {\"count\":1}\n  actions Increment, Set {value}, close",
            session.handle("ls"),
        )
        assertEquals("error: Nope is not available here. Available: Increment, Set, close", session.handle("do Nope"))
    }
}
