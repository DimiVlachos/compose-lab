package dev.dimvlachos.lab.core.presentation.components.padlock

/** How strong a password is, as the padlock shows it: from an empty field to locked shut. */
internal enum class StrengthLevel {
    Empty,
    Weak,
    Fair,
    Good,
    Strong,
}

/** The length a password needs before it can be [StrengthLevel.Strong]. */
internal const val StrongLength = 12

/**
 * What [checkPassword] found in a password: its [level], and what it still lacks, for the tip under
 * the lock and the rules under the field.
 */
internal data class PasswordCheck(
    val level: StrengthLevel,
    val length: Int,
    val hasNumber: Boolean,
    // A symbol, or both a capital and a lowercase letter: the third rule under the field.
    val hasMix: Boolean,
    val hasCapital: Boolean,
    val hasSymbol: Boolean,
    // On every list of leaked passwords, whatever its length.
    val common: Boolean,
) {
    val longEnough: Boolean
        get() = length >= StrongLength

    /** How many more characters it needs to be long enough. */
    val missingCharacters: Int
        get() = (StrongLength - length).coerceAtLeast(0)
}

/**
 * The prototype's placeholder scoring: length and the kinds of character in it, and a few passwords
 * everyone tries. A real app would score as zxcvbn does, mapped to the same levels.
 */
internal fun checkPassword(password: String): PasswordCheck {
    val number = password.any { it in '0'..'9' }
    val lower = password.any { it in 'a'..'z' }
    val upper = password.any { it in 'A'..'Z' }
    val symbol = password.any { !(it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9') }
    val capital = upper && lower
    val common = password.lowercase() in CommonPasswords
    val kinds = (if (capital) 1 else 0) + (if (number) 1 else 0) + (if (symbol) 1 else 0)
    // Characters as a person counts them: an emoji is one, not the two halves it is stored as.
    val n = password.length - password.count { it.isLowSurrogate() }
    val level =
        when {
            n == 0 -> StrengthLevel.Empty
            common -> StrengthLevel.Weak
            n >= StrongLength && kinds >= 2 -> StrengthLevel.Strong
            n >= 9 && kinds >= 2 -> StrengthLevel.Good
            n >= 8 || (kinds >= 1 && n >= 6) -> StrengthLevel.Fair
            else -> StrengthLevel.Weak
        }
    return PasswordCheck(
        level = level,
        length = n,
        hasNumber = number,
        hasMix = symbol || capital,
        hasCapital = capital,
        hasSymbol = symbol,
        common = common,
    )
}

/** What the tip under the lock asks for, at most two of them, in the prototype's order. */
internal sealed interface Need {
    data class Characters(val count: Int) : Need

    data object Number : Need

    data object Capital : Need

    data object Symbol : Need
}

/** The first two things [check] still needs, as the tip names them. */
internal fun needs(check: PasswordCheck): List<Need> = buildList {
    if (check.missingCharacters > 0) add(Need.Characters(check.missingCharacters))
    if (!check.hasNumber) add(Need.Number)
    if (!check.hasCapital) add(Need.Capital) else if (!check.hasSymbol) add(Need.Symbol)
}
    .take(2)

private val CommonPasswords =
    setOf(
        "password",
        "password1",
        "qwerty",
        "123456",
        "12345678",
        "letmein",
        "iloveyou",
        "welcome",
        "monkey",
        "football",
        "baseball",
    )
