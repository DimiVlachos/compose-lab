package dev.dimvlachos.lab.cardfiledemo

private class Row(
    val name: String,
    val kind: String,
    val number: String,
    val line: String,
    val note: String,
)

/** The prototype's people and places: invented, American, in alphabetical order. */
private val rows =
    listOf(
        Row(
            "Abby Carter",
            "Carter Hardware",
            "(802) 555-0142",
            "14 Elm St, Burlington",
            "Spare key under the planter",
        ),
        Row(
            "Andre Wallace",
            "Piano teacher",
            "(802) 555-0178",
            "Tuesdays, 4:30 PM",
            "Bring the Chopin book",
        ),
        Row(
            "Ben Hollis",
            "Hollis Auto Body",
            "(802) 555-0119",
            "Route 7, Shelburne",
            "Ask for the Thursday rate",
        ),
        Row(
            "Bianca Reyes",
            "Sister-in-law",
            "(617) 555-0164",
            "Somerville, MA",
            "Birthday March 3",
        ),
        Row(
            "Caleb Moore",
            "Landlord",
            "(802) 555-0107",
            "Pine St Apartments",
            "Rent due on the 1st",
        ),
        Row(
            "Dana Whitfield",
            "Lakeside Dental",
            "(802) 555-0185",
            "Cleaning every 6 months",
            "Next: Nov 12, 9:00 AM",
        ),
        Row(
            "Eli Brennan",
            "College roommate",
            "(312) 555-0126",
            "Chicago, IL",
            "Owes me a deep dish",
        ),
        Row(
            "Frank Delgado",
            "Plumber · 24 hr",
            "(802) 555-0133",
            "Delgado Plumbing",
            "Fixed the water heater",
        ),
        Row(
            "Grandma June",
            "Family",
            "(207) 555-0150",
            "Bar Harbor, ME",
            "Call Sundays after church",
        ),
        Row(
            "Hannah Price",
            "Book club",
            "(802) 555-0171",
            "First Thursday, 7 PM",
            "Reading East of Eden",
        ),
        Row(
            "Jamal Turner",
            "Little League coach",
            "(802) 555-0192",
            "Field 3, Saturdays",
            "Snack duty Oct 21",
        ),
        Row(
            "Kendra Lee",
            "Pinecrest Animal Hosp.",
            "(802) 555-0148",
            "Biscuit's vet",
            "Shots due in spring",
        ),
        Row("Lucy Bennett", "Babysitter", "(802) 555-0115", "\$18 an hour", "Free most Fridays"),
        Row(
            "Maya Brooks",
            "Best friend",
            "(617) 555-0193",
            "Cambridge, MA",
            "Coffee Saturday, Blue Kettle",
        ),
        Row("Mom", "Family", "(802) 555-0101", "Home", "Call her back!"),
        Row("Nate Sullivan", "Next door", "(802) 555-0138", "16 Maple Ave", "Still has our ladder"),
        Row("Olivia Grant", "Grant Realty", "(802) 555-0166", "Church St", "Showing Sat, 11 AM"),
        Row(
            "Pete's Corner Pizza",
            "Takeout",
            "(802) 555-0122",
            "Open till 11 PM",
            "Large pepperoni, extra crisp",
        ),
        Row(
            "Rosa Martinez",
            "Across the street",
            "(802) 555-0159",
            "21 Maple Ave",
            "Waters the plants when we travel",
        ),
        Row(
            "Sam Whitaker",
            "Bandmate",
            "(802) 555-0184",
            "Practice Wed, 7 PM",
            "Bring the spare cable",
        ),
        Row(
            "Tom Becker",
            "Becker Tax & Books",
            "(802) 555-0177",
            "Main St, Suite 2",
            "Receipts by Feb 15",
        ),
        Row(
            "Walt Jensen",
            "Fishing buddy",
            "(802) 555-0109",
            "Lake Champlain",
            "Opening day in April",
        ),
        Row(
            "Zoe Chen",
            "Cousin",
            "(415) 555-0131",
            "San Francisco, CA",
            "Visiting for Thanksgiving",
        ),
    )

/** The card file's contacts, each with the prototype's seed for its hand: 31 + 17 per card. */
internal val DemoContacts: List<Contact> = rows.mapIndexed { i, r ->
    Contact(
        id = r.name.lowercase().replace(Regex("[^a-z]+"), "-").trim('-'),
        name = r.name,
        kind = r.kind,
        number = r.number,
        line = r.line,
        note = r.note,
        seed = 31 + i * 17,
    )
}
