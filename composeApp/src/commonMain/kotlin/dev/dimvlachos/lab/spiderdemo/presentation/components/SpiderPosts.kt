package dev.dimvlachos.lab.spiderdemo.presentation.components

import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.spider_post_0_meta
import dev.dimvlachos.lab.resources.spider_post_0_text
import dev.dimvlachos.lab.resources.spider_post_0_who
import dev.dimvlachos.lab.resources.spider_post_10_meta
import dev.dimvlachos.lab.resources.spider_post_10_text
import dev.dimvlachos.lab.resources.spider_post_10_who
import dev.dimvlachos.lab.resources.spider_post_1_meta
import dev.dimvlachos.lab.resources.spider_post_1_text
import dev.dimvlachos.lab.resources.spider_post_1_who
import dev.dimvlachos.lab.resources.spider_post_2_meta
import dev.dimvlachos.lab.resources.spider_post_2_text
import dev.dimvlachos.lab.resources.spider_post_2_who
import dev.dimvlachos.lab.resources.spider_post_3_meta
import dev.dimvlachos.lab.resources.spider_post_3_text
import dev.dimvlachos.lab.resources.spider_post_3_who
import dev.dimvlachos.lab.resources.spider_post_4_meta
import dev.dimvlachos.lab.resources.spider_post_4_text
import dev.dimvlachos.lab.resources.spider_post_4_who
import dev.dimvlachos.lab.resources.spider_post_5_meta
import dev.dimvlachos.lab.resources.spider_post_5_text
import dev.dimvlachos.lab.resources.spider_post_5_who
import dev.dimvlachos.lab.resources.spider_post_6_meta
import dev.dimvlachos.lab.resources.spider_post_6_text
import dev.dimvlachos.lab.resources.spider_post_6_who
import dev.dimvlachos.lab.resources.spider_post_7_meta
import dev.dimvlachos.lab.resources.spider_post_7_text
import dev.dimvlachos.lab.resources.spider_post_7_who
import dev.dimvlachos.lab.resources.spider_post_8_meta
import dev.dimvlachos.lab.resources.spider_post_8_text
import dev.dimvlachos.lab.resources.spider_post_8_who
import dev.dimvlachos.lab.resources.spider_post_9_meta
import dev.dimvlachos.lab.resources.spider_post_9_text
import dev.dimvlachos.lab.resources.spider_post_9_who
import org.jetbrains.compose.resources.StringResource

/** One neighbor's post: who, where and when, what they say, and its likes and replies. */
internal class SpiderPost(
    val who: StringResource,
    val meta: StringResource,
    val text: StringResource,
    val initials: String,
    val likes: Int,
    val talk: Int,
)

/**
 * The prototype's posts by id: the feed's first five, then the four batches the spider brings up
 * ([dev.dimvlachos.lab.core.presentation.components.spider.SpiderRefreshState.DefaultBatches]).
 */
internal val SpiderPosts =
    listOf(
        SpiderPost(
            Res.string.spider_post_0_who,
            Res.string.spider_post_0_meta,
            Res.string.spider_post_0_text,
            "HD",
            18,
            4,
        ),
        SpiderPost(
            Res.string.spider_post_1_who,
            Res.string.spider_post_1_meta,
            Res.string.spider_post_1_text,
            "PN",
            41,
            22,
        ),
        SpiderPost(
            Res.string.spider_post_2_who,
            Res.string.spider_post_2_meta,
            Res.string.spider_post_2_text,
            "EL",
            27,
            3,
        ),
        SpiderPost(
            Res.string.spider_post_3_who,
            Res.string.spider_post_3_meta,
            Res.string.spider_post_3_text,
            "TW",
            9,
            2,
        ),
        SpiderPost(
            Res.string.spider_post_4_who,
            Res.string.spider_post_4_meta,
            Res.string.spider_post_4_text,
            "CB",
            33,
            5,
        ),
        SpiderPost(
            Res.string.spider_post_5_who,
            Res.string.spider_post_5_meta,
            Res.string.spider_post_5_text,
            "JM",
            0,
            0,
        ),
        SpiderPost(
            Res.string.spider_post_6_who,
            Res.string.spider_post_6_meta,
            Res.string.spider_post_6_text,
            "PN",
            0,
            0,
        ),
        SpiderPost(
            Res.string.spider_post_7_who,
            Res.string.spider_post_7_meta,
            Res.string.spider_post_7_text,
            "FO",
            0,
            0,
        ),
        SpiderPost(
            Res.string.spider_post_8_who,
            Res.string.spider_post_8_meta,
            Res.string.spider_post_8_text,
            "DO",
            0,
            0,
        ),
        SpiderPost(
            Res.string.spider_post_9_who,
            Res.string.spider_post_9_meta,
            Res.string.spider_post_9_text,
            "TH",
            0,
            0,
        ),
        SpiderPost(
            Res.string.spider_post_10_who,
            Res.string.spider_post_10_meta,
            Res.string.spider_post_10_text,
            "EF",
            0,
            0,
        ),
    )
