# Moodboard — native-feel KMP showcase

## Context
compose-lab showcases hand-built Compose effects, but everything is drawn by Compose on both platforms.
Goal: a second app in this repo proving a KMP app can feel *native on each platform* — real iOS 26 Liquid Glass,
UIKit/SwiftUI context menus, alerts, action sheets, sheets and search on iOS; Material 3 Expressive with dynamic
color on Android — while sharing state, logic and the screens where sharing doesn't hurt nativeness.
On approval: save this spec verbatim to `docs/superpowers/specs/2026-10-03-moodboard-native-showcase.md` and commit;
implementation runs in a fresh session against it.

## Out of scope (v1)
Persistence (state resets on relaunch) · device photo library · iPad/landscape layouts · desktop/web targets ·
localization · scripted demo loops / `record.py` support · iOS build in CI.

## Architecture decisions
| Decision | Chosen | Rejected | Why |
|---|---|---|---|
| iOS chrome | **SwiftUI shell** (TabView, NavigationStack, toolbars, sheets, alerts) | Compose root + UIKit islands; fully native UI; Compose-imitated glass | Real Liquid Glass only exists in UIKit/SwiftUI |
| iOS lists | **SwiftUI** grids/lists (gallery, board detail, boards list, search results) | Compose grids + bridged menus | Large-title collapse, tab-bar minimize, scroll-edge glass and `.contextMenu` previews need native scroll views |
| Shared Compose screens | Photo detail content, board editor, filter sheet body | — | No native scroll-chrome dependency; shows Compose embedded in SwiftUI |
| Navigation | **Each platform owns its back stack** (SwiftUI NavigationStack / Android Navigation 3) | Shared Kotlin navigator | Keeps swipe-back and predictive back fully native |
| Screen models | **androidx ViewModel (KMP)**, MVI State + Action, state-only (no event channel) | Plain store classes | Familiar lifecycle; `viewModelScope` |
| Swift ↔ Kotlin | **SKIE** (Flows → AsyncSequence, suspend → async, sealed → enums) | Hand-rolled watch(); KMP-NativeCoroutines | User choice |
| Android look | **M3 Expressive + dynamic color**, edge-to-edge | Plain M3 | minSdk 31 supports dynamic color |
| Data | Bundled 20 images, **in-memory** repository | Persisted; Photos/MediaStore | Focus on UI |
| iOS minimum | **iOS 26.0** | 17 + material fallback | No fallback branches |
| DI | None — `MoodboardGraph` object singleton holds the repository | Koin | Repo has no DI; one dependency |

