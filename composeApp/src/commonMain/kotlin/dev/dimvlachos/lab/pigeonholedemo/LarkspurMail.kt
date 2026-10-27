package dev.dimvlachos.lab.pigeonholedemo

import dev.dimvlachos.lab.core.presentation.components.pigeonhole.MailItem
import dev.dimvlachos.lab.core.presentation.components.pigeonhole.PigeonholeState

/** The folders' ids, in the rack's order: three across, two down. */
internal val LarkspurFolders = listOf("work", "receipts", "family", "travel", "school", "archive")

private fun mail(id: String, from: String, subj: String, prev: String, time: String) =
    MailItem(id, from, subj, prev, time)

/** The mail the app starts with, as the prototype writes it: an invented American household's. */
internal val LarkspurMail: Map<String, List<MailItem>> =
    mapOf(
        PigeonholeState.Inbox to
            listOf(
                mail(
                    "m1",
                    "Corner Market",
                    "Your receipt from Corner Market",
                    "\$42.18 · 14 items · Thanks for shopping on Elm Street",
                    "9:41 AM",
                ),
                mail(
                    "m2",
                    "Dana Whitfield",
                    "Q3 roadmap review moved",
                    "Now Thursday at 2 PM in room 4B. Bring the hiring numbers.",
                    "9:12 AM",
                ),
                mail(
                    "m3",
                    "Mom",
                    "Thanksgiving at our place?",
                    "Aunt Carol is bringing pies. Can you grab the folding chairs?",
                    "8:30 AM",
                ),
                mail(
                    "m4",
                    "Bluebird Air",
                    "Your trip to Denver is confirmed",
                    "Confirmation K7QX2M · Fri, Nov 14 · Seat 14C",
                    "Yesterday",
                ),
                mail(
                    "m5",
                    "Lakeside Elementary",
                    "Field trip permission slip",
                    "Please sign by Friday for the science museum trip.",
                    "Yesterday",
                ),
                mail(
                    "m6",
                    "Hollis & Sons",
                    "Revised kitchen estimate",
                    "The new total is \$18,400, with the pantry shelves.",
                    "Mon",
                ),
                mail(
                    "m7",
                    "Priya Shah",
                    "Book club on Thursday",
                    "I’ll host this time. Bring the lemon bars if you can!",
                    "Mon",
                ),
            ),
        "work" to
            listOf(
                mail(
                    "w1",
                    "Dana Whitfield",
                    "Offsite agenda",
                    "Breakfast at 8, then planning in the big room.",
                    "Oct 20",
                ),
                mail(
                    "w2",
                    "Payroll",
                    "Expense report approved",
                    "\$312.40 will be in your next paycheck.",
                    "Oct 17",
                ),
                mail(
                    "w3",
                    "Marcus Lee",
                    "Thanks for the intro!",
                    "Coffee next week? I’m free Tuesday afternoon.",
                    "Oct 9",
                ),
            ),
        "receipts" to
            listOf(
                mail(
                    "r1",
                    "Ridgeway Hardware",
                    "Receipt #20418",
                    "\$23.06 · Paint roller, drop cloth, tape",
                    "Oct 22",
                ),
                mail(
                    "r2",
                    "Sunny Side Diner",
                    "Thanks for stopping by",
                    "\$18.40 · Card ending 4417",
                    "Oct 19",
                ),
                mail(
                    "r3",
                    "Pinecrest Pharmacy",
                    "Your order is ready",
                    "\$9.99 · Pick up at the front counter",
                    "Oct 14",
                ),
                mail(
                    "r4",
                    "Maple & Co.",
                    "Your order has shipped",
                    "\$64.00 · Arrives Thursday",
                    "Oct 8",
                ),
            ),
        "family" to
            listOf(
                mail("f1", "Jake", "Pics from the lake", "Ava caught her first fish!", "Oct 18"),
                mail(
                    "f2",
                    "Dad",
                    "Car registration",
                    "Reminder: it’s due by the end of the month.",
                    "Oct 2",
                ),
            ),
        "travel" to
            listOf(
                mail(
                    "t1",
                    "Pinewood Lodge",
                    "See you Friday",
                    "Check-in is from 3 PM. You’re in Cabin 12.",
                    "Oct 12",
                ),
                mail("t2", "Metro Transit", "Your monthly pass", "Valid Nov 1 to Nov 30", "Oct 1"),
            ),
        "school" to
            listOf(
                mail(
                    "s1",
                    "Ms. Rivera",
                    "Conference sign-ups",
                    "Pick a 15-minute slot for next week.",
                    "Oct 15",
                ),
            ),
        "archive" to
            listOf(
                mail(
                    "a1",
                    "Summit Bank",
                    "Your statement is ready",
                    "The October statement is available.",
                    "Oct 3",
                ),
                mail(
                    "a2",
                    "Fairview Library",
                    "Your hold is in",
                    "“The Overstory” is waiting at the front desk.",
                    "Sep 28",
                ),
                mail("a3", "City of Fairview", "Water bill", "\$48.20, due Nov 10", "Sep 25"),
            ),
    )
