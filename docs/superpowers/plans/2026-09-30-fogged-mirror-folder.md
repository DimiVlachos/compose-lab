# Fogged Mirror Folder Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Split the fogged mirror into two versions, Bathroom (still photo, mic only, the recorded clip)
and Your reflection (live camera), shown inside one "Fogged mirror" folder card.

**Architecture:**
- The two versions are thin entries in `fogdemo/bathroom` and `fogdemo/reflection` over the shared
  `FogDemo`, which gains a `reflection` flag for its card wording.
- The catalog gains a `CatalogEntry` (a single demo or a group); `AppNavigation` gains a group level;
  `CatalogScreen` is reused for the folder screen.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform 1.11.1, kotlin.test, compose ui-test v2.

**Spec:** `docs/superpowers/specs/2026-09-30-fogged-mirror-folder-design.md`

## Global Constraints

- **Ids:** `fog.mirror.bathroom` and `fog.mirror.camera`; the group's id is `fog.mirror`.
- **Titles:** "Fogged mirror" (the group, `demo_fog`), "Bathroom" (`demo_fog_bathroom`) and "Your
  reflection" (`demo_fog_reflection`), with typographic apostrophes in strings.
- **Bathroom never uses the camera:** `CameraAccess.Unavailable`, and its card never mentions it.
- **Project rules:**
  - core never imports feature packages (catalog, demo, navbardemo, gallerydemo, fogdemo) or App
  - previews private, no `Color(0x` outside `core.presentation.ui`
  - run `./gradlew spotlessApply` before each commit
- **Commands:**
  - tests: `./gradlew :composeApp:iosSimulatorArm64Test`
  - Konsist: `./gradlew :androidApp:testDebugUnitTest --rerun`
  - build: `./gradlew :androidApp:assembleDebug`
- **Commit trailer:**
  ```
  Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
  Claude-Session: https://claude.ai/code/session_01TkrycYKRAwVDjLBuvHiu6E
  ```

## Review Focus

1. **Recording by id:** `record=true` with `fog.mirror.bathroom` must open that demo directly, with no
   folder in between and no way to leave. Task 2 test.
2. **System back on the folder screen:** it goes home rather than leaving the app. Task 2 test.
3. **A demo opened from the folder:** Back returns to the folder, not home. Task 2 test.
4. **The Bathroom card with the mic already granted:** no card at all, since there's nothing to ask.
   Task 1 test.
5. **Bathroom in the recording:** no card and no camera, the still photo only. Covered by the existing
   `noCardInTheRecording` and camera tests; Task 1 asserts that Bathroom passes `Unavailable`.

---

### Task 1: Two versions of the fogged mirror

**Files:**
- Create: `composeApp/src/commonMain/kotlin/dev/dimvlachos/lab/fogdemo/bathroom/BathroomMirror.kt`
- Create: `composeApp/src/commonMain/kotlin/dev/dimvlachos/lab/fogdemo/reflection/ReflectionMirror.kt`
- Modify:
  - `fogdemo/FogDemos.kt` (two demos)
  - `fogdemo/presentation/components/FogDemo.kt` (`reflection` parameter, passed to the card)
  - `fogdemo/presentation/components/MirrorPermissionCard.kt` (the `reflection` wording)
  - `composeResources/values/strings.xml`
- Tests:
  - `fogdemo/FogDemosTest.kt`, where `.single()` becomes the Bathroom demo
  - `fogdemo/presentation/components/FogDemoCardUiTest.kt`, plus a Bathroom card test
  - `catalog/CatalogTest.kt`, with the new ids and titles

**Interfaces:**
- Produces:
  - `FogDemos.all: List<Demo>`, which is `[bathroom, camera]`
  - `FogDemo(state, fog, micAccess, cameraAccess, reflection: Boolean = true)`
  - `MirrorPermissionCard(camera, mic, onAllow, onOpenSettings, onNotNow, modifier, reflection: Boolean = true)`

- [ ] **Step 1: Write the failing tests**

In `CatalogTest.keepsTheFinishedDemoOfEachComponent`:

```kotlin
        assertEquals(
            listOf("navbar.all", "morph.app", "fog.mirror.bathroom", "fog.mirror.camera"),
            Catalog.demos.map { it.id },
        )
        assertEquals(
            listOf(
                Res.string.demo_navbar,
                Res.string.demo_morph_app,
                Res.string.demo_fog_bathroom,
                Res.string.demo_fog_reflection,
            ),
            Catalog.demos.map { it.title },
        )
```