## Module placement & new files
```
settings.gradle.kts                       + include(":moodboard:shared", ":moodboard:androidApp")
gradle/libs.versions.toml                 + skie plugin, lifecycle-viewmodel-compose, navigation3, lifecycle-viewmodel-navigation3
moodboard/shared/                         KMP lib (android + iosArm64 + iosSimulatorArm64), namespace dev.dimvlachos.moodboard.shared
  build.gradle.kts                        same shape as composeApp/build.gradle.kts; framework baseName "MoodboardShared", isStatic; apply co.touchlab.skie
  src/commonMain/composeResources/files/photos/   20 jpgs copied from composeApp drawable: photo_{naxos,mykonos,…}.jpg (12) + book_spread_{1..8}.jpg
  src/commonMain/kotlin/dev/dimvlachos/moodboard/
    domain/  Photo, Board, PhotoFilter, SortOrder, MoodboardRepository (interface)
    data/    InMemoryMoodboardRepository, SeedData, PhotoBytes (Res.readBytes + LRU of decoded ImageBitmap)
    MoodboardGraph.kt                     object { val repository; val photoBytes }
    gallery/    GalleryViewModel, GalleryState, GalleryAction
    detail/     PhotoDetailViewModel, PhotoDetailState/Action, PhotoDetailContent.kt (shared Compose)
    boards/     BoardsViewModel, BoardDetailViewModel, BoardEditorViewModel + State/Action, BoardEditorContent.kt (shared Compose)
    search/     SearchViewModel, SearchState/Action
    filter/     FilterSheetContent.kt (shared Compose: favorites-only switch, tag chips, sort segmented buttons)
    ui/theme/   MoodboardTheme (expect fun platformColorScheme(dark): ColorScheme)
  src/androidMain/…/ui/theme/PlatformColorScheme.android.kt   dynamic{Light,Dark}ColorScheme
  src/iosMain/…/ui/theme/PlatformColorScheme.ios.kt           fixed scheme (Compose bits on iOS stay neutral; system tint via SwiftUI)
  src/iosMain/…/ios/
    ViewControllers.kt     fun PhotoDetailViewController(vm): UIViewController, FilterSheetViewController(vm: GalleryViewModel),
                           BoardEditorViewController(vm, onDone: () -> Unit) — each a ComposeUIViewController
    SwiftViewModelStore.kt class with ViewModelStore: gallery(), photoDetail(id), boards(), boardDetail(id), boardEditor(id), search(), clear()
    ImageData.kt           suspend fun imageData(path: String): NSData
  src/commonTest/          ViewModel + repository tests (kotlin-test, coroutines-test)
moodboard/androidApp/                     com.android.application, applicationId/namespace dev.dimvlachos.moodboard
  src/main/AndroidManifest.xml           MainActivity, FileProvider (authority dev.dimvlachos.moodboard.share, cache-path "shared/")
  src/main/kotlin/dev/dimvlachos/moodboard/android/
    MainActivity.kt (enableEdgeToEdge), MoodboardApp.kt (Scaffold + NavigationBar + NavDisplay)
    nav/Routes.kt (Gallery, Boards, Search, PhotoDetail(id), BoardDetail(id), BoardEditor(id) — @Serializable NavKey)
    gallery/GalleryScreen.kt, boards/BoardsScreen.kt, boards/BoardDetailScreen.kt, search/SearchScreen.kt,
    detail/PhotoDetailScreen.kt (Scaffold wrapping shared PhotoDetailContent), share/SharePhoto.kt
  src/test/…/architecture/  Konsist tests mirroring androidApp/src/test/.../CodingConventionsKonsistTest.kt, scoped to dev.dimvlachos.moodboard
moodboard/iosApp/
  project.yml              copy of iosApp/project.yml: name Moodboard, deploymentTarget "26.0", bundle dev.dimvlachos.moodboard,
                           CFBundleDisplayName Moodboard, portrait only, FRAMEWORK_SEARCH_PATHS ../shared/build/xcode-frameworks/…,
                           OTHER_LDFLAGS -framework MoodboardShared, preBuild ./gradlew :moodboard:shared:embedAndSignAppleFrameworkForXcode
  Moodboard/MoodboardApp.swift, RootTabView.swift
  Moodboard/Bridge/  ScreenModel.swift (@Observable base: owns SwiftViewModelStore, collects `vm.state` in .task, clear() in deinit),
                     ComposeScreen.swift (UIViewControllerRepresentable wrapping the Kotlin factories), ImageCache.swift (actor + NSCache, preparingThumbnail)
  Moodboard/Gallery/ GalleryView.swift, PhotoGrid.swift, PhotoContextMenu.swift, PhotoDetailView.swift
  Moodboard/Boards/  BoardsView.swift, BoardDetailView.swift
  Moodboard/Search/  SearchView.swift
.github/workflows/ci.yml  + :moodboard:shared:iosSimulatorArm64Test, :moodboard:androidApp:testDebugUnitTest, :moodboard:androidApp:assembleDebug
README.md                 + short "Moodboard" section
```

