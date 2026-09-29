package dev.dimvlachos.lab.agenticdemo.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * Finds component ids the agent pointed at but never defined. The renderer cannot report these: to
 * it a missing child is one that simply has not arrived yet, so a Button whose label never comes
 * stays a label-less loading pill forever. Checked once a turn has finished, they are real mistakes
 * and go back to the agent like any other validation error.
 */
class ComponentReferenceCheck {
    private val defined = mutableMapOf<String, MutableSet<String>>()
    private val referenced = mutableMapOf<String, MutableSet<String>>()

    fun accept(line: String) {
        val message = runCatching { JSONObject(line) }.getOrNull() ?: return
        message.optJSONObject("deleteSurface")?.optString("surfaceId")?.let {
            defined.remove(it)
            referenced.remove(it)
            return
        }
        val update = message.optJSONObject("updateComponents") ?: return
        val surfaceId = update.optString("surfaceId")
        val components = update.optJSONArray("components") ?: return
        for (i in 0 until components.length()) {
            val component = components.optJSONObject(i) ?: continue
            defined.getOrPut(surfaceId, ::mutableSetOf) += component.optString("id")
            referenced.getOrPut(surfaceId, ::mutableSetOf) += references(component)
        }
    }

    fun missing(): Map<String, Set<String>> =
        referenced
            .mapValues { (surfaceId, ids) -> ids - defined[surfaceId].orEmpty() }
            .filterValues { it.isNotEmpty() }

    private fun references(component: JSONObject): Set<String> = buildSet {
        for (key in listOf("child", "trigger", "content")) {
            component.optString(key).takeIf { it.isNotEmpty() }?.let(::add)
        }
        when (val children = component.opt("children")) {
            is JSONArray -> for (i in 0 until children.length()) add(children.optString(i))
            // A template: one component id repeated over a data-model list.
            is JSONObject ->
                children.optString("componentId").takeIf { it.isNotEmpty() }?.let(::add)
        }
        component.optJSONArray("tabs")?.let { tabs ->
            for (i in 0 until tabs.length()) {
                tabs.optJSONObject(i)?.optString("child")?.takeIf { it.isNotEmpty() }?.let(::add)
            }
        }
    }
}
