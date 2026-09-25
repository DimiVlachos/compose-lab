#!/usr/bin/env bash
# Build the iOS app for the booted simulator and install it there.
set -euo pipefail
cd "$(dirname "$0")/../iosApp"
xcodegen generate --quiet
udid=$(xcrun simctl list devices booted -j | python3 -c 'import json,sys; d=json.load(sys.stdin)["devices"]; print(next(x["udid"] for v in d.values() for x in v if x["state"] == "Booted"))')
xcodebuild -project iosApp.xcodeproj -scheme iosApp -configuration Debug \
  -destination "platform=iOS Simulator,id=$udid" -derivedDataPath ../build/ios \
  CODE_SIGNING_ALLOWED=NO -quiet build
xcrun simctl install booted ../build/ios/Build/Products/Debug-iphonesimulator/iosApp.app
echo "Installed on $udid"
