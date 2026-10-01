# compose-lab

Compose Multiplatform components, rebuilt from scratch and recorded as short clips. Each component lives in its own package under `composeApp/src/commonMain/kotlin/dev/dimvlachos/lab/` and runs the same on Android and iOS.

## Nav bar (`core/presentation/components/navbar/`)

One bar, five layers that can each be switched on or off. Nothing recomposes while it animates: every animated value is read in the layout or draw phase. The app's demo, `navbar.all`, runs the finished bar; the GIFs below show each layer on its own.

| Layer | What it shows |
|---|---|
| ![](docs/media/navbar.indicator.gif) | **Morphing indicator.** The leading and trailing edges run on different springs. |
| ![](docs/media/navbar.icons.gif) | **Animated icons.** A circular reveal of the filled icon, with a squash and bounce. |
| ![](docs/media/navbar.scroll.gif) | **Collapse on scroll.** A nested-scroll observer shrinks the bar into a floating pill, and settles it to fully open or fully collapsed on release. |
| ![](docs/media/navbar.cutout.gif) | **Moving cutout.** A notch cut with `Path.combine`, following the bubble. The notch itself rides a calm, non-bouncing centre while the bubble leans into its travel and bulges on landing. |
| ![](docs/media/navbar.cutoutmorph.gif) | **Cutout morph.** The notch hugs the bubble as it sinks into the bar, then hands off to an in-bar pill, riding that same calm centre while the bubble leans into its travel and bulges on landing. |
| ![](docs/media/navbar.action.gif) | **Action button.** A jelly button squirts out of the bar on tabs that have one, and shrinks the bar to make room. |
| ![](docs/media/navbar.all.gif) | **Everything together.** |
| ![](docs/media/navbar.recompositions.gif) | **Frames vs recompositions.** Frames climb; the bar recomposes only on a tap. |

```kotlin
val items = listOf(
    NavItem(
        label = "Home",
        icon = Res.drawable.ic_home,
        selectedIcon = Res.drawable.ic_home_filled,
        action = NavAction(Res.drawable.ic_add, "New post"),
    ),
    NavItem(label = "Search", icon = Res.drawable.ic_search, selectedIcon = Res.drawable.ic_search),
    NavItem(label = "Saved", icon = Res.drawable.ic_saved, selectedIcon = Res.drawable.ic_saved_filled),
    NavItem(label = "Profile", icon = Res.drawable.ic_profile, selectedIcon = Res.drawable.ic_profile_filled),
)

val scrollState = rememberNavBarScrollState()

AnimatedNavBar(
    items = items,
    selectedIndex = selected,
    onSelect = { selected = it },
    onActionClick = { index -> /* the tapped tab's action fired */ },
    layers = NavBarLayers.All,
    scrollState = scrollState, // also add Modifier.nestedScroll(scrollState.nestedScrollConnection) to your list
)
```

## Profile gallery (`core/presentation/components/gallery/`)

![](docs/media/morph.app.gif)

One screen where everything that opens is a shared-element morph, all in one `SharedTransitionLayout`:

- **Photo.** A grid card grows into the full-screen photo and back (a container transform). The corners are paired, so the card's 16 dp and the photo's 0 dp meet halfway instead of a square photo showing through a rounded card. The title and caption follow the photo as it grows.
- **Profile photo.** The avatar zooms into a large circle over a scrim. A pencil button then morphs into a "Change profile photo" dialog, and back into the button.
- **Search.** The search button grows into a bar, and search opens as its own page, cross-fading with the gallery. Recent searches show until the query types in, then the matching photos, which glide into place as each letter narrows them.
- **Collapsing header.** As the grid scrolls, the avatar shrinks into a top bar with a shadow, following the finger.

Taps are serialised by a morph gate: one that arrives mid-morph waits for the morph to land, so an interrupted transition never leaves a stray copy behind. Nothing recomposes while anything morphs or scrolls. The system back gesture closes the topmost layer.

```kotlin
ProfileGallery(
    name = "Alex Morgan",
    portrait = Res.drawable.portrait,
    photos = photos, // List<MorphPhoto>(image, title, caption)
    searchQuery = "xos",
    recentSearches = listOf("Santorini", "Milos", "Hydra"),
    scene = scene, // GalleryScene(photo, avatar, dialog, search)
    onSceneChange = { scene = it },
)
```

The photo morph also ships on its own as `ImageMorph` (`core/presentation/components/imagemorph/`), with `MorphLayers` to switch its fixes on one by one.

## Fogged mirror (`core/presentation/components/fog/`)

![](docs/media/fog.mirror.bathroom.gif)

