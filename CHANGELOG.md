# Changelog

All notable changes to this project are documented here. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project uses [Semantic Versioning](https://semver.org).

## [Unreleased]

### Added

- **Fishing refresh** (`fishing`): pull-to-refresh where refreshing is fishing: the pull bends a rod, release casts a bobber into a band of water where it floats while the refresh loads, and the outcome plays out on the water, a catch hauled up from the deep on the line into the list, an empty hook, or a snapped line. Built on Material's `pullToRefresh`; the caller hoists a sealed `FishingStatus`.
- **Magnet filter** (`magnet`): drag horseshoe tag magnets over a table of photos to filter them; a magnet finds every match on the table, pulled in by how well it matches, strong ones stick, two snapped together search for both, and a magnet's photos fan out into a grid where each one opens.

### Changed

- **Pull cord** (`pullcord`): its Verlet rope moved into `physics/` as `VerletRope`, shared with the fishing refresh's line.

### Fixed

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
