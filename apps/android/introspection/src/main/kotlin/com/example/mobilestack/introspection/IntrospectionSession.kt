package com.example.mobilestack.introspection

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

/**
 * Answers the two commands of the protocol, `ls` and `do`, against a tree of nodes.
 *
 * A line that starts with `{` is a JSON request and gets a one-line JSON response. Anything else is the
 * plain form (`ls`, `do Action {"json": true}`) and gets plain text, which is what you type by hand.
 */
class IntrospectionSession(
    private val root: () -> IntrospectionNode,
    private val violations: () -> List<String> = { emptyList() },
) {
    fun handle(line: String): String {
        val trimmed = line.trim()
        return if (trimmed.startsWith("{")) handleJson(trimmed) else handlePlain(trimmed)
    }

    private class Focus(val node: IntrospectionNode, val path: List<String>, val actions: List<IntrospectionAction>)

    private class Failure(val code: String, override val message: String, val available: List<String>? = null) :
        Exception()

    /** The deepest node, plus its own actions and those its ancestors lend it. Nothing else is reachable. */
    private fun focus(): Focus {
        var node = root()
        val path = mutableListOf(node.name)
        var lent = emptyList<IntrospectionAction>()
        while (true) {
            val child = node.child ?: break
            lent = node.lentActions + lent
            node = child
            path += node.name
        }
        return Focus(node, path, node.actions + lent)
    }

    private fun perform(name: String, payload: JsonElement?) {
        val focus = focus()
        val action = focus.actions.firstOrNull { it.title == name } ?: throw Failure(
            code = "unknown_action",
            message = "$name is not available here. Available: ${focus.actions.joinToString { it.title }}",
            available = focus.actions.map { it.title },
        )
        try {
            action.run(payload?.takeUnless { it is JsonNull })
        } catch (error: InvalidPayload) {
            throw Failure("invalid_payload", error.reason)
        }
    }

    private fun handleJson(line: String): String {
        val request = try {
            Json.parseToJsonElement(line).jsonObject
        } catch (_: SerializationException) {
            return failureJson(JsonNull, Failure("bad_request", "request is not valid JSON"))
        } catch (_: IllegalArgumentException) {
            return failureJson(JsonNull, Failure("bad_request", "request is not valid JSON"))
        }
        val id = request["id"] ?: JsonNull
        try {
            when ((request["command"] as? JsonPrimitive)?.content) {
                "ls" -> Unit

                "do" -> {
                    val action = (request["action"] as? JsonPrimitive)?.takeIf { it.isString }?.content
                        ?: throw Failure("bad_request", "do needs an action")
                    perform(action, request["payload"])
                }

                else -> throw Failure("bad_request", "command must be ls or do")
            }
        } catch (failure: Failure) {
            return failureJson(id, failure)
        }
        return buildJsonObject {
            put("id", id)
            put("ok", true)
            put("node", nodeJson(focus()))
            put("violations", buildJsonArray { violations().forEach { add(JsonPrimitive(it)) } })
        }.toString()
    }

    private fun nodeJson(focus: Focus): JsonObject = buildJsonObject {
        put("name", focus.node.name)
        put("path", buildJsonArray { focus.path.forEach { add(JsonPrimitive(it)) } })
        put("state", focus.node.state)
        put(
            "actions",
            buildJsonArray {
                focus.actions.forEach { action ->
                    add(
                        buildJsonObject {
                            put("title", action.title)
                            action.inputSchema?.let { put("input", it) }
                        },
                    )
                }
            },
        )
    }

    private fun failureJson(id: JsonElement, failure: Failure): String = buildJsonObject {
        put("id", id)
        put("ok", false)
        put(
            "error",
            buildJsonObject {
                put("code", failure.code)
                put("message", failure.message)
                failure.available?.let { names ->
                    put("available", buildJsonArray { names.forEach { add(JsonPrimitive(it)) } })
                }
            },
        )
    }.toString()

    private fun handlePlain(line: String): String {
        val parts = line.split(" ", limit = 3)
        return when {
            parts[0] == "ls" -> describe(focus())

            parts[0] == "do" && parts.size >= 2 -> {
                val payload = parts.getOrNull(2)?.let {
                    try {
                        Json.parseToJsonElement(it)
                    } catch (_: SerializationException) {
                        return "error: payload is not valid JSON"
                    }
                }
                try {
                    perform(parts[1], payload)
                    ""
                } catch (failure: Failure) {
                    "error: ${failure.message}"
                }
            }

            else -> "error: commands are `ls` and `do Action [json]`"
        }
    }

    private fun describe(focus: Focus): String {
        val actions = focus.actions.joinToString { action ->
            val properties = action.inputSchema?.get("properties") as? JsonObject
            if (properties == null) action.title else "${action.title} {${properties.keys.sorted().joinToString()}}"
        }
        return "${focus.node.name}\n  state   ${focus.node.state}\n  actions $actions"
    }
}
