#!/usr/bin/env python3
"""Generate the per-theme course skyboxes.

How the renderer actually works
-------------------------------
``CourseSkyboxRenderer`` maps UV (0,0)-(1,1) onto **every one of the six cube faces**. It is not a
cubemap atlas — the same image is stretched across all six. The previous skyboxes were authored as
though it were a panorama, at 1536x1024 and 1.3 MB each, which is why they looked wrong overhead
and cost roughly two thirds of the jar.

Designing for what it does rather than what it looks like it should do gives two rules:

1. **Keep detail low on the image.** Every face shows the whole picture, including the one above
   the player. Scenery in the bottom quarter reads as a horizon on the side faces and stays
   harmlessly out of the way on the top face; scenery in the middle appears directly overhead.
2. **Keep it small.** A 512x512 image stretched over a 200-block cube is already far past the
   resolution anyone can resolve. 1536x1024 bought nothing but file size.

Style
-----
Flat, banded colour rather than photographic gradients — the look of a painted platformer backdrop
rather than a sky texture. Each theme carries silhouettes of that theme's own cast on the horizon,
which is what ties the backdrop to the level in front of it instead of being generic scenery.

Run:  python tools/SkyboxGen.py src/main/resources/assets/planeshift/textures/environment
"""

import math
import os
import random
import sys

from PIL import Image, ImageDraw, ImageFilter

SIZE = 512
HORIZON = int(SIZE * 0.72)


def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def sky(draw, top, bottom, bands=14):
    """Banded sky. Discrete steps read as painted; a smooth gradient reads as a photograph."""
    step = HORIZON / bands
    for i in range(bands):
        c = lerp(top, bottom, i / max(1, bands - 1))
        draw.rectangle([0, int(i * step), SIZE, int((i + 1) * step) + 1], fill=c)


def cloud(draw, cx, cy, scale, colour=(255, 255, 255)):
    """Chunky lobed cloud. Three overlapping ellipses is the whole trick."""
    for dx, dy, r in ((-1.0, 0.1, 0.55), (0.0, -0.25, 0.75), (1.0, 0.1, 0.55)):
        draw.ellipse([cx + dx * scale - r * scale, cy + dy * scale - r * scale,
                      cx + dx * scale + r * scale, cy + dy * scale + r * scale], fill=colour)
    draw.rectangle([cx - 1.4 * scale, cy - 0.1 * scale, cx + 1.4 * scale, cy + 0.5 * scale],
                   fill=colour)


def hills(draw, base_y, colour, count, amp, seed):
    """A row of rounded hills along the horizon."""
    r = random.Random(seed)
    x = -60
    while x < SIZE + 60:
        w = r.randint(90, 190)
        h = r.randint(int(amp * 0.5), amp)
        draw.ellipse([x, base_y - h, x + w, base_y + h], fill=colour)
        x += int(w * 0.62)


# ----------------------------------------------------------------- silhouettes
# Deliberately simple: at this size only the outline survives, so each shape is chosen to be
# unmistakable in outline alone — a Goomba is a wide dome on two feet, a Boo is a sphere with a
# ragged tail, a Piranha Plant is a stalk with a bulb.

def goomba(draw, x, y, s, c):
    draw.ellipse([x - s, y - s * 0.85, x + s, y + s * 0.35], fill=c)
    draw.rectangle([x - s * 0.75, y + s * 0.1, x - s * 0.15, y + s * 0.55], fill=c)
    draw.rectangle([x + s * 0.15, y + s * 0.1, x + s * 0.75, y + s * 0.55], fill=c)


def koopa(draw, x, y, s, c):
    draw.ellipse([x - s * 0.9, y - s * 0.7, x + s * 0.9, y + s * 0.5], fill=c)
    draw.ellipse([x + s * 0.3, y - s * 1.35, x + s * 1.1, y - s * 0.55], fill=c)
    draw.rectangle([x - s * 0.5, y + s * 0.3, x - s * 0.1, y + s * 0.75], fill=c)
    draw.rectangle([x + s * 0.15, y + s * 0.3, x + s * 0.55, y + s * 0.75], fill=c)


def piranha(draw, x, y, s, c):
    draw.rectangle([x - s * 0.18, y - s * 0.9, x + s * 0.18, y + s * 0.6], fill=c)
    draw.ellipse([x - s * 0.7, y - s * 1.7, x + s * 0.7, y - s * 0.6], fill=c)
    draw.polygon([(x - s * 0.7, y - s * 1.1), (x - s * 1.2, y - s * 1.4),
                  (x - s * 0.65, y - s * 1.45)], fill=c)


