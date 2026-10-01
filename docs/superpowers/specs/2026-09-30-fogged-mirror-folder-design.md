# Fogged mirror as a folder of two versions

## Why

The fogged mirror now does two different things: the live front camera behind the fog, and a still
bathroom photo behind it. They suit different moments. The camera is the fun one on a phone. The
bathroom is the calm, repeatable one, with no camera prompt, and it is what the clip is recorded
from. The catalog should offer them as two versions inside one "Fogged mirror" folder.

## What the user decided

- **Layout:** one "Fogged mirror" folder card on the home screen. Tapping it opens a list of the two
  versions.
- **Names:** "Bathroom" (the still photo) and "Your reflection" (the live camera).
- **Permissions:**
  - Bathroom asks only for the microphone, for blowing, and never for the camera.
  - Your reflection keeps today's card, which asks for the camera, then the mic.
- **Clip:** recorded from Bathroom: `scripts/record.py android fog.mirror.bathroom`.

## Behaviour

- **Home:** the other demos stay as cards. The fog demo becomes one folder card, titled "Fogged
  mirror", with subtitle `fog.mirror`.
- **Folder screen:**
  - a Back button and the title "Fogged mirror"
  - one card per version: "Bathroom" (`fog.mirror.bathroom`) and "Your reflection"
    (`fog.mirror.camera`)
- **Back:**
  - from a version it returns to the folder, and from the folder to home (on-screen Back and the
    system gesture alike)
  - a demo opened directly by id goes back home, as today
  - the recorded demo still can't be left
- **Bathroom:**
  - today's fogged mirror with the still photo, never using the camera
  - its card explains only the mic, with no "to find yourself" and no camera line
  - drips, breath, wipes and the heart clip all stay
- **Your reflection:** today's fogged mirror, unchanged.

## Design

- **Catalog:**
  - `CatalogEntry` is either `Single(demo)` or `Group(id, title, demos)`
  - `Catalog.entries` is the home list; `Catalog.demos` stays the flat list of every demo, for
    `find` and recording
  - the fog group is built in `Catalog` from `FogDemos.all`
- **`AppNavigation`:** gains `group` state, plus `openGroup(group)`.
  - `back()` leaves the demo first, then the group
  - `canGoBack` is true on a demo (unless it's the recorded one) or on a group screen
- **`CatalogScreen`:** takes a title, its entries and an optional back action; the home screen and
  the folder screen are both this screen.
- **Code folders:**
  - `fogdemo/bathroom/BathroomMirror.kt` calls `FogDemo` with `cameraAccess = Unavailable` and
    `reflection = false`
  - `fogdemo/reflection/ReflectionMirror.kt` calls `FogDemo` as today
  - everything shared stays in `fogdemo/presentation`
- **`FogDemo`:** gains `reflection: Boolean = true`, passed to the card, which picks its opening
  words by it.
- **Strings:**
  - `demo_fog` stays "Fogged mirror", now the folder's title
  - new: `demo_fog_bathroom` "Bathroom", `demo_fog_reflection` "Your reflection", and
    `mirror_card_body_what_bathroom`

## Testing

- **`CatalogTest`:** the home entries are navbar, morph and one fog group, and the demo ids are
  `navbar.all`, `morph.app`, `fog.mirror.bathroom` and `fog.mirror.camera`.
- **`AppNavigationTest`:** group → demo → back → group → back → home; `canGoBack` on a group; a demo
  opened by id goes home.
- **`AppSmokeTest`:** tap "Fogged mirror" to see both versions; open Bathroom; Back returns to the
  folder, then home.
- **Card test:** the Bathroom card shows the mic line and the bathroom wording, with no camera line;
  Allow asks only for the mic.
- **`FogDemosTest`:** both versions keep the clip script. Existing tests that used `.single()` use
  the Bathroom demo.

## Out of scope

- nested folders
- folder cards for the other demos
- a different clip for Your reflection
