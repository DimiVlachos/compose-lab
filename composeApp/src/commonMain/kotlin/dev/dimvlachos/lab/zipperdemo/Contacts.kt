package dev.dimvlachos.lab.zipperdemo

import androidx.compose.ui.graphics.Color
import dev.dimvlachos.lab.core.presentation.ui.ZipperAvatarColors
import dev.dimvlachos.lab.core.presentation.ui.ZipperMeAvatar

/** One person in the address book: their [name], the line under it and their avatar's colour. */
internal class Contact(val first: String, val last: String, val sub: String, val avatar: Color) {
    val name: String
        get() = "$first $last"

    val initials: String
        get() = "${first[0]}${last[0]}"
}

/** A row of the contacts list: a letter's header, or a contact. */
internal sealed interface ContactItem {
    class Header(val letter: String) : ContactItem

    /** [first] is the first row under its header, which has no rule above it. */
    class Person(val contact: Contact, val first: Boolean) : ContactItem
}

/**
 * The prototype's address book: 88 invented people in alphabetical order of first name, their last
 * names, lines and colours dealt out by its formulas, under a header for each letter.
 */
internal object Contacts {
    private val First =
        ("Aaron Abby Adrian Alicia Amir Andrea Avery Ben Bianca Brooke Bryce Caleb Carmen " +
                "Chloe Colin Courtney Dana Darius Derek Diane Elijah Emily Ethan Eva Faith Felix " +
                "Fiona Gabe Grace Grant Hannah Hector Holly Ian Imani Isaac Jacob Jada Jasmine " +
                "Jordan Julia Kayla Keith Kevin Kim Laura Leo Lila Logan Lucas Madison Marcus " +
                "Maria Mason Mia Nadia Nathan Nick Nora Olivia Omar Owen Paige Paul Priya Quinn " +
                "Rachel Reggie Riley Rosa Ryan Sam Sarah Sean Sofia Spencer Tara Tessa Tom Tyler " +
                "Vanessa Victor Wade Wendy Will Yolanda Zach Zoe")
            .split(" ")
    private val Last =
        ("Bennett Carter Delgado Ellis Foster Garcia Hughes Iverson Jenkins Kowalski " +
                "Lambert Morales Nguyen Ortiz Patel Ramsey Sullivan Thompson Underwood Vasquez " +
                "Walsh Young Brooks Coleman Dixon Fischer Greene Harper Kim Lopez Mitchell Park " +
                "Reed Shaw Turner Wallace")
            .split(" ")
    private val Sub =
        listOf(
            "Mobile",
            "Work · Harlow Dental",
            "Mobile",
            "Home",
            "Work · Pine Street Hardware",
            "Mobile",
            "Work · Bluebird Café",
            "Mobile",
            "Home",
            "Work · Lakeview Elementary",
            "Mobile",
            "Work · Cedar & Co. Realty",
        )
    private val Avatars = ZipperAvatarColors

    val people: List<Contact> = First.mapIndexed { i, f ->
        Contact(f, Last[(i * 7 + 3) % Last.size], Sub[i % Sub.size], Avatars[i % Avatars.size])
    }

    val items: List<ContactItem> = buildList {
        var cur = ' '
        for (c in people) {
            val ch = c.first[0]
            val first = ch != cur
            if (first) {
                cur = ch
                add(ContactItem.Header(ch.toString()))
            }
            add(ContactItem.Person(c, first))
        }
    }

    /** The owner's own card above the list. */
    const val MeName = "Maya Reyes"
    const val MeInitials = "MR"
    val MeAvatar = ZipperMeAvatar

    /** Each item's kind for the zipper's track: a header, a first row, or a row under a rule. */
    const val KindHeader = 0
    const val KindFirstRow = 1
    const val KindRow = 2

    val kinds: IntArray =
        items
            .map {
                when (it) {
                    is ContactItem.Header -> KindHeader
                    is ContactItem.Person -> if (it.first) KindFirstRow else KindRow
                }
            }
            .toIntArray()

    val sectionLetters: List<String?> = items.map { (it as? ContactItem.Header)?.letter }
}
