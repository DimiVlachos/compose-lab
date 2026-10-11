# Contributing

Thanks for taking a look. compose-lab is a showcase, so the bar for a change is that it makes a component better to use, to read or to watch.

## Before you start

For anything bigger than a fix, open an issue first and say what you'd like to change. Each component is a small, deliberate design, and it's easier to agree on the idea before the code.

## Set up

See [Run](README.md#run) for the requirements. Then:

```bash
./gradlew :androidApp:installDebug   # the lab app on a connected Android device
scripts/install-ios.sh               # the lab app on the booted iOS simulator
```

## Checks

CI runs these on every pull request. Run them locally first:

```bash
./gradlew spotlessCheck                          # formatting (ktfmt, kotlinlang style); fix with spotlessApply
./gradlew :composeApp:iosSimulatorArm64Test      # the components' tests
./gradlew :androidApp:testDebugUnitTest          # architecture rules (Konsist)
./gradlew :androidApp:lintDebug                  # Android lint
./gradlew :moodboard:shared:iosSimulatorArm64Test :moodboard:androidApp:testDebugUnitTest
./gradlew :moodboard:androidApp:lintDebug
```

CI also builds both debug APKs, links Moodboard's iOS framework (where a SKIE problem shows up), and builds the lab app in Xcode; [ci.yml](.github/workflows/ci.yml) has every step. The iOS tests and builds need macOS with Xcode. A separate hygiene check (`scripts/check-hygiene.sh`) keeps private links, local notes and oversized GIFs out of the repo.

## Code

- Keep a component's animated values out of composition: read them in layout or draw, so a component recomposes only when its inputs change. The existing components show how.
- Put platform code behind `expect`/`actual` and keep it small.
- New behaviour comes with a test in `commonTest`.
- A demo's stage sits under the demo screen's top bar, which owns the top of the screen (the status bar and the cutout). The demo keeps its controls clear of the sides and the foot with `demoInsets(state)`, which is zero while recording, when the clip's frame touches no edge.
- A landscape layout branches on the shape of the whole stage, `isWideStage(maxWidth, maxHeight)`, never on a height threshold. The 4:5 and 9:16 record frames are never wide, so clips keep their portrait layout.
- Every colour is a `LabTheme` token in `core/presentation/ui/Color.kt`, either themed (with a light value in `AppColors.Light`) or art (the same in both themes). The `AppColors` tests fail until a new token is sorted into one or the other, and hold the themed ones to their contrast.
- A new `Demo` needs a description, an icon (a vector drawable, `ic_demo_*`) and at least one tag, for its catalog card, the search and the tag filter.
- A change to how a component looks should update its clip. [Record a clip](README.md#record-a-clip) has the scripts.

## Commits and pull requests

Commit messages follow [Conventional Commits](https://www.conventionalcommits.org) with the component as the scope:

```
feat(navbar): let the action button take a custom shape
fix(fog): stop a drop from running past the glass
docs(readme): add the paper plane's envelope
```

Keep a pull request to one change, and describe what a reviewer should look at or try.

## Code of conduct

This project follows the [Contributor Covenant](CODE_OF_CONDUCT.md). By taking part, you agree to uphold it.
