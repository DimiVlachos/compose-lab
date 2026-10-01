"""Turn a photo of fogged glass into the fogged window's two textures.

Source: "Water droplets on foggy glass" by Chris F, Pexels
https://www.pexels.com/photo/water-droplets-on-foggy-glass-8628343/ (Pexels License: free to use
and modify). Download the original into scripts/fog_full.jpg, then run from the repo root:

    python3 -m venv /tmp/fogenv && /tmp/fogenv/bin/pip install pillow numpy
    /tmp/fogenv/bin/python scripts/fog-texture.py

It writes composeApp/src/commonMain/composeResources/drawable/fog_detail.jpg, the droplets, drops and
trails as grey around mid-grey for an overlay blend, with the photo's own scene taken out; and
fog_density.png, how thick the fog is, mostly even and thinner along the drips.
"""
from PIL import Image
import numpy as np

def gauss1d(sigma):
    r = max(1, int(3 * sigma))
    x = np.arange(-r, r + 1, dtype=np.float32)
    k = np.exp(-x * x / (2 * sigma * sigma))
    return k / k.sum()

def blur(a, sx, sy=None):
    """Separable Gaussian in float, sx across, sy down (edges reflected)."""
    sy = sx if sy is None else sy
    out = a
    for axis, s in ((1, sx), (0, sy)):
        k = gauss1d(s)
        r = len(k) // 2
        pad = [(0, 0), (0, 0)]
        pad[axis] = (r, r)
        p = np.pad(out, pad, mode='reflect')
        out = sum(k[i] * np.take(p, range(i, i + out.shape[axis]), axis=axis) for i in range(len(k)))
    return out

src = Image.open('scripts/fog_full.jpg').convert('L')
w, h = src.size
cw = int(h * 4 / 5)                      # a 4:5 portrait crop from the middle
x0 = (w - cw) // 2
OUT_W, OUT_H = 1296, 1620
L = np.asarray(src.crop((x0, 0, x0 + cw, h)).resize((OUT_W, OUT_H), Image.LANCZOS), dtype=np.float32) / 255.0

base = blur(L, 10)                       # the scene: what the high-pass throws away
hp = L - base                            # droplets, drops, trail edges
local = blur(L, 30) + 0.04               # local brightness, so dark areas count like bright ones
detail = hp / local
detail /= np.percentile(np.abs(detail), 99) + 1e-9
detail_img = np.clip(128 + detail * 110, 0, 255).astype(np.uint8)

# Speckle, measured tall and narrow: trails run down the glass, drop clusters do not.
energy = blur(np.abs(hp) / local, 3, 30)
energy /= blur(energy, 120) + 1e-6       # keep fog-versus-trail contrast, not the photo's broad trend
# Mostly even fog; thinned only along the clearest streaks, the few trails drops cut.
cut = np.percentile(energy, 15)
trail = np.clip((cut - energy) / (cut - np.percentile(energy, 2) + 1e-6), 0, 1)
trail = trail * trail * (3 - 2 * trail)
density = 0.92 - 0.6 * blur(trail, 1.5, 4)
density_img = np.clip(density * 255, 0, 255).astype(np.uint8)

Image.fromarray(detail_img).save('composeApp/src/commonMain/composeResources/drawable/fog_detail.jpg', quality=88)
# Thickness in the alpha channel of white, so the app can mask the fog with it.
alpha = Image.fromarray(density_img).resize((OUT_W // 3, OUT_H // 3), Image.LANCZOS)
white = Image.new('L', alpha.size, 255)
Image.merge('LA', (white, alpha)).save(
    'composeApp/src/commonMain/composeResources/drawable/fog_density.png', optimize=True
)
print('density mean %.2f p10 %.2f p90 %.2f' % (density.mean(), *np.percentile(density, [10, 90])))