Swap the imports: `demo_fog` becomes `demo_fog_bathroom` and `demo_fog_reflection`.

In `FogDemosTest`, replace every `FogDemos.all.single()` with `FogDemos.bathroom`, and add:

```kotlin
    @Test
    fun bothVersionsPlayTheSameClip() {
        val (bathroom, camera) = FogDemos.all
        assertEquals("fog.mirror.bathroom", bathroom.id)
        assertEquals("fog.mirror.camera", camera.id)
        assertTrue(bathroom.script === camera.script)
        assertFalse(camera.autoplay)
    }
```

Append to `FogDemoCardUiTest`. `showBathroom` sits beside `showDemo`, and the imports needed are the
`mirror_card_body_what_bathroom` and `mirror_card_body_what` resources:

```kotlin
    private fun ComposeUiTest.showBathroom() {
        setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides ResumedOwner()) {
                LabTheme {
                    Box(Modifier.size(400.dp, 700.dp)) {
                        FogDemo(
                            DemoState(),
                            FogState(),
                            mic,
                            CameraAccess.Unavailable,
                            reflection = false,
                        )
                    }
                }
            }
        }
    }

    @Test
    fun theBathroomCardExplainsOnlyTheMicrophone() = runComposeUiTest {
        mic = askableMic()
        showBathroom()

        onNodeWithText(text(Res.string.mirror_card_body_what_bathroom)).assertExists()
        onNodeWithText(text(Res.string.mirror_card_body_what)).assertDoesNotExist()
        onNodeWithText(micWhy).assertExists()
        onNodeWithText(cameraWhy).assertDoesNotExist()
        onNodeWithText(allow).performTouchInput { click() }
        waitForIdle()
        assertEquals(listOf("mic"), asked)
    }

    @Test
    fun theBathroomShowsNoCardOnceTheMicrophoneIsGranted() = runComposeUiTest {
        mic = grantedMic()
        showBathroom()
        onNodeWithText(title).assertDoesNotExist()
    }
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew :composeApp:iosSimulatorArm64Test --tests '*CatalogTest*' --tests '*FogDemosTest*' --tests '*FogDemoCardUiTest*'`
Expected: compilation FAILS with unresolved `demo_fog_bathroom`, `FogDemos.bathroom`, `reflection`
and `mirror_card_body_what_bathroom`.

- [ ] **Step 3: Implement**

In `strings.xml`, after `demo_fog`:

```xml
    <string name="demo_fog_bathroom">Bathroom</string>
    <string name="demo_fog_reflection">Your reflection</string>
```

After `mirror_card_body_what`:

```xml
    <string name="mirror_card_body_what_bathroom">Wipe the steam away with your finger, then blow at the bottom of your phone to fog it up again.</string>
```

Create `fogdemo/bathroom/BathroomMirror.kt`:

```kotlin
package dev.dimvlachos.lab.fogdemo.bathroom

import androidx.compose.runtime.Composable
import dev.dimvlachos.lab.core.camera.CameraAccess
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.fogdemo.presentation.components.FogDemo

/**
 * The fogged mirror of a steamy bathroom: a still reflection, the one the clip is recorded from.
 * It never asks for the camera, only for the microphone that hears a breath.
 */
@Composable
internal fun BathroomMirror(state: DemoState) {
    FogDemo(state, cameraAccess = CameraAccess.Unavailable, reflection = false)
}
```

Create `fogdemo/reflection/ReflectionMirror.kt`:

```kotlin
package dev.dimvlachos.lab.fogdemo.reflection

import androidx.compose.runtime.Composable
import dev.dimvlachos.lab.core.demo.DemoState
import dev.dimvlachos.lab.fogdemo.presentation.components.FogDemo

/** The fogged mirror with you in it: the front camera behind the fog. */
@Composable
internal fun ReflectionMirror(state: DemoState) {
    FogDemo(state)
}
```

In `FogDemos.kt`, replace `all`:

```kotlin
    // On the phone the glass is the user's to breathe on and wipe; Replay plays the clip.
    val bathroom = Demo("fog.mirror.bathroom", Res.string.demo_fog_bathroom, wipeTour, autoplay = false) {
        BathroomMirror(it)
    }

    val reflection = Demo("fog.mirror.camera", Res.string.demo_fog_reflection, wipeTour, autoplay = false) {
        ReflectionMirror(it)
    }

    val all: List<Demo> = listOf(bathroom, reflection)
```

