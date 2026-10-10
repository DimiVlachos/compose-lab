package dev.dimvlachos.lab.core.presentation.ui

import androidx.compose.ui.graphics.Color

/** The inks and papers of the Cyclades pop-up book. */
internal object CycladesPaper {
    // Paper and print.
    val paper = Color(0xFFF5EFE2)
    val speck = Color(0xFF786446)
    val gutterShade = Color(0x5746341E)
    val printTitle = Color(0xFF1D4F86)
    val printLine = Color(0xFF4A5560)
    val pullLabel = Color(0xFF7D6B4F)
    val slit = Color(0x8C3C2D19)
    val outline = Color(0x73504432)

    // The cloth boards.
    val cloth = Color(0xFF1B4B80)
    val clothThread = Color(0x0DFFFFFF)
    val gilt = Color(0xFFE2CC8E)
    val giltBright = Color(0xFFE8C96D)
    val coverTitle = Color(0xFFF7F1E3)

    // Skies.
    val skyTop1 = Color(0xFF8FC2EA)
    val skyMid1 = Color(0xFFD8EAF3)
    val skyLow1 = Color(0xFFF2EFE6)
    val sun = Color(0xFFF5CF62)
    val sunRay = Color(0xFFEFC24E)
    val bird1 = Color(0xFF4A6A86)
    val skyTop2 = Color(0xFFA6D1F0)
    val skyLow2 = Color(0xFFEEF2EF)
    val cloud = Color(0xD9FFFFFF)
    val bird2 = Color(0xFF52708A)
    val duskTop = Color(0xFFB88AB0)
    val duskMid = Color(0xFFEE9F73)
    val duskLow = Color(0xFFF8D7A4)
    val bird3 = Color(0xFF5B3F55)

    // Sea and shore.
    val seaDeep = Color(0xFF2B70B3)
    val seaShallow = Color(0xFF4B93CF)
    val foam = Color(0x8CFFFFFF)
    val foamFaint = Color(0x52FFFFFF)
    val sand = Color(0xFFEFE1BF)
    val paving = Color(0xFFE4D8C4)
    val pavingLine = Color(0xFFFBF7EF)
    val openSeaTop = Color(0xFF1A548E)
    val openSeaLow = Color(0xFF3B88C8)
    val waveTop = Color(0xFF2F75B8)
    val waveLow = Color(0xFF1D5290)
    val waveCrest = Color(0xD9FFFFFF)
    val waveFaint = Color(0x59FFFFFF)

    // Meadow.
    val meadow = Color(0xFFD4B97F)
    val path = Color(0xFFECDCB6)
    val shrub = Color(0xFF6F7D3E)
    val shrubLight = Color(0xFF8E9A52)
    val pebble = Color(0xFFB9AB90)

    // Whitewash, blue and stone.
    val whitewash = Color(0xFFFBF9F4)
    val whitewashWarm = Color(0xFFFBF8F2)
    val aegean = Color(0xFF2C69B0)
    val domeDark = Color(0xFF2A62A6)
    val domeLight = Color(0xFF3F80C8)
    val domeDeep = Color(0xFF1F4F8A)
    val doorway = Color(0xFF3A4652)
    val bell = Color(0xFFC4943F)
    val wallShade = Color(0x0F283246)
    val hillsFar = Color(0xFFC9B88D)
    val hillsNear = Color(0xFFA7A46C)
    val leaf = Color(0xFF5F8A3B)
    val leafDark = Color(0xFF4F7D33)
    val bougainvillea =
        listOf(Color(0xFFC8327A), Color(0xFFE0559A), Color(0xFFA82466), Color(0xFFD9437F))
    val islandFar = Color(0xFFB8A7C7)
    val islandNear = Color(0xFF9D8DB0)
    val rock = Color(0xFF9B8A74)
    val rockCrack = Color(0x593C2D1E)
    val towerShade = Color(0x12283246)
    val lamp = Color(0xFFF3D36E)
    val mast = Color(0xFF6B4A2B)
    val hullStripe = Color(0xFFC4572F)
    val sunGlow = Color(0x8CF6B26B)
    val sunsetSun = Color(0xFFF08A4B)
    val ridgeFar = Color(0xFF7A6188)
    val ridgeNear = Color(0xFF56456B)
    val towerLight = Color(0xFFFFFDF8)
    val towerLeft = Color(0xFFE6E0D4)
    val towerRight = Color(0xFFD6CFC0)
    val thatch = Color(0xFF8A6A42)
    val thatchLine = Color(0xB3503A20)
    val sailCloth = Color(0xFFFBF8F1)
    val hub = Color(0xFF5A3F25)
    val cactus = Color(0xFF6F9A4A)
    val cactusFlower = Color(0xFFC2344A)
    val spine = Color(0xCCFAF5DC)
    val drystone = Color(0xFFCFC4B1)
    val stoneDark = Color(0xFFBDB19C)
    val stoneLight = Color(0xFFD8CDB9)

    // The tour's own screen: Aegean ink and buttons.
    val ink = Color(0xFF1B2E4A)
    val inkMuted = Color(0xFF5E6878)
    val inkFaint = Color(0xFFC9BEAA)
    val button = Color(0xFF1F5FA8)
    val onButton = Color(0xFFFFFFFF)

    // The sky behind the tour, one for each moment of it: the cover at dawn, the islands at noon,
    // the crossing over open sea, the meltemi at sunset, and the farewell in the golden hour. Each
    // fades from a sky at the top to light cream at the foot, where the caption is read.
    val skies =
        listOf(
            CycladesSky(Color(0xFFCADDEB), Color(0xFFF1ECE3), Color(0xFFF8F2E8), Color(0xFFFFE3BC)),
            CycladesSky(Color(0xFF9FCBEC), Color(0xFFE6F1F4), Color(0xFFF7F1E5), Color(0xFFFFF8E8)),
            CycladesSky(Color(0xFF86BCE4), Color(0xFFD7EAF3), Color(0xFFF2F1EB), Color(0xFFF4FBFF)),
            CycladesSky(Color(0xFFD7B3CF), Color(0xFFF5D3BD), Color(0xFFFBEBDC), Color(0xFFFFBF86)),
            CycladesSky(Color(0xFFF0C59C), Color(0xFFF8E2C6), Color(0xFFFCF1E2), Color(0xFFFFD27A)),
        )
}

/**
 * A sky behind the pop-up tour: [top] fading through [middle] to [bottom], and the sun's [glow] in
 * the top corner, where the book's own suns are.
 */
internal class CycladesSky(val top: Color, val middle: Color, val bottom: Color, val glow: Color)
