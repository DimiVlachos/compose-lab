package dev.dimvlachos.lab.bookshelfdemo

import dev.dimvlachos.lab.core.presentation.components.bookshelf.shelf.ShelfBook

/** The demo's library: invented American books, none of them real. */
internal object DemoBooks {
    const val SaltRoad = 1
    const val MarshHollow = 2
    const val BluePlate = 3

    /**
     * On the ring: the books being read, each with its bookmark partway through a chapter, more
     * than fit across the screen. The one read last stands in the middle.
     */
    val reading =
        listOf(
            ShelfBook(
                31,
                "The Ferryman of Puget Sound",
                "Agnes Whitfield",
                "#2E5E6B",
                8.5,
                "Chapter One",
                29,
            ),
            ShelfBook(32, "Dust Bowl Hymnal", "Luther Cobb", "#C9B79C", 6.0, "Chapter One", 36),
            ShelfBook(
                33,
                "The Glassblower of Corning",
                "Ines Bauer",
                "#60263E",
                9.0,
                "Chapter One",
                43,
            ),
            ShelfBook(
                34,
                "Chesapeake Oyster Season",
                "Wade Hollins",
                "#3A4A6B",
                7.5,
                "Chapter One",
                50,
            ),
            ShelfBook(
                35,
                "A Railroad Through Nevada",
                "Pearl Easton",
                "#8A4B2B",
                11.0,
                "Chapter One",
                17,
            ),
            ShelfBook(
                36,
                "The Beekeepers of Asheville",
                "Lorna Pike",
                "#D49E34",
                5.5,
                "Chapter One",
                24,
            ),
            ShelfBook(
                4,
                "The Lighthouse Keeper's Ledger",
                "Mae Holloway",
                "#3A4A6B",
                8.0,
                "Chapter One",
                26,
            ),
            ShelfBook(5, "Tumbleweed Almanac", "Wes Calloway", "#C9B79C", 5.0, "Chapter One", 12),
            ShelfBook(
                MarshHollow,
                "Winter at Marsh Hollow",
                "Caleb Dunn",
                "#7A3B2E",
                7.0,
                "First Snow",
                21,
            ),
            ShelfBook(
                SaltRoad,
                "The Salt Road to Galveston",
                "June Whitaker",
                "#2E5E6B",
                9.5,
                "The Cotton Exchange",
                34,
            ),
            ShelfBook(
                BluePlate,
                "Night Shift at the Blue Plate",
                "Rosa Delgado",
                "#D49E34",
                6.0,
                "Last Call",
                18,
            ),
            ShelfBook(
                6,
                "Letters from Lake Superior",
                "Nora Lindqvist",
                "#8A4B2B",
                10.0,
                "Chapter One",
                39,
            ),
            ShelfBook(
                7,
                "The Orchard on Route 30",
                "Eli Brandt",
                "#60263E",
                12.0,
                "Chapter One",
                55,
            ),
            ShelfBook(
                8,
                "Field Notes from Cedar Point",
                "Harlan Pike",
                "#5C6438",
                11.0,
                "Chapter One",
                47,
            ),
            ShelfBook(
                37,
                "Two Summers on Nantucket",
                "Grace Ellery",
                "#E7DCC4",
                8.0,
                "Chapter One",
                31,
            ),
            ShelfBook(38, "The Ozark Fiddler", "Homer Teague", "#5C6438", 6.5, "Chapter One", 38),
            ShelfBook(
                39,
                "Salt Flats at Bonneville",
                "Ray Castellano",
                "#F4F1EA",
                9.5,
                "Chapter One",
                45,
            ),
            ShelfBook(
                40,
                "The Telegraph Girl of Omaha",
                "Bess Lindgren",
                "#7A3B2E",
                7.0,
                "Chapter One",
                12,
            ),
            ShelfBook(41, "Bluebonnet Country", "Lupe Garza", "#24343A", 10.0, "Chapter One", 19),
            ShelfBook(
                42,
                "Northern Lights over Duluth",
                "Erik Sandvik",
                "#2E5E6B",
                12.0,
                "Chapter One",
                26,
            ),
        )

    /** The top plank: books waiting to be begun. */
    val upNext =
        listOf(
            ShelfBook(11, "Cotton and Iron", "Lydia Marsh", "#24343A", 9.0),
            ShelfBook(12, "The Prairie Schoolhouse", "Amos Reed", "#E7DCC4", 6.5),
            ShelfBook(13, "Bayou Lanterns", "Celeste Fontaine", "#2E5E6B", 8.0),
            ShelfBook(14, "Gold Rush Ledger", "Silas Harte", "#D49E34", 13.0),
            ShelfBook(15, "Cape Ann Weather", "Ruth Coleman", "#F4F1EA", 4.5),
            ShelfBook(16, "Copper Canyon Nights", "Diego Navarro", "#7A3B2E", 10.5),
            ShelfBook(17, "The Last Streetcar", "Frances Kowalski", "#3A4A6B", 7.5),
            ShelfBook(18, "Maple Street Mornings", "Hattie Brooks", "#5C6438", 5.5),
        )

    /** The bottom plank: books already read. */
    val finished =
        listOf(
            ShelfBook(21, "Harbor Lights of Mystic", "Thomas Avery", "#60263E", 8.5),
            ShelfBook(22, "The Long Haul", "Dolores Quinn", "#C9B79C", 11.5),
            ShelfBook(23, "Saturday at the County Fair", "Bud Larson", "#8A4B2B", 6.0),
            ShelfBook(24, "Riverboat Summer", "Clara Jennings", "#24343A", 9.5),
            ShelfBook(25, "Snowbound in Sitka", "Owen Mercer", "#E7DCC4", 7.0),
            ShelfBook(26, "The Quilt Show", "Edna Pruitt", "#D49E34", 4.0),
            ShelfBook(27, "Mile Marker Sixty-One", "Jesse Tolliver", "#2E5E6B", 12.5),
            ShelfBook(28, "Brownstone Letters", "Miriam Katz", "#7A3B2E", 8.0),
        )
}