def boo(draw, x, y, s, c):
    draw.ellipse([x - s, y - s, x + s, y + s * 0.8], fill=c)
    draw.polygon([(x - s * 0.8, y + s * 0.5), (x, y + s * 1.5), (x + s * 0.8, y + s * 0.5)], fill=c)


def bowser_spire(draw, x, y, s, c):
    draw.polygon([(x - s, y), (x - s * 0.35, y - s * 2.4), (x, y - s * 1.7),
                  (x + s * 0.35, y - s * 2.6), (x + s, y)], fill=c)


def cactus(draw, x, y, s, c):
    draw.rectangle([x - s * 0.22, y - s * 1.6, x + s * 0.22, y], fill=c)
    draw.rectangle([x - s * 0.8, y - s * 1.0, x - s * 0.22, y - s * 0.75], fill=c)
    draw.rectangle([x - s * 0.8, y - s * 1.5, x - s * 0.55, y - s * 0.75], fill=c)
    draw.rectangle([x + s * 0.22, y - s * 1.25, x + s * 0.8, y - s], fill=c)
    draw.rectangle([x + s * 0.55, y - s * 1.7, x + s * 0.8, y - s], fill=c)


def peak(draw, x, y, s, c, cap=None):
    draw.polygon([(x - s, y), (x, y - s * 1.8), (x + s, y)], fill=c)
    if cap:
        draw.polygon([(x - s * 0.34, y - s * 1.2), (x, y - s * 1.8), (x + s * 0.34, y - s * 1.2)],
                     fill=cap)


def pipe(draw, x, y, s, c):
    draw.rectangle([x - s * 0.5, y - s * 1.4, x + s * 0.5, y], fill=c)
    draw.rectangle([x - s * 0.72, y - s * 1.75, x + s * 0.72, y - s * 1.3], fill=c)


# ----------------------------------------------------------------- themes

def grass():
    img = Image.new("RGB", (SIZE, SIZE), (110, 200, 245))
    d = ImageDraw.Draw(img)
    sky(d, (36, 120, 226), (150, 216, 250))
    d.rectangle([0, HORIZON, SIZE, SIZE], fill=(74, 166, 74))
    for cx, cy, s in ((80, 110, 26), (250, 78, 34), (410, 130, 22), (330, 170, 18)):
        cloud(d, cx, cy, s)
    hills(d, HORIZON + 6, (52, 138, 58), 5, 66, 11)
    hills(d, HORIZON + 26, (40, 116, 46), 4, 52, 12)
    SIL = (28, 84, 40)
    goomba(d, 96, HORIZON - 6, 17, SIL)
    koopa(d, 168, HORIZON - 8, 16, SIL)
    pipe(d, 300, HORIZON + 2, 26, (32, 120, 52))
    piranha(d, 300, HORIZON - 34, 17, SIL)
    goomba(d, 404, HORIZON - 4, 14, SIL)
    return img


def desert():
    img = Image.new("RGB", (SIZE, SIZE), (246, 206, 140))
    d = ImageDraw.Draw(img)
    sky(d, (232, 150, 72), (252, 224, 168))
    d.ellipse([370, 60, 452, 142], fill=(255, 246, 206))
    d.rectangle([0, HORIZON, SIZE, SIZE], fill=(226, 186, 118))
    for cx, cy, s in ((110, 96, 24), (300, 66, 20)):
        cloud(d, cx, cy, s, (252, 232, 200))
    hills(d, HORIZON + 8, (214, 172, 104), 4, 58, 21)
    hills(d, HORIZON + 30, (196, 152, 88), 4, 46, 22)
    SIL = (140, 96, 48)
    cactus(d, 88, HORIZON - 2, 22, SIL)
    cactus(d, 350, HORIZON - 4, 18, SIL)
    pipe(d, 232, HORIZON + 2, 24, (168, 120, 56))
    koopa(d, 424, HORIZON - 6, 15, SIL)
    return img