Imports: swap `demo_fog` for `demo_fog_bathroom` and `demo_fog_reflection`, add `BathroomMirror` and
`ReflectionMirror`, and remove the now-unused `FogDemo` import.

In `FogDemo.kt`, add the parameter after `cameraAccess`:

```kotlin
    cameraAccess: CameraAccess = rememberCameraAccess(enabled = !state.recording),
    reflection: Boolean = true,
) {
```

Pass it to the card call: `reflection = reflection,`.

In `MirrorPermissionCard.kt`:
- add `reflection: Boolean = true,` after `modifier`
- replace the body line with:

```kotlin
        CardText(
            if (reflection) Res.string.mirror_card_body_what
            else Res.string.mirror_card_body_what_bathroom,
            LabTheme.colors.textPrimary,
        )
```

- import `mirror_card_body_what_bathroom`

- [ ] **Step 4: Run the tests to verify they pass**

Until Task 2 adds the folder, the home screen shows "Bathroom" and "Your reflection" as plain cards.
So in `AppSmokeTest.theCatalogOpensEachDemoDirectly`, change `onNodeWithText("Fogged mirror")` to
`onNodeWithText("Bathroom")`; Task 2 changes it back.

Run: `./gradlew spotlessApply :composeApp:iosSimulatorArm64Test`
Expected: BUILD SUCCESSFUL, all tests pass.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: the fogged mirror comes in two versions, Bathroom and Your reflection"
```

---

### Task 2: A folder card in the catalog

**Files:**
- Create: `composeApp/src/commonMain/kotlin/dev/dimvlachos/lab/catalog/CatalogEntry.kt`
- Modify: `catalog/Catalog.kt`, `AppNavigation.kt`, `App.kt` and
  `catalog/presentation/screen/CatalogScreen.kt`
- Tests: `AppNavigationTest.kt`, `AppSmokeTest.kt` and `catalog/CatalogTest.kt`

**Interfaces:**
- Consumes: `FogDemos.all` (Task 1).
- Produces:
  - `sealed interface CatalogEntry { class Single(val demo: Demo); class Group(val id: String, val title: StringResource, val demos: List<Demo>) }`
  - `Catalog.entries: List<CatalogEntry>`
  - `AppNavigation.group`, plus `openGroup(group)`
  - `CatalogScreen(title: String, entries: List<CatalogEntry>, onOpenDemo: (Demo) -> Unit, onOpenGroup: (CatalogEntry.Group) -> Unit, onBack: (() -> Unit)? = null)`

- [ ] **Step 1: Write the failing tests**

Append to `CatalogTest`:

```kotlin
    @Test
    fun theFoggedMirrorIsAFolderOfItsTwoVersions() {
        val group = Catalog.entries.filterIsInstance<CatalogEntry.Group>().single()
        assertEquals("fog.mirror", group.id)
        assertEquals(Res.string.demo_fog, group.title)
        assertEquals(listOf("fog.mirror.bathroom", "fog.mirror.camera"), group.demos.map { it.id })
        assertEquals(
            listOf("navbar.all", "morph.app"),
            Catalog.entries.filterIsInstance<CatalogEntry.Single>().map { it.demo.id },
        )
    }
```

Add `import dev.dimvlachos.lab.resources.demo_fog`.

Append to `AppNavigationTest`:

```kotlin
    private val folder = Catalog.entries.filterIsInstance<CatalogEntry.Group>().single()

    @Test
    fun backFromAVersionReturnsToItsFolderThenHome() {
        val navigation = AppNavigation(initialDemo = null, record = false)
        navigation.openGroup(folder)
        assertTrue(navigation.canGoBack)
        navigation.openDemo(folder.demos.first())
        navigation.back()
        assertNull(navigation.demo)
        assertEquals(folder, navigation.group)
        navigation.back()
        assertNull(navigation.group)
        assertFalse(navigation.canGoBack)
    }

    @Test
    fun aVersionRecordedByIdOpensWithoutItsFolder() {
        val bathroom = Catalog.find("fog.mirror.bathroom")!!
        val navigation = AppNavigation(initialDemo = bathroom, record = true)
        assertNull(navigation.group)
        assertFalse(navigation.canGoBack)
    }
