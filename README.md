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

## Licence

MIT. Icon path data comes from Material Icons (Apache 2.0).
