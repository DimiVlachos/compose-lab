#!/usr/bin/env bash
# Make the README GIF for one demo from its recorded clip.
set -euo pipefail
demo="${1:?usage: scripts/gif.sh <demoId> [android|ios]}"
platform="${2:-android}"
cd "$(dirname "$0")/.."
mkdir -p docs/media
ffmpeg -loglevel error -y -i "out/$demo-$platform.mp4" \
  -vf "fps=24,scale=480:-1:flags=lanczos,split[s0][s1];[s0]palettegen=max_colors=96[p];[s1][p]paletteuse=dither=bayer:bayer_scale=4" \
  "docs/media/$demo.gif"
ls -lh "docs/media/$demo.gif"
