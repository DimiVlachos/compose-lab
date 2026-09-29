package dev.dimvlachos.lab.agenticdemo.data

import androidx.a2ui.model.protocol.A2uiClientErrorMessage
import androidx.a2ui.model.protocol.A2uiClientEventMessage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import org.json.JSONObject

class A2uiWireMapperTest {
    @Test
    fun anEventBecomesAV09ActionWithWireFieldNames() {
        val event =
            A2uiClientEventMessage(
                type = "book",
                surfaceId = "booking_1",
                componentId = "book_btn",
                timestamp = 1_790_000_000_000,
                context = mapOf("name" to "Ada", "party" to 4, "diet" to listOf("vegetarian")),
            )

        val wire = JSONObject(A2uiWireMapper.toWire(event))

        assertEquals("v0.9", wire.getString("version"))
        val action = wire.getJSONObject("action")
        assertEquals("book", action.getString("name"))
        assertEquals("booking_1", action.getString("surfaceId"))
        assertEquals("book_btn", action.getString("sourceComponentId"))
        assertEquals("2026-09-21T14:13:20Z", action.getString("timestamp"))
        val context = action.getJSONObject("context")
        assertEquals("Ada", context.getString("name"))
        assertEquals(4, context.getInt("party"))
        assertEquals("vegetarian", context.getJSONArray("diet").getString(0))
        assertFalse(wire.has("metadata"))
    }

    @Test
    fun aValidationErrorLiftsItsPathBesideTheMessage() {
        val error =
            A2uiClientErrorMessage(
                code = "VALIDATION_FAILED",
                surfaceId = "booking_1",
                message = "Unknown component 'Stepper'",
                context = mapOf("path" to "/components/3"),
            )

        val body = JSONObject(A2uiWireMapper.toWire(error)).getJSONObject("error")

        assertEquals("VALIDATION_FAILED", body.getString("code"))
        assertEquals("booking_1", body.getString("surfaceId"))
        assertEquals("/components/3", body.getString("path"))
        assertFalse(body.has("context"))
    }

    @Test
    fun otherErrorsKeepTheirContextKeys() {
        val error =
            A2uiClientErrorMessage(
                code = "RUNTIME_ERROR",
                surfaceId = "options_1",
                message = "boom",
                context = mapOf("componentId" to "card_2"),
            )

        val body = JSONObject(A2uiWireMapper.toWire(error)).getJSONObject("error")

        assertEquals("card_2", body.getString("componentId"))
    }
}
