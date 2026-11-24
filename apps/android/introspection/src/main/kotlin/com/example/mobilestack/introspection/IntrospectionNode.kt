package com.example.mobilestack.introspection

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

/** A view model, navigator or app root as seen from outside: a name, its state as JSON, and what a user could do. */
interface IntrospectionNode {
    val name: String

    /** Every field a user could see. */
    val state: JsonObject

    /** One entry per action, every time. Whether an action does anything is the view model's decision. */
    val actions: List<IntrospectionAction>

    /** Actions a container lends to whichever descendant is focused, such as a stack's `goBack`. */
    val lentActions: List<IntrospectionAction> get() = emptyList()

    /** The node below this one. Following `child` from the root finds the focused node. */
    val child: IntrospectionNode? get() = null
}

class InvalidPayload(val reason: String) : Exception(reason)

class IntrospectionAction private constructor(
    val title: String,
    val inputSchema: JsonObject?,
    private val handler: (JsonElement?) -> Unit,
) {
    constructor(title: String, run: () -> Unit) : this(title, null, { run() })

    internal fun run(payload: JsonElement?) = handler(payload)

    companion object {
        /** The payload is decoded into [Input] before [run] is called, so a malformed one never reaches the view model. */
        fun <Input> withInput(
            title: String,
            serializer: DeserializationStrategy<Input>,
            schema: JsonObject,
            run: (Input) -> Unit,
        ) = IntrospectionAction(title, schema) { payload ->
            if (payload == null) throw InvalidPayload("$title needs a JSON payload")
            val input = try {
                strict.decodeFromJsonElement(serializer, payload)
            } catch (_: SerializationException) {
                throw InvalidPayload("$title payload does not match its schema")
            } catch (_: IllegalArgumentException) {
                throw InvalidPayload("$title payload does not match its schema")
            }
            run(input)
        }

        private val strict = Json { isLenient = false }
    }
}

/** Builds an object schema whose properties are all required. */
fun objectSchema(vararg properties: Pair<String, String>): JsonObject = buildJsonObject {
    put("type", "object")
    putJsonObject("properties") {
        properties.forEach { (name, type) -> putJsonObject(name) { put("type", type) } }
    }
    put(
        "required",
        buildJsonArray {
            properties.map { it.first }.sorted().forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) }
        },
    )
}
