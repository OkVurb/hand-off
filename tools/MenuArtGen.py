#!/usr/bin/env python3
"""Generate the title-screen art FancyMenu uses.

Why this exists
---------------
FancyMenu can put an image behind the vanilla title screen, but it cannot draw one. The instance
had six 512x512 course skyboxes sitting in its assets folder, which are square and would stretch
badly across a 16:9 menu, so this draws proper wide art instead.

Everything here is drawn from primitives in the same palette as BlockTextureGen, so the menu looks
like the game rather than like a screenshot of it.

Run:  python tools/MenuArtGen.py "<instance>/config/fancymenu/assets"
"""

import math
import os
import sys

from PIL import Image, ImageDraw, ImageFilter

W, H = 1920, 1080

SKY_TOP = (92, 168, 232)
SKY_BOTTOM = (176, 222, 248)
HILL_FAR = (122, 186, 118)
HILL_NEAR = (86, 158, 84)
GROUND = (104, 172, 92)
DIRT = (142, 102, 62)
CLOUD = (252, 252, 252)


def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def sky(img):
    d = ImageDraw.Draw(img)
    for y in range(H):
        d.line([(0, y), (W, y)], fill=lerp(SKY_TOP, SKY_BOTTOM, y / H))


def cloud(d, cx, cy, scale):
    """A rounded cloud built from overlapping circles, the way the games draw them."""
    for dx, dy, r in ((-1.6, 0.1, 0.62), (-0.6, -0.3, 0.86), (0.5, -0.15, 0.78), (1.5, 0.15, 0.58)):
        d.ellipse([cx + dx * scale - r * scale, cy + dy * scale - r * scale,
                   cx + dx * scale + r * scale, cy + dy * scale + r * scale], fill=CLOUD)
    d.rectangle([cx - 2.1 * scale, cy, cx + 2.1 * scale, cy + 0.62 * scale], fill=CLOUD)


def hills(img):
    """Two bands of rolling hills. The far band is lighter, which is the whole depth cue."""
    d = ImageDraw.Draw(img)
    for band, (colour, base, amp, freq) in enumerate((
            (HILL_FAR, 760, 54, 0.0031),
            (HILL_NEAR, 850, 76, 0.0019))):
        pts = [(x, base + math.sin(x * freq + band * 2.1) * amp
                + math.sin(x * freq * 2.7 + band) * amp * 0.35) for x in range(0, W + 8, 8)]
        d.polygon(pts + [(W, H), (0, H)], fill=colour)


def ground(img):
    d = ImageDraw.Draw(img)
    top = 960
    d.rectangle([0, top, W, H], fill=DIRT)
    d.rectangle([0, top, W, top + 26], fill=GROUND)
    # Block seams, so the floor reads as the game's grid rather than as a painted strip.
    for x in range(0, W, 48):
        d.line([(x, top), (x, H)], fill=(120, 86, 52), width=2)
    for y in range(top + 48, H, 48):
        d.line([(0, y), (W, y)], fill=(120, 86, 52), width=2)


def background(path):
    img = Image.new("RGB", (W, H))
    sky(img)

    clouds = Image.new("RGB", (W, H))
    clouds.paste(img)
    d = ImageDraw.Draw(clouds)
    for cx, cy, s in ((240, 210, 46), (760, 150, 34), (1310, 240, 52), (1680, 130, 30)):
        cloud(d, cx, cy, s)
    # A touch of blur on the clouds only: it reads as distance without softening the block grid.
    img = Image.blend(img, clouds.filter(ImageFilter.GaussianBlur(1.2)), 1.0)

    hills(img)
    ground(img)

    # A gentle vignette, so the buttons in the middle stay legible over the brightest part.
    veil = Image.new("L", (W, H), 0)
    vd = ImageDraw.Draw(veil)
    vd.ellipse([-W * 0.15, -H * 0.35, W * 1.15, H * 1.35], fill=90)
    veil = veil.filter(ImageFilter.GaussianBlur(180))
    img = Image.composite(img, Image.new("RGB", (W, H), (24, 34, 52)),
                          veil.point(lambda v: 255 - (90 - v)))
    img.save(path)
    print("  wrote %s (%dx%d)" % (os.path.basename(path), W, H))


