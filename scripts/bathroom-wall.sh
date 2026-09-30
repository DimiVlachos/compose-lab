#!/usr/bin/env bash
# Build the wall the fogged mirror hangs on, bathroom_wall.jpg (1220 × 2639), from a photo whose
# top edge cuts through the mirror.
#
# Source: "Contemporary bathroom interior with mirror above washbasin at home" by Max Vakhtbovych,
# Pexels https://www.pexels.com/photo/7046159/ (Pexels License: free to use and modify).
#
# The mirror's frame is the same top and bottom, so its missing top is its bottom turned upside
# down: rounded corners, bracket bars and the outer rail. Every piece starts and ends on a grout
# line (the tiles repeat every ~121 px), so the rows of tiles run on unbroken. From the top down:
#   plain tiles    two rows: a grout line, and the plain tile face under the frame's shadow
#                  stretched tall, the taps painted out with tile from one tile's width (364 px) over
#   frame's top    the photo's rows 676–1037 flipped, the taps under the rail painted out
#   more glass     three rows of the mirror's straight sides
#   the photo      from row 70: the glass, the frame's bottom, the taps and the basin
#   counter face   the counter's front, shaded, so the picture is as tall as a phone
set -euo pipefail
cd "$(dirname "$0")/.."
src="${TMPDIR:-/tmp}/bathroom-wall-source.jpg"
[ -f "$src" ] || curl -sL -o "$src" \
  "https://images.pexels.com/photos/7046159/pexels-photo-7046159.jpeg?auto=compress&w=2400"

# The counter's front face: a lit lip, a shadow under it, then darkening grey with a little grain.
face="if(lt(Y,5),218,if(lt(Y,14),205-(Y-5)*6,150-40*Y/H))+2*(random(1)-0.5)"

ffmpeg -loglevel error -y -i "$src" -f lavfi -i "color=c=gray:s=1220x136,format=rgb24" \
  -filter_complex "
[1]geq=r='$face':g='$face-2':b='$face-1'[face];
[0]format=rgb24,crop=1220:1602:580:0,split=8[g1][g2][b1][b3][s1][b2][m][base];
[g1]crop=1220:3:0:1036[grout];
[g2]crop=310:3:186:1036[clean];
[grout][clean]overlay=550:0,split=2[ga][gb];
[b1]crop=1220:26:0:1045[face1];
[b3]crop=310:26:186:1045[face2];
[face1][face2]overlay=550:0,scale=1220:118:flags=bilinear,split=2[ba][bb];
[ga][ba]vstack,split=2[t1][t2];
[s1]crop=1220:324:0:676[upper];
[b2]crop=1220:26:0:985,scale=1220:36:flags=bilinear[lower];
[upper][lower][gb]vstack=inputs=3,vflip[top];
[m]crop=1220:122:0:70,split=3[m1][m2][m3];
[base]crop=1220:1532:0:70[photo];
[bb]nullsink;
[t1][t2][top][m1][m2][m3][photo][face]vstack=inputs=8" \
  -frames:v 1 -q:v 3 composeApp/src/commonMain/composeResources/drawable/bathroom_wall.jpg
ls -lh composeApp/src/commonMain/composeResources/drawable/bathroom_wall.jpg
