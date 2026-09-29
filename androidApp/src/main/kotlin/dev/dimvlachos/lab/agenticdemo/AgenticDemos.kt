package dev.dimvlachos.lab.agenticdemo

import dev.dimvlachos.lab.agenticdemo.presentation.components.ConciergeDemo
import dev.dimvlachos.lab.core.demo.Demo
import dev.dimvlachos.lab.core.demo.demoScript
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.demo_agentic_concierge

object AgenticDemos {
    // Live and interactive: there is nothing to script, the agent decides what appears.
    val all: List<Demo> =
        listOf(
            Demo("agentic.concierge", Res.string.demo_agentic_concierge, demoScript {}) {
                ConciergeDemo()
            }
        )
}
