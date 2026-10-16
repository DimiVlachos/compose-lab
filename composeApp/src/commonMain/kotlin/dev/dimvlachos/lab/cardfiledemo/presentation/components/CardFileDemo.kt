package dev.dimvlachos.lab.cardfiledemo.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.dimvlachos.lab.cardfiledemo.CardFileDemos
import dev.dimvlachos.lab.cardfiledemo.ContactDeck
import dev.dimvlachos.lab.cardfiledemo.DemoContacts
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.core.demo.demoInsets
import dev.dimvlachos.lab.core.platform.Sound
import dev.dimvlachos.lab.core.platform.rememberSoundPlayer
import dev.dimvlachos.lab.core.presentation.components.cardfile.CardFile
import dev.dimvlachos.lab.core.presentation.components.cardfile.CardFileState
import dev.dimvlachos.lab.core.presentation.components.cardfile.FileCard
import dev.dimvlachos.lab.core.presentation.ui.LabTheme
import dev.dimvlachos.lab.resources.Res
import dev.dimvlachos.lab.resources.cardfile_call_failed
import dev.dimvlachos.lab.resources.cardfile_hint
import dev.dimvlachos.lab.resources.cardfile_letter
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

/** How long the note that a call or message couldn't open stays up. */
private const val CallFailedNoteMs = 3_000L

/**
 * Contacts as a chrome rotary card file on a lamplit desk. The panel under it names the front
 * contact; a pulled-out card calls or messages them. The script's spins, pull and file go through
 * the same state a finger drives.
 */
@Composable
internal fun CardFileDemo(
    state: DemoState,
    deck: ContactDeck = remember { ContactDeck(DemoContacts) },
    file: CardFileState = remember { CardFileState(initial = CardFileDemos.StartIndex) },
) {
    val sounds = rememberSoundPlayer(setOf(Sound.CardTick))
    val uri = LocalUriHandler.current
    // Counts failed opens, so a second failure while the note shows starts its time again.
    var failures by remember { mutableIntStateOf(0) }
    var callFailed by remember { mutableStateOf(false) }
    // The description is built outside composition, so the letter's format is fetched here once.
    val letterFormat = stringResource(Res.string.cardfile_letter, "%s")

    fun open(url: String) {
        try {
            uri.openUri(url)
        } catch (_: IllegalArgumentException) {
            // What UriHandler promises, and what Android throws when nothing can take the link.
            failures++
        } catch (_: IllegalStateException) {
            failures++
        }
    }

    LaunchedEffect(failures) {
        if (failures == 0) return@LaunchedEffect
        callFailed = true
        delay(CallFailedNoteMs)
        callFailed = false
    }

    DisposableEffect(state, file) {
        state.setCardSpinHandler { by, duration -> file.spinBy(by, duration) }
        state.setControlTapHandler { control ->
            when (control) {
                CardFileDemos.Pull -> file.pull()
                CardFileDemos.File -> file.file()
            }
        }
        // Puts the card back and the wheel on its first contact, so the clip loops.
        state.setFeedResetHandler {
            file.file()
            file.spinTo(CardFileDemos.StartIndex)
        }
        onDispose {
            state.setCardSpinHandler(null)
            state.setControlTapHandler(null)
            state.setFeedResetHandler(null)
        }
    }

    Column(Modifier.fillMaxSize().windowInsetsPadding(demoInsets(state))) {
        CardFile(
            cards = deck.cards,
            face = { ContactCardFace(deck.contact(it)) },
            pulled = { entry ->
                val contact = deck.contact(entry)
                val digits = contact.number.filter { it.isDigit() }
                PulledContact(
                    contact,
                    onCall = { open("tel:$digits") },
                    onMessage = { open("sms:$digits") },
                )
            },
            describe = { card ->
                when (card) {
                    is FileCard.Divider -> letterFormat.replace("%s", card.letter.toString())
                    is FileCard.Entry -> deck.contact(card).let { "${it.name}, ${it.number}" }
                }
            },
            modifier = Modifier.weight(1f).fillMaxWidth(),
            state = file,
            sounds = sounds,
        )
        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
            // Read through derivedStateOf, so the panel recomposes as the front card changes, not
            // on every frame of a spin.
            val front by
                remember(deck, file) { derivedStateOf { deck.cards.getOrNull(file.front) } }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    (front as? FileCard.Entry)?.let { deck.contact(it).name } ?: "",
                    style = LabTheme.typography.subtitle,
                    color = LabTheme.colors.textPrimary,
                )
                if (deck.cards.isNotEmpty() || callFailed) {
                    Text(
                        if (callFailed) stringResource(Res.string.cardfile_call_failed)
                        else stringResource(Res.string.cardfile_hint),
                        style = LabTheme.typography.label,
                        // The note stands out from the muted hint it replaces.
                        color =
                            if (callFailed) LabTheme.colors.textPrimary
                            else LabTheme.colors.textMuted,
                        textAlign = TextAlign.Center,
                        modifier =
                            if (callFailed)
                                Modifier.testTag("call-failed").semantics {
                                    liveRegion = LiveRegionMode.Polite
                                }
                            else Modifier,
                    )
                }
            }
        }
    }
}
