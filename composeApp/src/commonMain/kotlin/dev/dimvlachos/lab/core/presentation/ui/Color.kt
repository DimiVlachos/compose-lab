package dev.dimvlachos.lab.core.presentation.ui

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * The lab's colours, in a dark palette ([Dark], the defaults) and a light one ([Light]), with a
 * violet accent in both.
 *
 * Two kinds of token live here. The themed ones are the app's own surfaces and text, and the
 * surroundings each demo stands on (the book's table, the magnets' steel table and rail, the
 * fishing rod and line); [Light] gives each of them a light value. The art is the demos' own
 * objects, which look the same whatever the theme: the book's pages and thread, the pop-up's card,
 * the lamp, the photo prints and magnets, the water, the mirror's glass and the text over a photo.
 * [Light] leaves those as they are. The demos' tokens are marked one way or the other where they
 * are declared.
 *
 * @property isDark Whether this is the dark palette.
 * @property background Behind every screen.
 * @property surface Cards and dialogs.
 * @property bar The nav bar and the search bar.
 * @property textPrimary Text and icons.
 * @property textMuted Secondary text and unselected icons.
 * @property accent The selected tab, buttons and your own chat bubbles.
 * @property accentSoft The accent, faint: the nav bar's indicator.
 * @property onAccent Text and icons on the accent.
 * @property topBar The top bar over every screen, lifted a little off the background.
 * @property topBarShadow The shadow the top bar casts.
 * @property chipBorder The outline of an unselected filter chip.
 * @property selection Behind selected text, under the accent's handles.
 * @property sendDisc A disc behind the chat's send dart, so it reads on a light screen; none in the
 *   dark palette.
 * @property mirrorGlass Art: the fogged mirror's glass before the camera comes on.
 * @property onPhoto Art: text over a photo's dark scrim.
 * @property onPhotoMuted Art: secondary text over a photo's dark scrim.
 */