A steamed-up bathroom mirror, hanging on a tiled wall above the basin, that you wipe clear with a finger. Two versions share one component:

- **Bathroom** (`fog.mirror.bathroom`): a still bathroom behind the glass. It asks for nothing, and mists slowly back over once you stop wiping.
- **Your reflection** (`fog.mirror.camera`): the live front camera behind the glass, so wiping finds your face, standing in the bathroom: on Android, ML Kit's selfie segmentation cuts you out of each frame on the phone itself, and the bathroom shows behind you instead of your own room. Like the bathroom, it mists slowly back over once you stop wiping. One card explains why it needs the camera before asking; without it, the mirror shows the bathroom's still reflection.

What's on the glass:

- **Fog from a real photo.** Condensation texture from a photo of wet glass, over a blurred, milky copy of the scene behind it.
- **Soft wipes.** Soft-edged strokes follow the finger.
- **Mist.** Once you stop wiping, the steamy room slowly fogs the glass evenly back over. (`FogState` can also fill fog back in from the bottom up, like a breath; the demo doesn't use it.)
- **Running drops.** Now and then a drop gathers, in a range of small sizes, and runs down. The bigger it is, the faster it goes. As it runs it stretches from a sphere into a teardrop, and it cuts a clear trail through the fog behind it. Once it stops, it settles softly into the condensation.
- **Drops meeting a wipe.** A drop that runs into a wiped patch slips just inside, then spreads out slowly and thins away into the wet glass.
- **The clip.** Drops run first, then a hand scrubs a porthole clear, smearing away the drop resting in its path. A soft fingertip marks each touch, like a phone's "show taps". One more drop runs into the porthole and spreads away, then the room mists it all back over, so the clip loops. Replay in **Your reflection** plays the same showcase over your live face, ending with the same mist.

```kotlin
val fog = remember { FogState() }                   // starts fully fogged
val drips = remember(fog) { DripDriver(fog) }       // set drips.glass to the glass's size in dp,
                                                    // then drips.advance(seconds) on each frame

FoggedWindow(
    photo = painterResource(Res.drawable.mirror_view), // or the camera's live Painter
    state = fog,
    beads = { drips.beads },                         // read while drawing: a running drop only redraws
)
```

### Made with Compose

Compose made every part of this simple to build, and the same Kotlin runs on Android and iOS:

- **Blend modes are the whole fog.**
  - Inside an offscreen `graphicsLayer` (`CompositingStrategy.Offscreen`), `BlendMode.DstOut` wipes holes in the fog, `Overlay` lays the condensation texture on top, and `DstIn` thins it.
  - Breath and mist *fill back in* rather than pile up: `DstOut`, then `Plus` through a `SrcIn` mask, in `saveLayer` from `drawIntoCanvas`.
  - That's plain Porter–Duff maths, the same on both platforms.
- **`Modifier.blur` and `ColorFilter.colorMatrix`** turn one `Painter` into the frosted copy of the scene. The camera is just another `Painter`, so the live mirror needed no special path.
- **Drops are drawn with ordinary draw calls.** Each teardrop is a `Path` of two cubics and an arc, drawn longer the faster it runs. Its shade, rim, edge light and glint are a filled path, strokes, an arc and a circle, and a settled drop gets a soft `Brush.radialGradient` halo. `withTransform` spreads it as it melts into a wipe.
- **Drawing never triggers recomposition.** Beads are handed over as a lambda and read in `drawBehind`. The fog's marks are a `mutableStateListOf` read in the draw phase, and camera frames sit in snapshot state that only `onDraw` reads. So a running drop or a new frame costs a redraw, not a recomposition.
- **Clocks and gestures are coroutines.**
  - `withFrameNanos` drives the drops and the mist.
  - `snapshotFlow` wakes the drip loop the moment you wipe, and otherwise lets the glass sleep.
  - `pointerInput` with `awaitEachGesture` tells a wipe from a hold.
  - `repeatOnLifecycle` keeps the camera on only while the screen is showing.
- **Everything is testable.** `runComposeUiTest` plus `captureToImage` read real pixels: a drop is darker in the middle, a spread one is thinner and wider, and a wipe clears the fog. The drips and the mist run on the test's own frame clock, so a test steps through many seconds of drops frame by frame, exactly the same every run.
- **The mirror hangs on a real wall with ordinary layout.** `BoxWithConstraints` measures the screen. An `Image` with `ContentScale.Crop` and a custom `Alignment` crops the wall photo around the mirror. The glass is a `Box` placed with `Modifier.offset` and `size`, and clipped with `RoundedCornerShape`. So the fog lands exactly on the photo's glass on any screen, and a finger on the tiles wipes nothing.
- **A cut-out is just a blend mode.** The camera's frame and the person mask go into one layer, and `BlendMode.DstIn` keeps only you. A small `Painter` then draws the bathroom behind you, and the fog takes the result like any other picture.
- **`expect`/`actual`** keeps the platform code small: CameraX on Android, a stub on iOS. Everything you see is common code.

What makes Compose so good for this is that nothing here needed a custom view, an OpenGL shader or a platform escape hatch. The fog, the water and the wall are ordinary composables, modifiers and draw calls, laid out, animated and tested like any other screen, and one codebase draws them on both platforms. Compose makes a surface that feels physical and alive just another piece of UI.

## Run

Prerequisites: macOS with Xcode for iOS and for the iOS tests (`iosSimulatorArm64Test`), an Android SDK (`ANDROID_HOME` or `local.properties`'s `sdk.dir`), Python 3 and ffmpeg for recording, and [XcodeGen](https://github.com/yonaskolb/XcodeGen) for iOS.

- Android: `./gradlew :androidApp:installDebug`
- iOS: boot a simulator, then run `scripts/install-ios.sh`
- Tests: `./gradlew :composeApp:iosSimulatorArm64Test`
- Architecture (Konsist): `./gradlew :androidApp:testDebugUnitTest`
- Formatting: `./gradlew spotlessApply`

## Record a clip

Every demo plays a timed script that ends where it started, so clips loop cleanly.

```bash
scripts/record.py android navbar.all              # out/navbar.all-android.mp4 (1080×1350, 60 fps, ≤ 5 MB)
scripts/record.py ios navbar.all
scripts/record.py android navbar.all --label && scripts/record.py ios navbar.all --label
scripts/side-by-side.sh navbar.all                 # out/navbar.all-both.mp4
scripts/gif.sh navbar.all                          # docs/media/navbar.all.gif
```

Needs Python 3 and ffmpeg.

## Envelope

The nav bar supports 2–4 tabs, at a minimum slot width of about 64 dp.

The profile gallery shows photos in 2 columns of 4:5 cards. Opening one photo straight from another is not a morph: close to the grid first.

## Licence

MIT. Icon path data comes from Material Icons (Apache 2.0).

Photos from [Unsplash](https://unsplash.com), used under the [Unsplash licence](https://unsplash.com/license):

| Photo | Photographer |
|---|---|
| [Portrait](https://unsplash.com/photos/DItYlc26zVI) | Christian Buehner |
| [Corfu](https://unsplash.com/photos/vhRn4aDempw) | Tobias Reich |
| [Paxos](https://unsplash.com/photos/bs1aa6L9rPM) | Luke Moss |
| [Santorini](https://unsplash.com/photos/6N2mSJsKTtA) | James Ting |
| [Milos](https://unsplash.com/photos/0lq90N_B1XE) | Despina Galani |
| [Naxos](https://unsplash.com/photos/dGD7R9R4ZiI) | Sebastiano Corti |
| [Hydra](https://unsplash.com/photos/jfnTAhvysZI) | Despina Galani |
| [Mykonos](https://images.unsplash.com/photo-1601581875309-fafbf2d3ed3a) | Johnny Africa |
| [Crete](https://images.unsplash.com/photo-1575237402880-4b496a83ae04) | Joshua Kettle |
| [Rhodes](https://images.unsplash.com/photo-1572375901777-1b257481cbb0) | Vlad Kiselov |
| [Zakynthos](https://images.unsplash.com/photo-1612279427382-f8349a383af8) | Julian Timmerman |
| [Kefalonia](https://images.unsplash.com/photo-1598959594958-34761147b7b0) | Mac McDade |
| [Folegandros](https://images.unsplash.com/photo-1688765866663-0fd353e7d5df) | Tom Waldek |

Photos from [Pexels](https://www.pexels.com), used under the [Pexels licence](https://www.pexels.com/license/):

| Photo | Photographer |
|---|---|
| [Houseplants in pots standing in a bathtub](https://www.pexels.com/photo/15618010/) (the fogged mirror's reflection without a camera) | nana |
| [Contemporary bathroom interior with mirror above washbasin at home](https://www.pexels.com/photo/7046159/) (the wall the fogged mirror hangs on, extended with more tiles above and a counter front below) | Max Vakhtbovych |
| [Water droplets on foggy glass](https://www.pexels.com/photo/water-droplets-on-foggy-glass-8628343/) (the fog's condensation, via `scripts/fog-texture.py`) | Chris F |
