package dev.dimvlachos.lab.spiderdemo

import dev.dimvlachos.lab.core.demo.Demo
import dev.dimvlachos.lab.core.demo.DemoTag
import dev.dimvlachos.lab.core.demo.demoScript
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.demo_spider
import dev.dimvlachos.lab.resources.demo_spider_description
import dev.dimvlachos.lab.resources.ic_demo_spider
import dev.dimvlachos.lab.spiderdemo.presentation.components.SpiderRefreshDemo
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal object SpiderDemos {
    // The steps the script takes, by name.
    const val PullShort = "pull-short"
    const val Pull = "pull"
    const val Button = "button"

    // The prototype's clip: a short pull, so the spider peeks out and scurries back, then a full
    // pull, swaying a little, so its eyes go wide and letting go fetches; the new posts are reeled
    // up to the top. Then one more way in: a tap on the refresh button fetches the next batch. Each
    // step waits until the spider is home. The feed then fades back to its first five posts, so it
    // loops.
    private val refresh = demoScript {
        at(0.8.seconds, lasts = 2500.milliseconds) { tapControl(PullShort) }
        at(4.4.seconds, lasts = 7800.milliseconds) { tapControl(Pull) }
        at(13.2.seconds, lasts = 6800.milliseconds) { tapControl(Button) }
        at(21.0.seconds, lasts = 1400.milliseconds) { resetFeed() }
    }

    val all =
        listOf(
            Demo(
                "spider.refresh",
                Res.string.demo_spider,
                refresh,
                description = Res.string.demo_spider_description,
                icon = Res.drawable.ic_demo_spider,
                tags = setOf(DemoTag.Gesture, DemoTag.Physics, DemoTag.Canvas),
                tall = true,
            ) {
                SpiderRefreshDemo(it)
            },
        )
}
