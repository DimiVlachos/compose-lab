#!/usr/bin/env bash
# Make the README GIF for one demo from its recorded clip. A photo-heavy clip can go smaller:
#   GIF_FPS=15 GIF_WIDTH=360 GIF_COLORS=128 scripts/gif.sh morph.app
set -euo pipefail
demo="${1:?usage: scripts/gif.sh <demoId> [android|ios]}"
platform="${2:-android}"
cd "$(dirname "$0")/.."
mkdir -p docs/media
ffmpeg -loglevel error -y -i "out/$demo-$platform.mp4" \
  -vf "fps=${GIF_FPS:-24},scale=${GIF_WIDTH:-480}:-1:flags=lanczos,split[s0][s1];[s0]palettegen=max_colors=${GIF_COLORS:-96}:stats_mode=diff[p];[s1][p]paletteuse=dither=bayer:bayer_scale=4:diff_mode=rectangle" \
  "docs/media/$demo.gif"
ls -lh "docs/media/$demo.gif"