```

Add `import dev.dimvlachos.lab.catalog.CatalogEntry`.

Append to `AppSmokeTest`:

```kotlin
    @Test
    fun theFoggedMirrorFolderOpensItsVersionsAndBackStepsOut() = runComposeUiTest {
        setContent { App(initialDemoId = null, record = false, label = false) }
        onNodeWithText("Fogged mirror").performClick()
        onNodeWithText("Bathroom").assertExists()
        onNodeWithText("Your reflection").assertExists()
        onNodeWithText("compose-lab").assertDoesNotExist()

        onNodeWithText("Bathroom").performClick()
        onNodeWithText("Your reflection").assertDoesNotExist()
        onAllNodesWithText("Back").onFirst().performClick()
        onNodeWithText("Your reflection").assertExists()

        onAllNodesWithText("Back").onFirst().performClick()
        onNodeWithText("compose-lab").assertExists()
    }

    @Test
    fun systemBackFromTheFolderReturnsHome() = runComposeUiTest {
        val input = DirectNavigationEventInput()
        setContent {
            val owner = rememberNavigationEventDispatcherOwner(parent = null)
            DisposableEffect(owner) {
                owner.navigationEventDispatcher.addInput(input)
                onDispose { owner.navigationEventDispatcher.removeInput(input) }
            }
            CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides owner) {
                App(initialDemoId = null, record = false, label = false)
            }
        }
        onNodeWithText("Fogged mirror").performClick()
        runOnUiThread { input.backCompleted() }
        waitForIdle()
        onNodeWithText("compose-lab").assertExists()
    }
```

In `AppSmokeTest.theCatalogOpensEachDemoDirectly`, change `onNodeWithText("Bathroom")` (from Task 1)
back to `onNodeWithText("Fogged mirror")`: the home screen shows the folder, not its versions.

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew :composeApp:iosSimulatorArm64Test --tests '*CatalogTest*' --tests '*AppNavigationTest*' --tests '*AppSmokeTest*'`
Expected: compilation FAILS with unresolved `CatalogEntry`, `entries`, `openGroup` and `group`.

- [ ] **Step 3: Implement**

Create `catalog/CatalogEntry.kt`:

```kotlin
package dev.dimvlachos.lab.catalog

import dev.dimvlachos.lab.core.demo.Demo
import org.jetbrains.compose.resources.StringResource

/** A card on the home screen: one demo, or a folder of versions of one. */
sealed interface CatalogEntry {
    class Single(val demo: Demo) : CatalogEntry

    class Group(val id: String, val title: StringResource, val demos: List<Demo>) : CatalogEntry
}
```

Replace `Catalog.kt`'s body:

```kotlin
object Catalog {
    // The finished demo of each component, each one a clip; the fogged mirror in two versions.
    val entries: List<CatalogEntry> =
        (NavBarDemos.all + GalleryDemos.all).map { CatalogEntry.Single(it) } +
            CatalogEntry.Group("fog.mirror", Res.string.demo_fog, FogDemos.all)

    /** Every demo, in or out of a folder: what an id finds, and what a recording plays. */
    val demos: List<Demo> =
        entries.flatMap {
            when (it) {
                is CatalogEntry.Single -> listOf(it.demo)
                is CatalogEntry.Group -> it.demos
            }
        }

    fun find(id: String?): Demo? = demos.firstOrNull { it.id == id }
}
```

Add the imports `dev.dimvlachos.lab.resources.Res` and `dev.dimvlachos.lab.resources.demo_fog`.

Replace `AppNavigation`:

```kotlin
/**
 * Where the app is: home, a folder of versions, or a demo. The on-screen Back button and the
 * system back gesture both step back through [back]: a demo to its folder, if it came from one,
 * then home. A demo opened directly by id goes back home too, except while it is being recorded:
 * the recorder owns that screen.
 */
internal class AppNavigation(initialDemo: Demo?, record: Boolean) {
    var demo by mutableStateOf(initialDemo)
        private set

    var group by mutableStateOf<CatalogEntry.Group?>(null)
        private set

    private val recordedDemo = if (record) initialDemo else null

    /** False on the home screen, where back leaves the app, and on the recorded demo. */
    val canGoBack: Boolean
        get() = demo.let { if (it != null) it !== recordedDemo else group != null }

    fun openGroup(group: CatalogEntry.Group) {
        this.group = group
    }

    fun openDemo(demo: Demo) {
        this.demo = demo
    }

    fun back() {
        when {
            !canGoBack -> Unit
            demo != null -> demo = null
            else -> group = null
        }
    }
}
```

Add `import dev.dimvlachos.lab.catalog.CatalogEntry`.

Replace `CatalogScreen`, reusing the `DemoScreen` Back style (`TextButton` with `Res.string.action_back`):

