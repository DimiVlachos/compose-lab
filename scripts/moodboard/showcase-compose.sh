#!/usr/bin/env bash
# Puts the iOS and Android showcase takes side by side over a background (Android Studio with the
# project open), each trimmed to start half a second before the script's first step.
# Usage: showcase-compose.sh <ios.mov> <iosStart> <android.mp4> <androidStart> <background.png> <out.mp4>
#   <*Start>: the second, in that take, at which its script began (the steps' zero).
set -euo pipefail
ios="$1"; ios0="$2"; android="$3"; android0="$4"; bg="$5"; out="$6"
here="$(cd "$(dirname "$0")" && pwd)"
assets="$(mktemp -d)"
trap 'rm -rf "$assets"' EXIT

# Canvas 1920x1080; two phones 940 px tall on the right, labels underneath, so the left of the frame
# shows the IDE: the project tree and the shared code both apps run.
H=940; IW=432; AW=421; GAP=90; TOP=46; RIGHT=90
IX=$(( 1920 - RIGHT - AW - GAP - IW )); AX=$(( IX + IW + GAP ))
swift "$here/showcase-assets.swift" "$assets" $IW $H 58 "iOS" ios >/dev/null
swift "$here/showcase-assets.swift" "$assets" $AW $H 46 "Android" android >/dev/null
# The simulator only writes a frame when the screen changes: make the take constant-rate before
# seeking into it, or the clip starts on a missing (black) frame and runs early by the gap. One
# continuous take with one offset: each step lands within a second of Android's, and nothing can play
# twice the way cutting the take per step did.
ios_cfr="$assets/ios-cfr.mp4"
ffmpeg -loglevel error -y -i "$ios" -vf fps=60 -c:v libx264 -crf 12 "$ios_cfr"
# Ends half a second before the script does: then the iOS test finishes and Xcode closes the app.
DUR=40; PRE=0.5

ffmpeg -loglevel error -y \
  -loop 1 -i "$bg" \
  -ss "$(echo "$ios0 + $PRE" | bc)" -t $DUR -i "$ios_cfr" \
  -ss "$(echo "$android0 + $PRE" | bc)" -t $DUR -i "$android" \
  -i "$assets/ios-mask.png" -i "$assets/android-mask.png" \
  -i "$assets/ios-shadow.png" -i "$assets/android-shadow.png" \
  -i "$assets/ios-label.png" -i "$assets/android-label.png" \
  -filter_complex "
    [0:v]scale=1920:1080:force_original_aspect_ratio=increase,crop=1920:1080:0:0,eq=brightness=-0.06[bg];
    [1:v]setpts=PTS-STARTPTS,fps=30,scale=$IW:$H,format=rgba[i0];[i0][3:v]alphamerge[i];
    [2:v]setpts=PTS-STARTPTS,fps=30,scale=$AW:$H,format=rgba[a0];[a0][4:v]alphamerge[a];
    [bg][5:v]overlay=$((IX-60)):$((TOP-60))[s1];[s1][6:v]overlay=$((AX-60)):$((TOP-60))[s2];
    [s2][i]overlay=$IX:$TOP:shortest=1[p1];[p1][a]overlay=$AX:$TOP:shortest=1[p2];
    [p2][7:v]overlay=$IX+($IW-w)/2:$((TOP+H+18))[l1];[l1][8:v]overlay=$AX+($AW-w)/2:$((TOP+H+18)),format=yuv420p[v]" \
  -map "[v]" -t $DUR -r 30 -c:v libx264 -preset slow -crf 20 -movflags +faststart -an "$out"
ls -lh "$out"
