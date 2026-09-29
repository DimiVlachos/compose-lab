package dev.dimvlachos.lab.agenticdemo.data

import androidx.a2ui.model.processor.A2uiJsonMessageParser
import androidx.a2ui.model.processor.A2uiJsonReader
import androidx.a2ui.model.processor.A2uiJsonToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import java.io.StringReader

/** The library's A2UI parser, reading through Gson instead of the (stubbed) android.util one. */
val JvmA2uiParser = A2uiJsonMessageParser { json -> GsonA2uiJsonReader(json) }

private class GsonA2uiJsonReader(json: String) : A2uiJsonReader {
    private val delegate = JsonReader(StringReader(json))

    override fun peek(): A2uiJsonToken =
        when (delegate.peek()) {
            JsonToken.BEGIN_OBJECT -> A2uiJsonToken.BEGIN_OBJECT
            JsonToken.BEGIN_ARRAY -> A2uiJsonToken.BEGIN_ARRAY
            JsonToken.END_OBJECT -> A2uiJsonToken.END_OBJECT
            JsonToken.END_ARRAY -> A2uiJsonToken.END_ARRAY
            JsonToken.NAME -> A2uiJsonToken.NAME
            JsonToken.STRING -> A2uiJsonToken.STRING
            JsonToken.NUMBER -> A2uiJsonToken.NUMBER
            JsonToken.BOOLEAN -> A2uiJsonToken.BOOLEAN
            JsonToken.NULL -> A2uiJsonToken.NULL
            JsonToken.END_DOCUMENT,
            null -> A2uiJsonToken.END_DOCUMENT
        }

    override fun beginObject() = delegate.beginObject()

    override fun endObject() = delegate.endObject()

    override fun beginArray() = delegate.beginArray()

    override fun endArray() = delegate.endArray()

    override fun hasNext(): Boolean = delegate.hasNext()

    override fun nextName(): String = delegate.nextName()

    override fun nextString(): String = delegate.nextString()

    override fun nextBoolean(): Boolean = delegate.nextBoolean()

    override fun nextDouble(): Double = delegate.nextDouble()

    override fun nextInt(): Int = delegate.nextInt()

    override fun nextLong(): Long = delegate.nextLong()

    override fun nextNull() = delegate.nextNull()

    override fun skipValue() = delegate.skipValue()

    override fun close() = delegate.close()
}
