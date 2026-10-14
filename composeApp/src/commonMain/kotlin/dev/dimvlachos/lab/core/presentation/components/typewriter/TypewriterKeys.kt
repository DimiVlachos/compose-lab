package dev.dimvlachos.lab.core.presentation.components.typewriter

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/** What a key on the typewriter's keyboard does. */
@Immutable
internal sealed interface KeyAction {
    /** Types [char], or its capital while Shift is down. */
    @Immutable data class Type(val char: Char) : KeyAction

    data object Space : KeyAction

    data object Shift : KeyAction

    /** Runs the carriage back a letter, and the correction tape lifts it off. */
    data object Backspace : KeyAction

    /** Pushes the return lever: a new line. */
    data object Return : KeyAction

    /** Turns to the other page of keys: figures and stops, or letters. */
    data object Page : KeyAction
}

/** A key: what it does, and how wide it is in key widths. */
@Immutable
internal data class TypeKey(val action: KeyAction, val width: Float = 1f) {
    /** Its name, unique on the keyboard: the letter for a letter key. */
    val id: String
        get() =
            when (action) {
                is KeyAction.Type -> action.char.toString()
                else -> action.toString()
            }
}

/** Where the keys are, row by row, on each page. */
internal object TypewriterLayout {
    private fun letters(row: String) = row.map { TypeKey(KeyAction.Type(it)) }

    /** The letters: QWERTY, with Shift and Backspace either side of the bottom row. */
    val Letters: List<List<TypeKey>> =
        listOf(
            letters("qwertyuiop"),
            letters("asdfghjkl"),
            listOf(TypeKey(KeyAction.Shift, 1.3f)) +
                letters("zxcvbnm") +
                TypeKey(KeyAction.Backspace, 1.3f),
            listOf(
                TypeKey(KeyAction.Page, 1.3f),
                TypeKey(KeyAction.Type(',')),
                TypeKey(KeyAction.Space, 5f),
                TypeKey(KeyAction.Type('.')),
                TypeKey(KeyAction.Return, 1.3f),
            ),
        )

    /** The figures and stops. */
    val Figures: List<List<TypeKey>> =
        listOf(
            letters("1234567890"),
            letters("-:'?!()/&"),
            letters("\";@#%*+") + TypeKey(KeyAction.Backspace, 1.3f),
            listOf(
                TypeKey(KeyAction.Page, 1.3f),
                TypeKey(KeyAction.Type(',')),
                TypeKey(KeyAction.Space, 5f),
                TypeKey(KeyAction.Type('.')),
                TypeKey(KeyAction.Return, 1.3f),
            ),
        )

    /** The page a character is typed from, or null if no key types it. */
    fun pageOf(char: Char): Boolean? {
        val c = char.lowercaseChar()
        fun on(page: List<List<TypeKey>>) = page.any { row ->
            row.any { (it.action as? KeyAction.Type)?.char == c }
        }
        return when {
            on(Letters) -> false
            on(Figures) -> true
            else -> null
        }
    }
}

/** A key pressed: [id] dips, with the scripted fingertip on it if [touched]. */
@Immutable internal data class KeyPress(val id: String, val touched: Boolean)

/**
 * The typewriter's keyboard: which page of keys shows, whether Shift is down, and the keys pressed
 * by a scripted fingertip or a hardware keyboard, for each to dip, the fingertip shown on it.
 */
@Stable
internal class TypewriterKeyboard {
    /** Whether the figures page shows, rather than the letters. */
    var figures by mutableStateOf(false)
        internal set

    /** Whether the next letter is a capital. */
    var shifted by mutableStateOf(false)
        internal set

    private val pressed = MutableSharedFlow<KeyPress>(extraBufferCapacity = PressBuffer)

    /** Every key pressed by a script or a hardware keyboard, as it is pressed. */
    val presses: SharedFlow<KeyPress> = pressed

    internal fun tapped(id: String) {
        pressed.tryEmit(KeyPress(id, touched = true))
    }

    internal fun dipped(id: String) {
        pressed.tryEmit(KeyPress(id, touched = false))
    }

    private companion object {
        const val PressBuffer = 32
    }
}

/**
 * The keys to press, in turn, to type [char] on a keyboard showing [figures] or not, [shifted] or
 * not: turning the page first if the character is on the other one, and Shift for a capital. A
 * character no key types needs nothing.
 */
internal fun keysFor(char: Char, figures: Boolean, shifted: Boolean): List<TypeKey> {
    val keys = mutableListOf<TypeKey>()
    when (char) {
        ' ' -> keys += TypeKey(KeyAction.Space)
        '\n' -> keys += TypeKey(KeyAction.Return)
        else -> {
            val page = TypewriterLayout.pageOf(char) ?: return emptyList()
            if (page != figures) keys += TypeKey(KeyAction.Page)
            if (char.isUpperCase() && !shifted) keys += TypeKey(KeyAction.Shift)
            keys += TypeKey(KeyAction.Type(char.lowercaseChar()))
        }
    }
    return keys
}

/** The key that types [char] on its own page, for a hardware key to dip, if there is one. */
internal fun keyIdFor(char: Char): String? =
    when (char) {
        ' ' -> TypeKey(KeyAction.Space).id
        '\n' -> TypeKey(KeyAction.Return).id
        else -> TypewriterLayout.pageOf(char)?.let { char.lowercaseChar().toString() }
    }