## Screens — native component per platform
| Feature | iOS (SwiftUI, iOS 26) | Android (Compose M3 Expressive) |
|---|---|---|
| Tabs | `TabView` with `Tab` API: Gallery, Boards, `Tab(role: .search)`; `.tabBarMinimizeBehavior(.onScrollDown)` — glass tab bar | `NavigationBar` (3 items), Nav3 `NavDisplay`, predictive back |
| Gallery | `NavigationStack`, large title, `LazyVGrid` 3 cols; toolbar filter button (glass) | `LargeFlexibleTopAppBar` + exitUntilCollapsed, `LazyVerticalGrid` |
| Context menu | `.contextMenu(menuItems:preview:)`: Share (ShareLink), Favorite/Unfavorite, Add to board ▸ (boards + "New board…"), Delete (destructive) | long-press → `DropdownMenu` anchored to cell + `HapticFeedbackType.LongPress`; "Add to board" opens a second menu |
| Delete photo | `.alert` "Delete photo?" Delete (destructive)/Cancel | `AlertDialog` |
| New board | `.alert` with `TextField` | `AlertDialog` with `OutlinedTextField` |
| Delete board (action sheet) | `.confirmationDialog`: "Delete board only" / "Delete board and its photos" (destructive) / Cancel | `ModalBottomSheet` with the same two actions |
| Filter | `.sheet` + `.presentationDetents([.medium, .large])` hosting shared `FilterSheetContent` | `ModalBottomSheet` hosting shared `FilterSheetContent` |
| Photo detail | push with `.navigationTransition(.zoom(sourceID:in:))`; hosted shared `PhotoDetailContent` under glass toolbar: favorite (`.sensoryFeedback(.selection)`), ShareLink, `Menu` (Add to board, Delete) | `Scaffold` + `TopAppBar` actions (favorite, share, overflow `DropdownMenu`) around shared content |
| Board detail | SwiftUI grid of members; context menu "Remove from board"; toolbar Edit → board editor sheet | `LazyVerticalGrid` + `DropdownMenu`; Edit → BoardEditor route |
| Board editor | `.sheet` hosting shared `BoardEditorContent` (name field + all-photos grid with checkmarks) + toolbar Done | same Compose content in a full-screen route |
| Boards list | `List` with cover thumbnail + count, swipe-to-delete (→ confirmationDialog), `.contextMenu` Rename/Delete, toolbar "+" | `LazyColumn` of `ListItem`s, `ExtendedFloatingActionButton` "New board", long-press menu |
| Search | `.searchable` in the search tab (bottom glass field), `.searchSuggestions` tag list, results grid | M3 `SearchBar` (expanded), `SuggestionChip` tags, results grid |
| Share | `ShareLink(item: Image(uiImage:), preview:)` | `Intent.ACTION_SEND` with FileProvider URI (bytes written to `cacheDir/shared/`) |

Shared Compose content draws edge to edge and pads scrolling content with `WindowInsets.safeDrawing`, so glass bars on iOS sit over the content.

## State / Action
```kotlin
data class Photo(val id: String, val title: String, val path: String, val tags: Set<String>, val isFavorite: Boolean)
data class Board(val id: String, val name: String, val photoIds: List<String>)
data class PhotoFilter(val favoritesOnly: Boolean = false, val tags: Set<String> = emptySet(), val sort: SortOrder = SortOrder.Default)
enum class SortOrder { Default, Title }

interface MoodboardRepository {
  val photos: StateFlow<List<Photo>>; val boards: StateFlow<List<Board>>
  fun toggleFavorite(id: String); fun deletePhoto(id: String)          // also removes from every board
  fun createBoard(name: String, initialPhotoId: String? = null): String
  fun renameBoard(id: String, name: String); fun deleteBoard(id: String, deletePhotos: Boolean)
  fun setMembership(boardId: String, photoId: String, member: Boolean)
}

data class GalleryState(val photos: List<Photo>, val boards: List<Board>, val filter: PhotoFilter, val allTags: List<String>)
sealed interface GalleryAction { ToggleFavorite(id) · AddToBoard(photoId, boardId) · CreateBoardWith(photoId, name) · Delete(id) · SetFilter(filter) }
data class PhotoDetailState(val photo: Photo?, val boards: List<Board>, val isDeleted: Boolean)   // iOS/Android pop when isDeleted
sealed interface PhotoDetailAction { ToggleFavorite · Delete · SetMembership(boardId, member) }
data class BoardsState(val boards: List<BoardSummary>)   // BoardSummary(id, name, count, coverPath?)
sealed interface BoardsAction { Create(name) · Rename(id, name) · Delete(id, deletePhotos) }
data class BoardDetailState(val board: Board?, val photos: List<Photo>, val isDeleted: Boolean)
sealed interface BoardDetailAction { Remove(photoId) }
data class BoardEditorState(val name: String, val photos: List<Photo>, val selected: Set<String>)
sealed interface BoardEditorAction { Rename(name) · Toggle(photoId) }   // edits apply live to the repository
data class SearchState(val query: String, val suggestions: List<String>, val results: List<Photo>)
sealed interface SearchAction { QueryChanged(q) · TagTapped(tag) }   // matches title or tag, case-insensitive
```
Each ViewModel exposes `val state: StateFlow<…>` (`stateIn(viewModelScope, Eagerly, …)`) and `fun onAction(action)`.
Seed: 12 island photos (title = island name, tags {island, greece, sea}), 8 "Little Nemo · Spread N" (tags {comic, little nemo});
boards "Islands" (all islands), "Little Nemo" (all spreads), "Blue" (santorini, mykonos, milos). Favorites: santorini, book_spread_1.
No SavedStateHandle (in-memory by design).

