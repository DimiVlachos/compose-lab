#!/usr/bin/env bash
# Put the labelled Android and iOS clips of one demo next to each other on a 4:5 canvas.
# Record them first: scripts/record.py android <id> --label && scripts/record.py ios <id> --label
set -euo pipefail
demo="${1:?usage: scripts/side-by-side.sh <demoId>}"
cd "$(dirname "$0")/.."
dest="out/$demo-both.mp4"
ffmpeg -loglevel error -y -i "out/$demo-android-labeled.mp4" -i "out/$demo-ios-labeled.mp4" -filter_complex \
  "[0:v]scale=540:675,setsar=1[a];[1:v]scale=540:675,setsar=1[i];[a][i]hstack=inputs=2:shortest=1,pad=1080:1350:0:(oh-ih)/2:color=0x0B0D12,fps=60,format=yuv420p[v]" \
  -map "[v]" -c:v libx264 -preset slow -crf 20 -movflags +faststart -an "$dest"
ls -lh "$dest"