@Immutable
public data class AppColors(
    val isDark: Boolean = true,
    val background: Color = Color(0xFF0B0D12),
    val surface: Color = Color(0xFF171A22),
    val bar: Color = Color(0xFF232733),
    val textPrimary: Color = Color(0xFFE8EAF0),
    val textMuted: Color = Color(0xFF8A90A0),
    val accent: Color = Color(0xFF7C5CFF),
    val accentSoft: Color = Color(0x337C5CFF),
    val onAccent: Color = Color(0xFFFFFFFF),
    val topBar: Color = Color(0xFF12151C),
    val topBarShadow: Color = Color(0xFF000000),
    val chipBorder: Color = Color(0xFF2E3340),
    val selection: Color = Color(0x667C5CFF),
    val sendDisc: Color = Color.Transparent,
    val mirrorGlass: Color = Color(0xFF171A22),
    val onPhoto: Color = Color(0xFFF2F3F7),
    val onPhotoMuted: Color = Color(0xFFC9CDD6),
    // Art. The open book: the shade across a lifted leaf, the sheen on it, the shadow in the
    // gutter, the binding thread and the crease down the spine, all warm like paper under a lamp.
    val pageShade: Color = Color(0xFF2E2416),
    val pageGlare: Color = Color(0xFFFFF8EC),
    val bookGutter: Color = Color(0xFF2B2014),
    val bookCrease: Color = Color(0xFF4F412C),
    // The paper edges of the sheets under the pages.
    // The binding thread in the centre fold: unbleached linen, and the darker grain of its twist.
    val thread: Color = Color(0xFFE6DAC0),
    val threadTwist: Color = Color(0xFF9A8662),
    val pageEdge: Color = Color(0xFFEDE3CB),
    val pageEdgeLine: Color = Color(0xFFA8926A),
    // The back of a pop-up piece: plain white card, its print faintly showing through.
    val paperBack: Color = Color(0xFFF8F5EE),
    // Themed. The table the book demo lies on, lit from above its centre (dark walnut, or light
    // oak in the light palette), and the scripted finger.
    val table: Color = Color(0xFF2B1F17),
    val tableLit: Color = Color(0xFF4A3727),
    val touch: Color = Color(0xFFFFFFFF),
    // The chat: the bubbles of the messages that come in (themed), and the writing paper its
    // planes are folded from (art).
    val bubbleTheirs: Color = Color(0xFF262A36),
    val paper: Color = Color(0xFFF3F1EC),
    // Art. The pull-cord lamp: its green enamel shade, the brass of its rod, rim and bead, the
    // cotton cord, the bulb dark and lit, and the warm light it throws. It hangs over a light
    // screen and a dark one alike, so it reads on both.
    val lampShade: Color = Color(0xFF2E4A3E),
    val lampShadeSheen: Color = Color(0xFF5F8273),
    val lampBrass: Color = Color(0xFFB8945A),
    val lampCord: Color = Color(0xFF8E8676),
    val bulbOff: Color = Color(0xFFD9D3C4),
    val bulbLit: Color = Color(0xFFFFF6DC),
    val lampLight: Color = Color(0xFFFFD592),
    // The magnet filter. Themed: a steel table, deeper towards its foot (brushed aluminium in the
    // light palette), and the rail the magnets wait on with a well for each; the iron filings
    // dusted over it; the shadows under the photo prints, and the scrim behind a fanned-out
    // cluster. Art: the prints themselves and the metal clips across their tops; the magnets' red
    // faces, steel rims and glint, and the count badge over a magnet.
    val tableSteel: Color = Color(0xFF3B4048),
    val tableSteelDeep: Color = Color(0xFF22262C),
    val stripRail: Color = Color(0xFF15181C),
    val stripWell: Color = Color(0xFF2A2E35),
    val filing: Color = Color(0xFFC9CED6),
    val cardPaper: Color = Color(0xFFF4F1EA),
    val cardShadow: Color = Color(0x66000000),
    val clipMetal: Color = Color(0xFF9AA1AB),
    val clipSheen: Color = Color(0xFFE3E7EC),
    val magnetRed: Color = Color(0xFFC8372D),
    val magnetSteel: Color = Color(0xFF8B929C),
    val magnetSheen: Color = Color(0xFFFFFFFF),
    val badge: Color = Color(0xFFF4F1EA),
    val onBadge: Color = Color(0xFF1B1D21),
    val fanScrim: Color = Color(0xFF0E1013),
    // Art. The magnet filter demo's tags, one colour each for its magnets and count badges: a warm
    // dusk for Sunset, the Aegean for Sea, a harbour teal for Boats, warm stone for Cliffs and
    // bougainvillea for Village. Deep enough that the white glint and the steel rim read on them.
    val tagSunset: Color = Color(0xFFE0712C),
    val tagSea: Color = Color(0xFF2B7BC4),
    val tagBoats: Color = Color(0xFF23998A),
    val tagCliffs: Color = Color(0xFFA27A5C),
    val tagVillage: Color = Color(0xFFC4426E),
    // The fishing refresh. Art: a band of clear Aegean water, turquoise at its top and deep blue
    // below, see-through so the list under it shows, tinted, and the sunlight that glints on it
    // and slants down into it; a cork handle and a red-and-white bobber. Themed: the crest where
    // the water meets the air, and the glossy rod, its reel and the line, pale on the dark screen
    // and darker on the light one.
    val waterTop: Color = Color(0xB32FB3CB),
    val waterDeep: Color = Color(0xE60B4A75),
    val waterLight: Color = Color(0xFFE6FBFF),
    val waterCrest: Color = Color(0xFFDDF3FF),
    val rodCork: Color = Color(0xFFB98B5E),
    val rodBlank: Color = Color(0xFFB9C3CC),
    val rodReel: Color = Color(0xFF9AA1AB),
    val fishingLine: Color = Color(0xFFE9EEF2),
    val bobberRed: Color = Color(0xFFE0402F),
    val bobberWhite: Color = Color(0xFFF7F4EE),
    // The typewriter composer. Themed: its sheet of paper and the slips sent from it, a shade
    // dimmer on the dark screen, with the darker paper of its edges; and the machine's green
    // enamel, lit at its top and deep at its foot. Art: the ink, the black platen and its sheen,
    // and the ribbon's black and red halves.
    val typePaper: Color = Color(0xFFE8E0CB),
    val typePaperEdge: Color = Color(0xFFCFC5AB),
    val enamelLight: Color = Color(0xFF6F9A91),
    val enamel: Color = Color(0xFF4F7A71),
    val enamelDeep: Color = Color(0xFF335851),
    val typeInk: Color = Color(0xFF1F1D1B),
    val platen: Color = Color(0xFF0C0D0F),
    val platenSheen: Color = Color(0xFF5A6066),
    val ribbonBlack: Color = Color(0xFF17181A),
    val ribbonRed: Color = Color(0xFFA8141C),
) {
    public companion object {
        /** The dark palette: the constructor's defaults. */
        public val Dark: AppColors = AppColors()

        /** The light palette: the themed tokens lightened, the art left as it is. */
        public val Light: AppColors =
            AppColors(
                isDark = false,
                background = Color(0xFFF2F2F7),
                surface = Color(0xFFFFFFFF),
                bar = Color(0xFFE4E2EE),
                textPrimary = Color(0xFF15171E),
                textMuted = Color(0xFF5C6270),
                accent = Color(0xFF6542E6),
                accentSoft = Color(0x2E6542E6),
                onAccent = Color(0xFFFFFFFF),
                topBar = Color(0xFFFFFFFF),
                topBarShadow = Color(0xFF000000),
                chipBorder = Color(0xFFD5D3DE),
                selection = Color(0x526542E6),
                sendDisc = Color(0xFF6542E6),
                table = Color(0xFFAE8862),
                tableLit = Color(0xFFD6B386),
                touch = Color(0xFF15171E),
                bubbleTheirs = Color(0xFFE4E2EE),
                tableSteel = Color(0xFFC3C8CF),
                tableSteelDeep = Color(0xFFA3A9B2),
                stripRail = Color(0xFFE1E4E8),
                stripWell = Color(0xFFBFC4CB),
                filing = Color(0xFF3E434B),
                cardShadow = Color(0x38000000),
                fanScrim = Color(0xFFEEF0F3),
                waterCrest = Color(0xFFB5E9F5),
                rodBlank = Color(0xFF6B7682),
                rodReel = Color(0xFF707884),
                fishingLine = Color(0xFF4A535E),
                typePaper = Color(0xFFF8F3E6),
                typePaperEdge = Color(0xFFE9E1CC),
                enamelLight = Color(0xFFA3CDC3),
                enamel = Color(0xFF78AA9F),
                enamelDeep = Color(0xFF4E8075),
            )
    }
}