## Swift bridge
- SKIE gives `for await state in vm.state { … }` inside `.task` on the screen's `@Observable` model; Actions are built in Swift as `GalleryActionToggleFavorite(id:)`.
- Lifetime: each SwiftUI screen model owns one `SwiftViewModelStore`; deinit → `store.clear()` (cancels viewModelScope).
- Hosted Compose screens get the *same* ViewModel instance passed into the iosMain factory (Compose doesn't call `viewModel {}` on iOS).
- Images: Swift `ImageCache` actor → `ImageDataKt.imageData(path:)` (SKIE async) → `UIImage(data:)` → `preparingThumbnail(of:)` for grid cells.

## Error handling
In-memory, no error wrapper. Only failure is a missing image resource → neutral placeholder tile, logged via kermit.
Actions on a missing id are no-ops.

## Dependencies to add
- `co.touchlab.skie` plugin **0.10.15** (adds Kotlin 2.4.20 support; confirm it builds on the repo's 2.4.10 in step 1, otherwise bump to the newest SKIE that does).
- `org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose` (version.ref `lifecycle` = 2.9.6) — commonMain.
- `androidx.navigation3:navigation3-runtime`, `navigation3-ui`, `androidx.lifecycle:lifecycle-viewmodel-navigation3` — latest stable at implementation time; Android app only. Plus kotlinx-serialization plugin for `@Serializable` routes.
- Material 3 Expressive: use `compose.material3` from CMP 1.11.1 if it exposes `MaterialExpressiveTheme` / `MotionScheme.expressive()`; otherwise add the explicit `org.jetbrains.compose.material3:material3` version that does.

## Decisions deferred to Claude
- No DI library; `MoodboardGraph` singleton. Board editor edits apply live (no Save/Cancel). Swift language mode 5 (like the lab) to avoid Swift 6 strict-concurrency friction with SKIE types.
- Photos copied (not shared) from composeApp resources so the modules stay independent.

## Implementation order (fresh session, commit per task)
1. **Spike, biggest risk first:** scaffolding of both modules, SKIE builds, iOS NavigationStack pushing a hosted Compose `PhotoDetailContent` under a glass toolbar. Verify (a) glass samples the Compose Metal layer, (b) `WindowInsets.safeDrawing` in the hosted view reflects bar heights. If (a) fails, stop and report.
2. Domain + in-memory repository + seed + tests.
3. ViewModels + tests.
4. Android: theme, nav, all screens, share.
5. iOS: tab shell, gallery + context menu + alerts, detail, boards + confirmationDialog, editor sheet, filter sheet, search.
6. CI steps, Konsist, README.

## Verification
- `./gradlew spotlessCheck :moodboard:shared:iosSimulatorArm64Test :moodboard:androidApp:testDebugUnitTest :moodboard:androidApp:assembleDebug`
- Android: install on S23 Ultra over adb Wi-Fi; check dynamic color follows the wallpaper, predictive back on detail, every menu/dialog/sheet in the table.
- iOS: `cd moodboard/iosApp && xcodegen && xcodebuild -scheme Moodboard -destination 'platform=iOS Simulator,name=iPhone 17'`; walk the same table on iOS 26: glass tab bar minimizes on scroll, large title collapses, context menu preview lifts, zoom transition, confirmationDialog, detents, bottom search field.
