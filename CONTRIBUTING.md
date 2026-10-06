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
```

The iOS tests need macOS with Xcode.

## Code

- Keep a component's animated values out of composition: read them in layout or draw, so a component recomposes only when its inputs change. The existing components show how.
- Put platform code behind `expect`/`actual` and keep it small.
- New behaviour comes with a test in `commonTest`.
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
