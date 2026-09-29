package dev.dimvlachos.lab.agenticdemo.data

import androidx.a2ui.model.protocol.A2uiClientErrorMessage
import androidx.a2ui.model.protocol.A2uiClientEventMessage
import androidx.a2ui.model.protocol.A2uiClientToServerMessage
import java.time.Instant
import org.json.JSONArray
import org.json.JSONObject

/**
 * The renderer hands outbound events over as Kotlin objects with no serializer, and their field
 * names are not the wire's (`componentId` is `sourceComponentId`, the timestamp is ISO-8601, a
 * validation error's path sits beside its message). This writes the A2UI v0.9 client-to-server JSON
 * the agent was told to expect.
 */
object A2uiWireMapper {
    private const val VERSION = "v0.9"

    fun toWire(message: A2uiClientToServerMessage): String =
        when (message) {
            is A2uiClientEventMessage -> action(message)
            is A2uiClientErrorMessage -> error(message)
        }.toString()

    private fun action(event: A2uiClientEventMessage): JSONObject {
        val wire =
            JSONObject()
                .put("version", VERSION)
                .put(
                    "action",
                    JSONObject()
                        .put("name", event.type)
                        .put("surfaceId", event.surfaceId)
                        .put("sourceComponentId", event.componentId)
                        .put("timestamp", Instant.ofEpochMilli(event.timestamp).toString())
                        .put("context", toJson(event.context)),
                )
        // Surfaces created with sendDataModel attach their data model. A2UI carries it as transport
        // metadata, so it rides beside the action rather than inside it.
        event.clientDataModel?.let { wire.put("metadata", toJson(it.toPayloadMap())) }
        return wire
    }

    private fun error(error: A2uiClientErrorMessage): JSONObject {
        val body =
            JSONObject()
                .put("code", error.code)
                .put("surfaceId", error.surfaceId)
                .put("message", error.message)
        if (error.code == "VALIDATION_FAILED") {
            body.put("path", error.context["path"])
        } else {
            error.context.forEach { (key, value) -> body.put(key, toJson(value)) }
        }
        return JSONObject().put("version", VERSION).put("error", body)
    }

    private fun toJson(value: Any?): Any =
        when (value) {
            null -> JSONObject.NULL
            is Map<*, *> ->
                JSONObject().apply { value.forEach { (k, v) -> put(k.toString(), toJson(v)) } }
            is Iterable<*> -> JSONArray().apply { value.forEach { put(toJson(it)) } }
            is Array<*> -> JSONArray().apply { value.forEach { put(toJson(it)) } }
            else -> value
        }
}