def wordmark(path):
    """The game's name, drawn as blocks rather than set in a font.

    A text element would need FancyMenu to pick a font and would land differently at every GUI
    scale. An image is the same everywhere, which is what a wordmark has to be.

    <p>Cells are drawn as one continuous run per stroke, not as individual squares. Drawn
    separately with a gap they read as beads on a string rather than as letters, which is a real
    difference at menu size and the reason the first version of this was unusable.
    """
    glyphs = {
        "P": ["1111", "1001", "1001", "1111", "1000", "1000", "1000"],
        "L": ["1000", "1000", "1000", "1000", "1000", "1000", "1111"],
        "A": ["0110", "1001", "1001", "1111", "1001", "1001", "1001"],
        "N": ["1001", "1101", "1101", "1011", "1011", "1001", "1001"],
        "E": ["1111", "1000", "1000", "1110", "1000", "1000", "1111"],
        "S": ["0111", "1000", "1000", "0110", "0001", "0001", "1110"],
        "H": ["1001", "1001", "1001", "1111", "1001", "1001", "1001"],
        "I": ["111", "010", "010", "010", "010", "010", "111"],
        "F": ["1111", "1000", "1000", "1110", "1000", "1000", "1000"],
        "T": ["11111", "00100", "00100", "00100", "00100", "00100", "00100"],
    }
    word = "PLANESHIFT"
    cell = 16
    gap = 1          # in cells, between letters
    rows = 7
    margin = 18
    depth = 6        # shadow offset

    widths = [len(glyphs[ch][0]) for ch in word]
    cols = sum(widths) + gap * (len(word) - 1)
    sw = cols * cell + margin * 2 + depth
    sh = rows * cell + margin * 2 + depth

    img = Image.new("RGBA", (sw, sh), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    # Lay the whole word into one grid first, so strokes can be drawn as continuous runs.
    grid = [[False] * cols for _ in range(rows)]
    cx = 0
    for ch, w in zip(word, widths):
        g = glyphs[ch]
        for r in range(rows):
            for c in range(w):
                if g[r][c] == "1":
                    grid[r][cx + c] = True
        cx += w + gap

    def runs(row):
        """Contiguous filled spans in a row, as (start, end) in cells."""
        out, run = [], None
        for c in range(cols):
            if grid[row][c] and run is None:
                run = c
            elif not grid[row][c] and run is not None:
                out.append((run, c)); run = None
        if run is not None:
            out.append((run, cols))
        return out

    def rect(x0, y0, x1, y1, fill):
        d.rectangle([x0, y0, x1 - 1, y1 - 1], fill=fill)

    # Shadow, face, then a highlight on any cell with nothing above it: the same three-tone
    # treatment the block textures use, so the wordmark belongs to the same world.
    for r in range(rows):
        for a, b in runs(r):
            x0 = margin + a * cell
            x1 = margin + b * cell
            y0 = margin + r * cell
            rect(x0 + depth, y0 + depth, x1 + depth, y0 + cell + depth, (26, 30, 46, 205))
    for r in range(rows):
        for a, b in runs(r):
            x0 = margin + a * cell
            x1 = margin + b * cell
            y0 = margin + r * cell
            rect(x0, y0, x1, y0 + cell, (226, 68, 54, 255))
    for r in range(rows):
        for a, b in runs(r):
            for c in range(a, b):
                if r > 0 and grid[r - 1][c]:
                    continue
                x0 = margin + c * cell
                y0 = margin + r * cell
                rect(x0, y0, x0 + cell, y0 + 4, (255, 156, 136, 255))

    img.save(path)
    print("  wrote %s (%dx%d)" % (os.path.basename(path), sw, sh))


def main():
    if len(sys.argv) < 2:
        print(__doc__)
        return 1
    target = sys.argv[1]
    os.makedirs(target, exist_ok=True)
    print("rendering menu art:")
    background(os.path.join(target, "menu_background.png"))
    wordmark(os.path.join(target, "menu_wordmark.png"))
    return 0


if __name__ == "__main__":
    sys.exit(main())
