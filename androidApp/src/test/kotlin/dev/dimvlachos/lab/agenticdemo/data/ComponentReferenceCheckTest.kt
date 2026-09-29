package dev.dimvlachos.lab.agenticdemo.data

import kotlin.test.Test
import kotlin.test.assertEquals
import org.json.JSONObject

class ComponentReferenceCheckTest {
    @Test
    fun aButtonWhoseLabelNeverArrivesIsReported() {
        val check = ComponentReferenceCheck()

        check.accept(
            components(
                "options",
                """{"id":"root","component":"Column","children":["card"]}""",
                """{"id":"card","component":"Card","child":"btn"}""",
                """{"id":"btn","component":"Button","child":"btn_label","action":{}}""",
            )
        )

        assertEquals(mapOf("options" to setOf("btn_label")), check.missing())
    }

    @Test
    fun referencesResolvedByALaterUpdateAreNotReported() {
        val check = ComponentReferenceCheck()

        check.accept(components("s", """{"id":"root","component":"Card","child":"t"}"""))
        check.accept(components("s", """{"id":"t","component":"Text","text":"Hi"}"""))

        assertEquals(emptyMap(), check.missing())
    }

    @Test
    fun tabsModalsAndTemplatesAreFollowedAndDeletedSurfacesForgotten() {
        val check = ComponentReferenceCheck()

        check.accept(
            components(
                "s",
                """{"id":"root","component":"Tabs","tabs":[{"title":"A","child":"a"}]}""",
                """{"id":"m","component":"Modal","trigger":"open","content":"body"}""",
                """{"id":"l","component":"List","children":{"componentId":"row","path":"/x"}}""",
            )
        )
        assertEquals(mapOf("s" to setOf("a", "open", "body", "row")), check.missing())

        check.accept(JSONObject("""{"version":"v0.9","deleteSurface":{"surfaceId":"s"}}"""))
        assertEquals(emptyMap(), check.missing())
    }

    @Test
    fun messagesThatAreNotComponentUpdatesAreIgnored() {
        val check = ComponentReferenceCheck()

        check.accept(
            JSONObject("""{"version":"v0.9","updateDataModel":{"surfaceId":"s","value":{}}}""")
        )

        assertEquals(emptyMap(), check.missing())
    }

    private fun components(surfaceId: String, vararg components: String) =
        JSONObject(
            """{"version":"v0.9","updateComponents":{"surfaceId":"$surfaceId","components":[""" +
                components.joinToString(",") +
                "]}}"
        )
}
