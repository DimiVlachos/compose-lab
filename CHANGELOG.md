# Changelog

All notable changes to this project are documented here. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project uses [Semantic Versioning](https://semver.org).

## [Unreleased]

### Added

- **Magnet filter** (`magnet`): drag tag magnets over a table of photos to filter them; matches are pulled by how well they match, strong ones stick, and two magnets combine as AND / OR.

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
