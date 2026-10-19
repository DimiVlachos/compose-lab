package dev.dimvlachos.lab.bookshelfdemo.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.bookshelfdemo.BookshelfDemos
import dev.dimvlachos.lab.bookshelfdemo.DemoBooks
import dev.dimvlachos.lab.bookshelfdemo.DemoChapters
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.demo.demoInsets
import dev.dimvlachos.lab.core.platform.rememberSoundPlayer
import dev.dimvlachos.lab.core.presentation.components.bookshelf.pageturn.rememberPageTurnState
import dev.dimvlachos.lab.core.presentation.components.bookshelf.shelf.LocalShelfSounds
import dev.dimvlachos.lab.core.presentation.components.bookshelf.shelf.ReadingShelf
import dev.dimvlachos.lab.core.presentation.components.bookshelf.shelf.ShelfChapter
import dev.dimvlachos.lab.core.presentation.components.bookshelf.shelf.ShelfSoundSet
import dev.dimvlachos.lab.core.presentation.components.bookshelf.shelf.ShelfStyle
import dev.dimvlachos.lab.core.presentation.components.bookshelf.shelf.rememberShelfContent
import dev.dimvlachos.lab.core.presentation.components.bookshelf.shelf.rememberShelfState
import dev.dimvlachos.lab.core.presentation.components.bookshelf.shelf.steadyShelfContent
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.core.presentation.ui.isWideStage
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.bookshelf_on_the_shelves
import dev.dimvlachos.lab.resources.bookshelf_reading_now
import kotlinx.coroutines.flow.first
import org.jetbrains.compose.resources.stringResource

// The ring takes this much of the screen's height, as the hero of a reading list does.
private const val RingHero = 0.45f

// The planks are laid out in the band they were drawn for.
private const val PlanksAspect = 412f / 640f

// How long the shelves take to fade in once they are ready.
private const val ShowMs = 250

/**
 * A reading list on two shelves: the books being read on a ring that spins, and below it two oak
 * planks of books to read next and books read. Any book tips out by its spine; one being read opens
 * at its bookmark and its pages turn.
 */
@Composable
internal fun BookshelfDemo(state: DemoState) {
    val ringShelf = rememberShelfState()
    val plankShelf = rememberShelfState()
    // One book in hand at a time, across both shelves.
    ringShelf.others = { listOf(plankShelf) }
    plankShelf.others = { listOf(ringShelf) }
    val ring =
        steadyShelfContent(
            rememberShelfContent(
                listOf(DemoBooks.reading),
                DemoBooks.SaltRoad,
                perPlank = Int.MAX_VALUE,
            ),
            ringShelf,
        )
    val planks =
        steadyShelfContent(
            rememberShelfContent(
                listOf(DemoBooks.upNext, DemoBooks.finished),
                readingBookId = null,
            ),
            plankShelf,
        )
    val chapterFor: (Int) -> ShelfChapter? = { id ->
        DemoChapters.forBook(id)?.let { ShelfChapter(it.title, it.words) }
    }
    val scroll = rememberScrollState()
    // Both shelves come in together, whole, once their books are baked.
    val shown = remember { Animatable(0f) }
    LaunchedEffect(ringShelf, plankShelf) {
        snapshotFlow { ringShelf.ready && plankShelf.ready }.first { it }
        shown.animateTo(1f, tween(ShowMs))
    }
    val ringTurns = rememberPageTurnState()
    val plankTurns = rememberPageTurnState()
    // The script's steps, by name: a ring spun, a book taken out or put back, a page turned, the
    // planks scrolled to, and everything put back where it began so the clip loops.
    DisposableEffect(state, ringShelf, plankShelf) {
        state.setControlTapHandler { control ->
            when (control) {
                BookshelfDemos.SpinOn -> ringShelf.ringSpin?.spinBy(2)
                BookshelfDemos.SpinBack -> ringShelf.ringSpin?.spinBy(-2)
                BookshelfDemos.TakeOut ->
                    ringShelf.ringSpin?.let { spin ->
                        spin.ring?.books?.getOrNull(spin.centredIndex)?.let {
                            ringShelf.openDirect(it.id)
                        }
                    }
                BookshelfDemos.TurnOn -> ringTurns.tap(forward = true)
                BookshelfDemos.TurnBack -> ringTurns.tap(forward = false)
                BookshelfDemos.PutBack -> {
                    ringShelf.putBack()
                    plankShelf.putBack()
                }
                BookshelfDemos.ToPlanks -> scroll.animateScrollTo(scroll.maxValue)
                BookshelfDemos.TakeOutPlank -> plankShelf.openDirect(BookshelfDemos.PlankBook)
            }
        }
        state.setFeedResetHandler {
            ringShelf.putBack()
            plankShelf.putBack()
            scroll.animateScrollTo(0)
            ringShelf.ringSpin?.let { spin ->
                spin.ring?.indexOf(DemoBooks.SaltRoad)?.takeIf { it >= 0 }?.let(spin::spinTo)
            }
        }
        onDispose {
            state.setControlTapHandler(null)
            state.setFeedResetHandler(null)
        }
    }
    val sounds = rememberSoundPlayer(ShelfSoundSet)
    CompositionLocalProvider(LocalShelfSounds provides sounds) {
        BoxWithConstraints(Modifier.fillMaxSize().windowInsetsPadding(demoInsets(state))) {
            val spread = isWideStage(maxWidth, maxHeight)
            val viewport = maxHeight
            ReadingShelf(
                ring,
                ringShelf,
                chapterFor,
                Modifier.fillMaxSize(),
                ShelfStyle.Ring,
                spread,
                ringTurns,
            ) { ringHero ->
                ReadingShelf(
                    planks,
                    plankShelf,
                    chapterFor,
                    Modifier.fillMaxSize(),
                    ShelfStyle.Planks,
                    spread,
                    plankTurns,
                ) { plankHero ->
                    Column(
                        Modifier.fillMaxSize()
                            .graphicsLayer { alpha = shown.value }
                            .verticalScroll(scroll)
                    ) {
                        SectionLabel(stringResource(Res.string.bookshelf_reading_now))
                        ringHero(Modifier.fillMaxWidth().height(viewport * RingHero))
                        SectionLabel(stringResource(Res.string.bookshelf_on_the_shelves))
                        plankHero(Modifier.fillMaxWidth().aspectRatio(PlanksAspect))
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    BasicText(
        text,
        Modifier.padding(horizontal = 20.dp, vertical = 10.dp).semantics { heading() },
        style = LabTheme.typography.label.copy(color = LabTheme.colors.textMuted),
    )
}
