# Changelog

All notable changes to this project are documented here. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project uses [Semantic Versioning](https://semver.org).

## [Unreleased]

### Added

- **Paper bag cart** (`paperbag`): a cart icon that is a small kraft paper bag. Products hop into it on an arc. It squashes and rocks as each lands and bulges as it fills, long things poke out of the top, and its count is rewritten in grease pencil. Tap it and it tips open over a list of what is in it, where − lifts a thing back out and Check out folds its top over and places the order. Its demo is a grocery aisle, with haptics and synthesized paper sounds.
- **Star stickers** (`stickers`): a star rating on a teacher's sheet of embossed gold foil stickers. Drag across and each star peels off with a curl, flies up and is pressed into its slot with a glint and a bounce; drag back and they peel off onto the sheet again; a tap jumps to a score. The label names the stars that have landed, the peeled flap is the sticker's paper back, shaded as it bends, and synthesized peel, press and settle sounds and a tick per star come with it. One slider for screen readers and keys. Its demo rates a taco truck order.
- **Mailbox badge** (`mailbox`): an inbox button that is a tin rural mailbox. Mail slips in through its door and the red flag swings up with the unread count painted on it, and comes down once everything is read. Tap it, or pull its door down, to open the inbox. Its demo is a neighborhood app for Elm Street.
- **Padlock password** (`padlock`): a password's strength meter and show/hide switch as a brass padlock. A weak password leaves the shackle swung open and rattling; as it gets stronger the shackle lowers, turns to steel and drops home with a clunk and a glint at strong; tapping the lock swings the keyhole cover aside to show the password. Haptics and synthesized sounds; the lock is a switch for screen readers and the level a live region.
- **Conch voice note** (`conch`): hold-to-record voice messages where the composer's mic button is a pink-lipped queen conch. Hold it and it lifts into a warm lamp pool, its mouth glowing and rippling with a simulated voice while rings spread from it; slide left and it cools grey-blue, and letting go cancels with a puff of sand; let go and it flies into the chat as a voice note bubble whose shell hums as it plays. Haptics and synthesized sounds; the conch is a button with Send and Cancel actions, and recording, sent and canceled are announced.
- **Typewriter composer** (`typewriter`): a chat composer that is a 1950s portable typewriter, seen a little from above, with a type basket, ribbon spools, a rubber platen and a bell you can see working, typed on its own tiered, chrome-ringed keys (QWERTY, Shift, Delete, New line, a page of figures and stops) in place of the phone's keyboard, or on a hardware keyboard. Each letter strikes the sheet, the bell rings five letters before the margin and the carriage returns by itself, or by pushing the chrome return lever with a finger, deleted letters are lifted off with correction tape, and the sheet is sent by pulling it out of the roller (or with Ctrl/⌘+Return, or a screen reader's Send action), flying into the thread as a paper slip. Haptics on every strike for any caller; its synthesized sounds are the lab's internal `SoundPlayer` hook. Every key is a named button for a screen reader, and what is typed survives the screen being made again.
- **Smoke delete** (`smoke`): swipe a list row away and it burns from its edge, right under the finger, into smoke that curls up over the list while the list closes under it; draw the finger back and the smoke flows back down into the row as you go; let go before a third has burnt and the smoke is drawn back in, and Undo breathes it back. A row can go the other way in mist. Its demo is a chat list with Clear read, an archive to restore from, and an Undo snackbar.
- **Sound** (`core/platform/SoundPlayer`): demos can play short clips, synthesized by `scripts/synth-sounds.py` (no recordings, so no licences), through SoundPool on Android and AVAudioEngine on iOS, where a clip played faster or slower also moves in pitch, as on Android. Nothing plays while the phone is on silent, and a loop starts once its clip has loaded and keeps quiet while the phone is on silent.
- **Pop-up book** (`popup`): a pop-up book whose paper scenery stands up in perspective as a spread opens, with soft shadows and pull tabs that move its pieces; its demo is a Cyclades travel onboarding. The page-turn book's projection moved into a shared `perspective/Homography`.
- **Fishing refresh** (`fishing`): pull-to-refresh where refreshing is fishing: the pull bends a rod, release casts a bobber into a band of water where it floats while the refresh loads, and the outcome plays out on the water, a catch hauled up from the deep on the line into the list, an empty hook, or a snapped line. Built on Material's `pullToRefresh`; the caller hoists a sealed `FishingStatus`.
- **App shell**: a shadowed top bar on every screen; search from home's bar that also finds the versions inside a folder; tag chips to filter the catalog; an adaptive grid of cards, each with an icon and a one-line description.
- **Light theme**: a light palette beside the dark one, and a System/Light/Dark button on home that is remembered across launches. The system bars and the window follow the theme. Each demo's surroundings follow it too, while its art stays the same in both. Recordings always use the dark theme.
- **Landscape**: every demo works on a phone on its side. Layouts branch on the stage's shape (`isWideStage`), and demos keep their controls clear of the sides and foot with `demoInsets`, so recorded clips keep their portrait layout.
- **Magnet filter** (`magnet`): drag horseshoe tag magnets over a table of photos to filter them; a magnet finds every match on the table, pulled in by how well it matches, strong ones stick, two snapped together search for both, and a magnet's photos fan out into a grid where each one opens.

### Changed

- **Recording**: a demo can ask for a tall stage (`tall = true`), recorded as a 9:16 clip with `scripts/record.py --tall`. The fishing refresh's feed uses it, so its clip shows more of the list.
- **Pull cord** (`pullcord`): its Verlet rope moved into `physics/` as `VerletRope`, shared with the fishing refresh's line.

### Fixed

- **Demo screen**: the stage no longer pads the top that the bar already covers, which left a doubled gap over the pop-up book.
- **Pull cord** (`pullcord`): a scripted pull that started as the last one's fingertip faded out was cancelled by it.

## [0.1.0] - 2026-10-06

The first release: five components, the apps that demo them, and Moodboard.

### Added

- **Nav bar** (`navbar`): a bottom bar built from switchable layers, with a morphing indicator, animated icons, collapse on scroll, a moving cutout and a jelly action button.
- **Profile gallery** (`gallery`, `imagemorph`, `profile`): a profile screen where every open is a shared-element morph, serialised by a morph gate, with a collapsing header and morphing search.
- **Fogged mirror** (`fog`): a steamed-up mirror you wipe with a finger, with running drops and mist that returns; on Android, a camera version that cuts you out of your room with ML Kit selfie segmentation, on the phone.
- **Page-turn book** (`pageturn`): pages that curl in 3D under the finger, drawn in plain `DrawScope`.
- **Paper plane** (`paperplane`): a chat that sends each message as a folded paper dart.
- **Moodboard** (`moodboard/`): a Kotlin Multiplatform app that shares data, logic and ViewModels, with a SwiftUI UI on iOS 26 and a Material 3 Expressive UI on Android.
- Scripted demos for every component, and tools to record them as clips and GIFs.

[Unreleased]: https://github.com/DimiVlachos/compose-lab/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/DimiVlachos/compose-lab/releases/tag/v0.1.0