```kotlin
/**
 * A list of cards, each opening a demo or a folder of versions: the home screen, and a folder's
 * own screen with its title and a way [onBack].
 */
@Composable
internal fun CatalogScreen(
    title: String,
    entries: List<CatalogEntry>,
    onOpenDemo: (Demo) -> Unit,
    onOpenGroup: (CatalogEntry.Group) -> Unit,
    onBack: (() -> Unit)? = null,
) {
    LazyColumn(
        modifier =
            Modifier.fillMaxSize()
                .background(LabTheme.colors.background)
                .windowInsetsPadding(WindowInsets.safeDrawing),
        contentPadding = PaddingValues(LabTheme.spacing.mediumLarge),
        verticalArrangement = Arrangement.spacedBy(LabTheme.spacing.smallMedium),
    ) {
        if (onBack != null) {
            item {
                TextButton(
                    onClick = onBack,
                    colors = ButtonDefaults.textButtonColors(contentColor = LabTheme.colors.accent),
                ) {
                    Text(stringResource(Res.string.action_back))
                }
            }
        }
        item {
            Text(title, color = LabTheme.colors.textPrimary, style = LabTheme.typography.title)
        }
        items(entries, key = { it.key }) { entry ->
            when (entry) {
                is CatalogEntry.Single ->
                    CatalogCard(
                        title = stringResource(entry.demo.title),
                        subtitle = entry.demo.id,
                        onClick = { onOpenDemo(entry.demo) },
                    )
                is CatalogEntry.Group ->
                    CatalogCard(
                        title = stringResource(entry.title),
                        subtitle = entry.id,
                        onClick = { onOpenGroup(entry) },
                    )
            }
        }
    }
}

private val CatalogEntry.key: String
    get() =
        when (this) {
            is CatalogEntry.Single -> demo.id
            is CatalogEntry.Group -> id
        }

@Preview
@Composable
private fun CatalogScreenPreview() {
    LabTheme {
        CatalogScreen(
            title = "compose-lab",
            entries = Catalog.entries,
            onOpenDemo = {},
            onOpenGroup = {},
        )
    }
}
```

The imports to add are:
- `androidx.compose.material3.ButtonDefaults`
- `androidx.compose.material3.TextButton`
- `dev.dimvlachos.lab.catalog.CatalogEntry`
- `dev.dimvlachos.lab.resources.action_back`

The `catalog_title` import moves to `App.kt`. Before using the `DemoScreen` Back style, check its
colour for the Back `TextButton` and use the same one.

In `App.kt`, replace the `if (demo != null) … else CatalogScreen(...)` block:

```kotlin
        val demo = navigation.demo
        val group = navigation.group
        when {
            demo != null ->
                DemoScreen(
                    demo = demo,
                    record = record && demo === initialDemo,
                    label = if (label) stringResource(platformLabel) else null,
                    onBack = if (navigation.canGoBack) navigation::back else null,
                )
            group != null ->
                CatalogScreen(
                    title = stringResource(group.title),
                    entries = group.demos.map { CatalogEntry.Single(it) },
                    onOpenDemo = navigation::openDemo,
                    onOpenGroup = navigation::openGroup,
                    onBack = navigation::back,
                )
            else ->
                CatalogScreen(
                    title = stringResource(Res.string.catalog_title),
                    entries = Catalog.entries,
                    onOpenDemo = navigation::openDemo,
                    onOpenGroup = navigation::openGroup,
                )
        }
```

Imports to add:
- `dev.dimvlachos.lab.catalog.CatalogEntry`
- `dev.dimvlachos.lab.resources.Res`
- `dev.dimvlachos.lab.resources.catalog_title`

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew spotlessApply :composeApp:iosSimulatorArm64Test :androidApp:testDebugUnitTest --rerun :androidApp:assembleDebug`
Expected: BUILD SUCCESSFUL, every test passes.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: the fogged mirror is a folder card holding its two versions"
```

---

## Verification

- **Tests:** `./gradlew :composeApp:iosSimulatorArm64Test`, all green.
- **Konsist:** `./gradlew :androidApp:testDebugUnitTest --rerun`.
- **Build:** `./gradlew :androidApp:assembleDebug`.
- **On the phone** (`installDebug`):
  - Home shows the Fogged mirror card; it opens Bathroom and Your reflection.
  - Bathroom asks only for the mic and shows the bathroom photo.
  - Your reflection shows the camera.
  - Back steps from a version to the folder, then home.
- **Recording:** `scripts/record.py android fog.mirror.bathroom`, only when the user says so.
