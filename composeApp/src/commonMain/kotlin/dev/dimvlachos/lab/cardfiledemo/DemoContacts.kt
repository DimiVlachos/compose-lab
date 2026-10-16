package dev.dimvlachos.lab.cardfiledemo

private fun c(first: String, last: String, area: String, line: Int, note: String) =
    Contact(
        "${last.lowercase()}-${first.lowercase()}",
        first,
        last,
        "($area) 555-01${line.toString().padStart(2, '0')}",
        note,
        seed = (first + last).hashCode(),
    )

/** The card file's people: invented, American, alphabetical by last name. */
internal val DemoContacts: List<Contact> =
    listOf(
            c("Grace", "Abbott", "617", 12, "Book club, 2nd Tue"),
            c("Leo", "Alvarez", "512", 47, "Fixed the porch"),
            c("Nora", "Bennett", "207", 3, "Cape house, July"),
            c("Maya", "Brooks", "415", 42, "Pottery class Thu"),
            c("Sam", "Burke", "312", 88, "Old roommate"),
            c("Owen", "Carter", "503", 21, "Carpool, Mon/Wed"),
            c("Ruby", "Chen", "206", 64, "Dentist reminder"),
            c("Eli", "Dawson", "303", 9, "Bluegrass jam"),
            c("Hazel", "Diaz", "305", 77, "Lends the kayak"),
            c("Cal", "Ellison", "802", 35, "Maple syrup guy"),
            c("June", "Fischer", "608", 50, "Ski trip host"),
            c("Ben", "Foster", "919", 16, "Soccer coach"),
            c("Iris", "Garner", "404", 93, "Plant sitter"),
            c("Wes", "Hale", "406", 28, "Fishing in May"),
            c("Lucy", "Hart", "212", 70, "Godmother"),
            c("Theo", "Ingram", "615", 5, "Band practice"),
            c("Ada", "Jensen", "651", 59, "Knits the hats"),
            c("Miles", "Keller", "720", 83, "Climbing gym"),
            c("Pearl", "Lawson", "843", 26, "Shrimp boil, Aug"),
            c("Gus", "Moreno", "702", 61, "Mechanic"),
            c("Ivy", "Nolan", "401", 14, "Library Saturdays"),
            c("Hank", "Osborne", "406", 98, "Ranch next door"),
            c("Clara", "Park", "213", 37, "Piano lessons"),
            c("Rhett", "Quinn", "504", 72, "Jazz brunch"),
            c("Mae", "Rivera", "520", 8, "Hiking buddy"),
            c("Jack", "Sullivan", "617", 55, "Ballpark seats, sec 9"),
            c("Tess", "Turner", "208", 31, "Babysitter"),
            c("Abe", "Vance", "802", 66, "Cider press"),
            c("Lila", "Warren", "971", 19, "Farmers market"),
            c("Finn", "Young", "907", 44, "Cabin keys"),
        )
        .sortedBy { it.last + it.first }