def snow():
    img = Image.new("RGB", (SIZE, SIZE), (206, 230, 250))
    d = ImageDraw.Draw(img)
    sky(d, (96, 150, 214), (222, 240, 252))
    d.rectangle([0, HORIZON, SIZE, SIZE], fill=(238, 246, 252))
    for cx, cy, s in ((140, 88, 30), (330, 120, 24)):
        cloud(d, cx, cy, s, (250, 252, 255))
    peak(d, 110, HORIZON + 10, 92, (150, 182, 214), (246, 250, 255))
    peak(d, 250, HORIZON + 10, 118, (132, 166, 202), (246, 250, 255))
    peak(d, 400, HORIZON + 10, 84, (150, 182, 214), (246, 250, 255))
    SIL = (96, 128, 166)
    goomba(d, 190, HORIZON - 4, 14, SIL)
    koopa(d, 336, HORIZON - 6, 15, SIL)
    r = random.Random(5)
    for _ in range(120):
        x, y = r.randrange(SIZE), r.randrange(HORIZON)
        d.point((x, y), fill=(255, 255, 255))
    return img


def lava():
    img = Image.new("RGB", (SIZE, SIZE), (48, 16, 18))
    d = ImageDraw.Draw(img)
    sky(d, (28, 10, 14), (168, 52, 26))
    d.rectangle([0, HORIZON, SIZE, SIZE], fill=(52, 20, 18))
    peak(d, 120, HORIZON + 12, 100, (34, 14, 14))
    bowser_spire(d, 268, HORIZON + 10, 54, (26, 10, 12))
    peak(d, 404, HORIZON + 12, 86, (34, 14, 14))
    # Lava glow along the base, which is where the light in this theme comes from.
    for i, c in enumerate(((214, 96, 30), (238, 140, 44), (252, 196, 90))):
        d.rectangle([0, SIZE - 26 + i * 8, SIZE, SIZE - 18 + i * 8], fill=c)
    r = random.Random(9)
    for _ in range(90):
        x = r.randrange(SIZE)
        y = r.randrange(HORIZON // 2, SIZE - 30)
        d.point((x, y), fill=(250, 190, 90))
    return img


def underground():
    img = Image.new("RGB", (SIZE, SIZE), (18, 18, 30))
    d = ImageDraw.Draw(img)
    sky(d, (10, 10, 20), (44, 42, 66))
    d.rectangle([0, HORIZON, SIZE, SIZE], fill=(30, 28, 44))
    # Stalactites hanging from the top, the one place a full-image face works in our favour.
    r = random.Random(3)
    x = 0
    while x < SIZE:
        w = r.randint(18, 46)
        h = r.randint(30, 96)
        d.polygon([(x, 0), (x + w, 0), (x + w / 2, h)], fill=(26, 24, 40))
        x += w
    hills(d, HORIZON + 14, (24, 22, 36), 4, 54, 31)
    SIL = (14, 14, 24)
    goomba(d, 150, HORIZON - 4, 15, SIL)
    boo(d, 320, HORIZON - 60, 18, (58, 56, 82))
    for _ in range(60):
        px, py = r.randrange(SIZE), r.randrange(SIZE)
        d.point((px, py), fill=(120, 150, 190))
    return img


def ghost_house():
    img = Image.new("RGB", (SIZE, SIZE), (30, 20, 46))
    d = ImageDraw.Draw(img)
    sky(d, (14, 8, 26), (76, 50, 104))
    d.ellipse([352, 54, 436, 138], fill=(232, 226, 240))
    d.ellipse([336, 44, 420, 128], fill=(30, 20, 46))
    d.rectangle([0, HORIZON, SIZE, SIZE], fill=(26, 18, 38))
    hills(d, HORIZON + 12, (22, 15, 33), 4, 56, 41)
    SIL = (16, 11, 24)
    # Bare trees.
    for tx in (70, 190, 430):
        d.rectangle([tx - 4, HORIZON - 54, tx + 4, HORIZON], fill=SIL)
        for dx, dy in ((-22, -40), (20, -46), (-14, -26), (16, -22)):
            d.line([tx, HORIZON - 40, tx + dx, HORIZON - 40 + dy], fill=SIL, width=3)
    boo(d, 268, HORIZON - 74, 22, (188, 178, 208))
    boo(d, 318, HORIZON - 44, 14, (150, 142, 172))
    return img


def water():
    """Seen from inside the water, not above it.

    The reference's underwater levels are not a beach with a blue filter -- the whole frame is
    submerged, the light comes down from a surface overhead rather than from a sun in a sky, and
    the far distance fades out rather than meeting a horizon. So this skybox has no horizon line at
    all: it is a vertical gradient from a bright, rippling ceiling to a dark floor, with shafts
    coming down through it and silhouettes of the cast dissolving into the murk.
    """
    img = Image.new("RGB", (SIZE, SIZE), (26, 78, 104))
    d = ImageDraw.Draw(img)
    sky(d, (92, 196, 208), (14, 48, 74), bands=22)

    # The surface overhead, rippling. Drawn as a bright band with a wave cut into its underside
    # rather than as a straight line, because a ruled edge reads as a ceiling and not as water.
    for x in range(SIZE):
        crest = 26 + int(7 * math.sin(x * 0.045)) + int(3 * math.sin(x * 0.11))
        d.line([(x, 0), (x, crest)], fill=(168, 232, 236))
        d.line([(x, crest), (x, crest + 3)], fill=(212, 246, 246))

    # Light shafts, angled and translucent. They are what tells the player which way is up when
    # there is no ground in frame.
    for x0, w in ((70, 34), (190, 22), (300, 40), (430, 26)):
        for i in range(w):
            x = x0 + i
            shade = 1.0 - abs(i - w / 2.0) / (w / 2.0)
            if shade <= 0.15:
                continue
            for y in range(30, int(HORIZON * 1.15)):
                fade = max(0.0, 1.0 - y / float(HORIZON * 1.15))
                if (x + y) % 3 == 0 and fade * shade > 0.35:
                    d.point((x + y // 6, y), fill=(150, 226, 232))

    # The cast, fading with depth.
    cheep(d, 120, 250, 18, (18, 62, 88))
    cheep(d, 330, 300, 14, (16, 56, 80))
    cheep(d, 240, 390, 22, (13, 46, 68))
    # Weed on the floor, breaking the bottom edge so the murk has something in it.
    r = random.Random(23)
    for _ in range(26):
        x = r.randrange(SIZE)
        h = r.randrange(20, 70)
        for y in range(SIZE - h, SIZE):
            d.point((x + int(4 * math.sin(y * 0.12)), y), fill=(20, 74, 62))
    return img


def cheep(draw, x, y, s, c):
    """A fish silhouette: body, tail, one fin. Enough to read at this distance and no more."""
    draw.ellipse([x - s, y - s * 0.6, x + s, y + s * 0.6], fill=c)
    draw.polygon([(x + s, y), (x + s * 1.7, y - s * 0.6), (x + s * 1.7, y + s * 0.6)], fill=c)
    draw.polygon([(x - s * 0.2, y - s * 0.55), (x + s * 0.4, y - s * 1.1),
                  (x + s * 0.5, y - s * 0.5)], fill=c)


def sky_theme():
    """Above the weather: pastel gradient, cloud banks below, and nothing solid anywhere.

    The floor of this one is more cloud rather than ground, because the subject of a sky level is
    height -- there should be no visible bottom to fall to, only more sky going down.
    """
    img = Image.new("RGB", (SIZE, SIZE), (150, 205, 245))
    d = ImageDraw.Draw(img)
    sky(d, (74, 142, 226), (232, 228, 250), bands=18)
    # Cloud banks stacked toward the bottom, largest and palest lowest, so depth reads as height.
    for base, scale, colour in ((HORIZON + 40, 52, (255, 255, 255)),
                                (HORIZON + 8, 40, (244, 246, 255)),
                                (HORIZON - 36, 30, (232, 238, 252))):
        for cx in range(-20, SIZE + 40, 96):
            cloud(d, cx + (base % 37), base, scale, colour)
    # A few high wisps, small and far, for the same reason the grass sky has distant hills.
    for cx, cy, s in ((90, 70, 16), (300, 52, 12), (420, 96, 18)):
        cloud(d, cx, cy, s)
    return img


THEMES = {
    "grass": grass,
    "desert": desert,
    "snow": snow,
    "lava": lava,
    "underground": underground,
    "ghost_house": ghost_house,
    # Both of these existed as themes with no skybox at all. CourseSkyboxRenderer builds its
    # texture path from the theme's own name, so a water or sky course was asking for a file that
    # was never drawn -- eight themes, six pictures, and no error anywhere to say so.
    "water": water,
    "sky": sky_theme,
}


def main():
    out = sys.argv[1] if len(sys.argv) > 1 else \
        "src/main/resources/assets/planeshift/textures/environment"
    os.makedirs(out, exist_ok=True)
    total = 0
    for name, fn in THEMES.items():
        img = fn()
        # One soft pass: kills the hard polygon edges without turning the bands into a gradient.
        img = img.filter(ImageFilter.GaussianBlur(0.4))
        path = os.path.join(out, f"course_skybox_{name}.png")
        img.convert("RGBA").save(path, optimize=True)
        size = os.path.getsize(path)
        total += size
        print(f"  {name:12s} {size / 1024:7.1f} KB")
    print(f"total {total / 1024:.1f} KB (was ~6.2 MB)")


if __name__ == "__main__":
    main()
