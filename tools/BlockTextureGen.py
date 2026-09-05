#!/usr/bin/env python3
"""Generate the 16x16 block textures.

Why this exists
---------------
Roughly half the block textures were flat two-colour placeholders under 200 bytes. At a distance
that is survivable; standing on a platform it is not, because a course is read almost entirely
through its surfaces. A player decides whether a block is standable, breakable or lethal from its
texture before they touch it, so a texture that says nothing costs them a life to learn.

The rules here, in order of importance:

1. **Function before beauty.** A hazard reads dark with hard highlights; a breakable block reads
   as seams; a solid platform reads as an unbroken surface with a lit top edge. Someone should be
   able to sort an unfamiliar block into those three buckets from six blocks away.
2. **Top faces are the ones players look at.** In a side-on 2.5D course the top edge of every
   platform sits on the player's eye line, so every tiling block gets a lighter top row and a
   darker bottom row. That one convention does most of the work of making a course look built
   rather than extruded.
3. **Distinct at a glance, not on inspection.** ``checkTextureAssets`` rejects near-duplicates
   inside a directory, which is a floor and not a goal: two blocks that differ only in hue are
   still two blocks the player cannot tell apart while moving.

Everything is drawn from geometry, so it stays original work and regenerates identically.

Run:  python tools/BlockTextureGen.py src/main/resources/assets/planeshift/textures/block
"""

import math
import os
import sys

from PIL import Image, ImageDraw

S = 16
CLEAR = (0, 0, 0, 0)


def shade(c, f):
    """Scale a colour toward black (f<1) or white (f>1), keeping it opaque."""
    r, g, b = c[:3]
    if f <= 1.0:
        return (int(r * f), int(g * f), int(b * f), 255)
    t = f - 1.0
    return (int(r + (255 - r) * t), int(g + (255 - g) * t), int(b + (255 - b) * t), 255)


def new(fill=CLEAR):
    if len(fill) == 3:
        fill = fill + (255,)
    return Image.new("RGBA", (S, S), fill)


def lit(img, base):
    """Apply the house lighting convention: bright top rows, dark bottom rows.

    This is the single most valuable thing in the file. Minecraft shades whole faces, not edges,
    so without a baked highlight a run of identical blocks is one flat slab and the player cannot
    see where one platform ends and the next begins.
    """
    d = ImageDraw.Draw(img)
    d.line([(0, 0), (S - 1, 0)], fill=shade(base, 1.28))
    d.line([(0, 1), (S - 1, 1)], fill=shade(base, 1.12))
    d.line([(0, S - 1), (S - 1, S - 1)], fill=shade(base, 0.66))
    d.line([(0, S - 2), (S - 1, S - 2)], fill=shade(base, 0.82))
    return img


def grain(img, base, seed, amount=0.06, density=3):
    """Sprinkle deterministic value noise so a face is not perfectly flat.

    Uses a hash rather than ``random`` so the file regenerates byte-identically anywhere.
    """
    px = img.load()
    for y in range(S):
        for x in range(S):
            if px[x, y][3] == 0:
                continue
            h = (x * 374761393 + y * 668265263 + seed * 2246822519) & 0xFFFFFFFF
            h = ((h ^ (h >> 13)) * 1274126177) & 0xFFFFFFFF
            if h % density:
                continue
            f = 1.0 + (amount if (h >> 16) & 1 else -amount)
            px[x, y] = shade(px[x, y], f)
    return img


# ---------------------------------------------------------------- tiling surfaces


def plain(base, seed, noise=0.06):
    img = new(base)
    grain(img, base, seed, noise)
    return lit(img, base)


def planks(base, seed):
    img = plain(base, seed, 0.08)
    d = ImageDraw.Draw(img)
    dark = shade(base, 0.62)
    for y in (4, 9, 14):
        d.line([(0, y), (S - 1, y)], fill=dark)
    # Butt joints, staggered, so the boards read as boards and not as stripes.
    d.line([(6, 0), (6, 3)], fill=dark)
    d.line([(11, 5), (11, 8)], fill=dark)
    d.line([(3, 10), (3, 13)], fill=dark)
    return lit(img, base)


def tiles(a, b, seed):
    img = new()
    d = ImageDraw.Draw(img)
    for ty in range(2):
        for tx in range(2):
            c = a if (tx + ty) % 2 == 0 else b
            d.rectangle([tx * 8, ty * 8, tx * 8 + 7, ty * 8 + 7], fill=c + (255,))
            d.rectangle([tx * 8, ty * 8, tx * 8 + 7, ty * 8 + 7], outline=shade(c, 0.7))
            d.line([(tx * 8 + 1, ty * 8 + 1), (tx * 8 + 6, ty * 8 + 1)], fill=shade(c, 1.2))
    grain(img, a, seed, 0.04)
    return lit(img, a)


def studded(base, stud, seed, inset=2):
    """A hard block: solid field, heavy border, four corner bolts. Reads as 'does not break'."""
    img = plain(base, seed, 0.04)
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, S - 1, S - 1], outline=shade(base, 0.55))
    d.rectangle([inset, inset, S - 1 - inset, S - 1 - inset], outline=shade(base, 1.18))
    for cx, cy in ((3, 3), (12, 3), (3, 12), (12, 12)):
        d.rectangle([cx - 1, cy - 1, cx + 1, cy + 1], fill=stud + (255,))
        d.point((cx - 1, cy - 1), fill=shade(stud, 1.4))
    return lit(img, base)


# ------------------------------------------------------------------- emblems


def question_mark(d, colour, shadow):
    """The most recognisable shape in the genre; worth drawing properly rather than as a blob."""
    pts = [(6, 4), (7, 4), (8, 4), (9, 5), (9, 6), (8, 7), (7, 8), (7, 9),
           (6, 5), (7, 11), (7, 12)]
    if shadow[3]:
        for x, y in pts:
            d.point((x + 1, y + 1), fill=shadow)
    for x, y in pts:
        d.point((x, y), fill=colour)


def coin_face(d, gold):
    d.ellipse([3, 2, 12, 13], fill=gold + (255,), outline=shade(gold, 0.6))
    d.ellipse([5, 4, 10, 11], outline=shade(gold, 0.78))
    d.line([(7, 4), (7, 11)], fill=shade(gold, 1.35))
    d.point((5, 4), fill=shade(gold, 1.5))


def chevrons(d, colour):
    """Direction arrows for the conveyor. Motion has to be legible while standing still."""
    for row in range(4):
        y = row * 4 + 1
        for i in range(3):
            x = 3 + i * 4
            d.line([(x, y), (x + 2, y + 1)], fill=colour)
            d.line([(x, y + 2), (x + 2, y + 1)], fill=colour)


# ------------------------------------------------------------------ shapes


def crate(base, seed):
    img = planks(base, seed)
    d = ImageDraw.Draw(img)
    edge = shade(base, 0.5)
    d.rectangle([0, 0, S - 1, S - 1], outline=edge)
    d.line([(1, 1), (S - 2, S - 2)], fill=edge)
    d.line([(S - 2, 1), (1, S - 2)], fill=edge)
    d.line([(2, 1), (S - 2, S - 3)], fill=shade(base, 1.15))
    return lit(img, base)


def trim(base, seed):
    """Horizontal moulding. Reads as a cornice, so it frames a structure instead of filling it."""
    img = plain(base, seed, 0.04)
    d = ImageDraw.Draw(img)
    for y, f in ((2, 1.3), (3, 0.7), (7, 1.22), (8, 0.62), (12, 1.3), (13, 0.7)):
        d.line([(0, y), (S - 1, y)], fill=shade(base, f))
    for x in range(1, S, 5):
        d.line([(x, 9), (x, 11)], fill=shade(base, 0.8))
    return img


def pillar(base, seed):
    """Fluted column: vertical grooves, so a stack of them reads as one tall shaft."""
    img = plain(base, seed, 0.03)
    d = ImageDraw.Draw(img)
    for x in (2, 6, 10, 14):
        d.line([(x, 0), (x, S - 1)], fill=shade(base, 0.68))
        if x + 1 < S:
            d.line([(x + 1, 0), (x + 1, S - 1)], fill=shade(base, 1.2))
    d.line([(0, 0), (S - 1, 0)], fill=shade(base, 1.3))
    d.line([(0, S - 1), (S - 1, S - 1)], fill=shade(base, 0.6))
    return img


def pillar_top(base, seed):
    img = plain(base, seed, 0.03)
    d = ImageDraw.Draw(img)
    d.rectangle([1, 1, 14, 14], outline=shade(base, 0.7))
    d.ellipse([3, 3, 12, 12], fill=shade(base, 1.1), outline=shade(base, 0.72))
    d.ellipse([6, 6, 9, 9], fill=shade(base, 0.85))
    return img


def lattice(base):
    """Cut-out diagonal trellis. The holes are the texture; keep them genuinely transparent."""
    img = new()
    d = ImageDraw.Draw(img)
    for k in range(-S, S * 2, 5):
        d.line([(k, 0), (k + S, S)], fill=base + (255,))
        d.line([(k, S), (k + S, 0)], fill=shade(base, 0.75))
    d.rectangle([0, 0, S - 1, S - 1], outline=shade(base, 0.6))
    return img


def banner(cloth, band, seed):
    img = plain(cloth, seed, 0.05)
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, S - 1, 2], fill=shade(cloth, 0.55))
    for i in range(4):
        x = i * 4
        d.polygon([(x, 6), (x + 2, 10), (x + 4, 6)], fill=band + (255,))
    d.line([(0, 12), (S - 1, 12)], fill=band + (255,))
    # Ragged lower hem so it hangs like cloth rather than ending like a tile.
    for x in range(0, S, 4):
        d.polygon([(x, S - 1), (x + 2, S - 4), (x + 4, S - 1)], fill=CLEAR)
    return img


def lamp(glow, frame):
    img = new(shade(frame, 0.8))
    d = ImageDraw.Draw(img)
    d.rectangle([2, 2, 13, 13], fill=glow + (255,))
    for r, f in ((5, 1.0), (3, 1.12), (1, 1.25)):
        d.ellipse([8 - r, 8 - r, 7 + r, 7 + r], fill=shade(glow, f))
    d.rectangle([0, 0, S - 1, S - 1], outline=frame + (255,))
    d.rectangle([2, 2, 13, 13], outline=shade(frame, 1.1))
    return img


def hedge(base, seed):
    """Dense clumped foliage. Clumps, not noise — noise at 16px turns to grey mush."""
    img = plain(base, seed, 0.10)
    d = ImageDraw.Draw(img)
    for i, (cx, cy) in enumerate(((3, 3), (11, 2), (6, 8), (13, 9), (2, 12), (9, 13))):
        f = 1.18 if i % 2 == 0 else 0.8
        d.ellipse([cx - 2, cy - 2, cx + 2, cy + 2], fill=shade(base, f))
    grain(img, base, seed + 1, 0.12, 2)
    return lit(img, base)


def cloud(base, seed):
    img = new(base)
    d = ImageDraw.Draw(img)
    for cx, cy, r in ((4, 5, 4), (11, 4, 3), (8, 9, 5), (13, 11, 3)):
        d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=shade(base, 1.0))
    for cx, cy, r in ((5, 11, 3), (12, 8, 2)):
        d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=shade(base, 0.9))
    grain(img, base, seed, 0.03)
    d.line([(0, 0), (S - 1, 0)], fill=(255, 255, 255, 255))
    d.line([(0, S - 1), (S - 1, S - 1)], fill=shade(base, 0.78))
    return img


def ice(base):
    """Semi-transparent with internal cracks, so a slide is visible before it is stepped on."""
    img = new(base[:3] + (208,))
    d = ImageDraw.Draw(img)
    bright = shade(base, 1.3)[:3] + (230,)
    for pts in (((2, 2), (6, 7), (5, 12)),
                ((10, 1), (12, 6), (9, 9)),
                ((13, 10), (11, 14), (14, 15))):
        d.line(pts, fill=bright)
    d.line([(0, 0), (S - 1, 0)], fill=(255, 255, 255, 235))
    d.line([(0, S - 1), (S - 1, S - 1)], fill=shade(base, 0.72)[:3] + (220,))
    return img


def grass_top(base, seed):
    img = plain(base, seed, 0.12)
    grain(img, base, seed + 7, 0.16, 2)
    d = ImageDraw.Draw(img)
    for cx, cy in ((3, 4), (10, 2), (13, 9), (6, 12)):
        d.point((cx, cy), fill=shade(base, 1.4))
    return img


def grass_side(dirt, grass, seed):
    img = plain(dirt, seed, 0.10)
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, S - 1, 2], fill=grass + (255,))
    # Fringe: an irregular boundary, because a ruler-straight one looks like a decal.
    for x in range(S):
        h = 3 + ((x * 2654435761) >> 5) % 3
        d.line([(x, 3), (x, h)], fill=shade(grass, 0.9))
    d.line([(0, 0), (S - 1, 0)], fill=shade(grass, 1.25))
    d.line([(0, S - 1), (S - 1, S - 1)], fill=shade(dirt, 0.66))
    return img


def coin_block_side(gold, seed):
    img = studded(gold, (140, 96, 20), seed, 1)
    d = ImageDraw.Draw(img)
    coin_face(d, gold)
    d.ellipse([3, 2, 12, 13], outline=shade(gold, 0.5))
    return img


def coin_block_top(gold, seed):
    img = plain(gold, seed, 0.05)
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, S - 1, S - 1], outline=shade(gold, 0.5))
    d.rectangle([3, 3, 12, 12], outline=shade(gold, 1.3))
    for cx, cy in ((2, 2), (13, 2), (2, 13), (13, 13)):
        d.point((cx, cy), fill=shade(gold, 0.6))
    return img


def hidden_block():
    """Only ever seen as a particle and as a faint hint; must not read as a solid block."""
    img = new()
    d = ImageDraw.Draw(img)
    ghost = (250, 226, 120, 70)
    for x in range(0, S, 3):
        d.point((x, 0), fill=ghost)
        d.point((x, S - 1), fill=ghost)
    for y in range(0, S, 3):
        d.point((0, y), fill=ghost)
        d.point((S - 1, y), fill=ghost)
    question_mark(d, (250, 226, 120, 90), CLEAR)
    return img


def rotating(spinning, seed):
    """SMW turn block.

    Spinning is drawn as a smear rather than a different colour: the player needs to read 'this is
    mid-animation', and a hue change would read as 'different block'.
    """
    base = (226, 176, 66)
    img = studded(base, (150, 108, 30), seed, 1)
    d = ImageDraw.Draw(img)
    if spinning:
        for y in range(2, S - 2, 2):
            d.line([(1, y), (S - 2, y)], fill=shade(base, 1.3))
            d.line([(1, y + 1), (S - 2, y + 1)], fill=shade(base, 0.72))
        d.line([(3, 8), (12, 8)], fill=shade(base, 1.45))
    else:
        white = (250, 244, 226, 255)
        d.line([(4, 8), (11, 8)], fill=white)
        d.polygon([(11, 5), (14, 8), (11, 11)], fill=white)
        d.polygon([(4, 5), (1, 8), (4, 11)], fill=white)
    return img


def conveyor_top():
    base = (60, 60, 68)
    img = plain(base, 61, 0.05)
    d = ImageDraw.Draw(img)
    chevrons(d, (232, 190, 60, 255))
    return img


def conveyor_side():
    base = (74, 74, 84)
    img = new(base)
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, S - 1, 3], fill=(48, 48, 56, 255))
    d.rectangle([0, 12, S - 1, S - 1], fill=(48, 48, 56, 255))
    for cx in (3, 8, 13):
        d.ellipse([cx - 3, 4, cx + 3, 11], fill=(150, 150, 160, 255),
                  outline=(36, 36, 44, 255))
        d.ellipse([cx - 1, 6, cx + 1, 9], fill=(90, 90, 100, 255))
    return img


def conveyor_end():
    base = (52, 52, 60)
    img = new(base)
    d = ImageDraw.Draw(img)
    d.ellipse([2, 2, 13, 13], fill=(150, 150, 160, 255), outline=(30, 30, 38, 255))
    d.ellipse([6, 6, 9, 9], fill=(226, 186, 60, 255))
    for a in range(0, 360, 45):
        x = 8 + int(4.5 * math.cos(math.radians(a)))
        y = 8 + int(4.5 * math.sin(math.radians(a)))
        d.point((x, y), fill=(60, 60, 70, 255))
    return img


def trampoline_side(base, seed):
    img = plain(base, seed, 0.05)
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, S - 1, 4], fill=shade(base, 1.2))
    d.rectangle([0, 5, S - 1, S - 1], fill=shade(base, 0.7))
    for x in range(1, S - 2, 3):
        d.line([(x, 6), (x + 2, S - 2)], fill=shade(base, 1.05))
    return img


def trampoline_top(base):
    """Taut fabric: concentric rings say 'this launches you' before the player lands on it."""
    img = new(base)
    d = ImageDraw.Draw(img)
    for r, f in ((7, 0.8), (5, 1.0), (3, 1.18), (1, 1.35)):
        d.ellipse([8 - r, 8 - r, 7 + r, 7 + r], outline=shade(base, f))
    d.rectangle([0, 0, S - 1, S - 1], outline=shade(base, 0.55))
    return img


def spring_top(base):
    img = new(shade(base, 0.6))
    d = ImageDraw.Draw(img)
    for r, f in ((7, 0.75), (5, 1.15), (3, 0.9), (1, 1.3)):
        d.ellipse([8 - r, 8 - r, 7 + r, 7 + r], outline=shade(base, f))
    d.line([(8, 1), (8, 4)], fill=shade(base, 1.4))
    return img


def pipe_top(base):
    """The pipe mouth: a ring with a dark hole, so a warp reads as an opening, not a lid."""
    img = new()
    d = ImageDraw.Draw(img)
    d.ellipse([0, 0, S - 1, S - 1], fill=base + (255,), outline=shade(base, 0.5))
    d.ellipse([2, 2, 13, 13], fill=shade(base, 1.18))
    d.ellipse([4, 4, 11, 11], fill=(22, 40, 24, 255))
    d.arc([1, 1, 14, 14], 150, 250, fill=shade(base, 1.4))
    return img


def muncher():
    """Black bulb with white fangs. Black plus hard white is the genre's universal 'do not touch'."""
    img = new()
    d = ImageDraw.Draw(img)
    body = (30, 28, 40)
    d.ellipse([2, 5, 13, 15], fill=body + (255,), outline=(12, 10, 18, 255))
    d.ellipse([4, 7, 8, 11], fill=(62, 58, 78, 255))
    for x in (4, 7, 10):
        d.polygon([(x, 8), (x + 1, 5), (x + 2, 8)], fill=(246, 246, 250, 255))
        d.polygon([(x, 13), (x + 1, 15), (x + 2, 13)], fill=(246, 246, 250, 255))
    d.line([(8, 0), (8, 5)], fill=(44, 92, 44, 255))
    d.line([(6, 2), (8, 4)], fill=(58, 116, 54, 255))
    return img


def spikes(metal, base):
    img = new()
    d = ImageDraw.Draw(img)
    d.rectangle([0, 11, S - 1, S - 1], fill=base + (255,))
    d.line([(0, 11), (S - 1, 11)], fill=shade(base, 1.4))
    for i in range(4):
        x = i * 4
        d.polygon([(x, 11), (x + 2, 1), (x + 4, 11)], fill=metal + (255,))
        d.line([(x + 2, 2), (x + 1, 10)], fill=shade(metal, 1.35))
        d.line([(x + 3, 4), (x + 3, 10)], fill=shade(metal, 0.7))
    return img


def cannon_side(base, seed):
    img = plain(base, seed, 0.05)
    d = ImageDraw.Draw(img)
    for y in (3, 8, 13):
        d.line([(0, y), (S - 1, y)], fill=shade(base, 1.35))
        if y + 1 < S:
            d.line([(0, y + 1), (S - 1, y + 1)], fill=shade(base, 0.6))
    for cy in (5, 10):
        for cx in (2, 8, 14):
            d.point((cx, cy), fill=shade(base, 1.6))
    return img


def cannon_front(base):
    img = new(base)
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, S - 1, S - 1], outline=shade(base, 1.3))
    d.ellipse([1, 1, 14, 14], fill=shade(base, 1.25), outline=shade(base, 0.5))
    d.ellipse([3, 3, 12, 12], fill=(10, 10, 14, 255))
    d.arc([2, 2, 13, 13], 160, 260, fill=shade(base, 1.7))
    return img


def pole(base):
    """Only the middle four columns are the pole; the model is thin and the rest must vanish."""
    img = new()
    d = ImageDraw.Draw(img)
    d.rectangle([6, 0, 9, S - 1], fill=base + (255,))
    d.line([(6, 0), (6, S - 1)], fill=shade(base, 1.4))
    d.line([(9, 0), (9, S - 1)], fill=shade(base, 0.62))
    for y in range(0, S, 5):
        d.line([(6, y), (9, y)], fill=shade(base, 0.78))
    return img


def pedestal(stone, gold, seed):
    img = plain(stone, seed, 0.05)
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, S - 1, 3], fill=gold + (255,))
    d.line([(0, 0), (S - 1, 0)], fill=shade(gold, 1.3))
    d.line([(0, 4), (S - 1, 4)], fill=shade(stone, 0.55))
    for x in range(1, S - 1, 4):
        d.line([(x, 6), (x, S - 2)], fill=shade(stone, 0.72))
        d.line([(x + 1, 6), (x + 1, S - 2)], fill=shade(stone, 1.18))
    return img


def pennant():
    """The checkered flag: two-tone, hard edges, no shading, so it reads at any distance."""
    img = new()
    d = ImageDraw.Draw(img)
    for ty in range(8):
        for tx in range(8):
            c = (250, 250, 252, 255) if (tx + ty) % 2 == 0 else (34, 34, 44, 255)
            d.rectangle([tx * 2, ty * 2, tx * 2 + 1, ty * 2 + 1], fill=c)
    # Trim the trailing edge into a swallowtail so it reads as cloth, not as a chessboard.
    for y in range(S):
        cut = abs(y - 8) // 2
        if cut:
            d.line([(S - cut, y), (S - 1, y)], fill=CLEAR)
    return img


def beacon(flag, glowing):
    img = new()
    d = ImageDraw.Draw(img)
    post = (250, 240, 210) if glowing else (206, 202, 194)
    d.rectangle([7, 0, 9, S - 1], fill=post + (255,))
    d.line([(7, 0), (7, S - 1)], fill=shade(post, 1.3))
    d.line([(9, 0), (9, S - 1)], fill=shade(post, 0.7))
    d.polygon([(10, 2), (15, 5), (10, 8)], fill=flag + (255,))
    d.polygon([(10, 2), (15, 5), (10, 5)], fill=shade(flag, 1.2))
    if glowing:
        for r in (3, 2):
            d.ellipse([8 - r, 12 - r, 7 + r, 11 + r], outline=shade(flag, 1.4))
    return img


def ring(base, active):
    img = new()
    d = ImageDraw.Draw(img)
    d.ellipse([1, 1, 14, 14], outline=shade(base, 0.6))
    d.ellipse([2, 2, 13, 13], outline=base + (255,))
    d.ellipse([3, 3, 12, 12], outline=shade(base, 1.25) if active else shade(base, 1.05))
    d.ellipse([4, 4, 11, 11], outline=shade(base, 0.75))
    if active:
        for x, y in ((4, 3), (12, 4), (3, 11), (11, 12)):
            d.point((x, y), fill=(255, 255, 240, 255))
    return img


def vine(base):
    """Two twisting strands plus leaves, so a climb reads as climbable and not as wallpaper."""
    img = new()
    d = ImageDraw.Draw(img)
    for phase, f in ((0, 1.0), (8, 0.78)):
        for y in range(S):
            x = (4 + phase // 2 + int(2.4 * math.sin((y + phase) * 0.6))) % S
            d.point((x, y), fill=shade(base, f))
            d.point(((x + 1) % S, y), fill=shade(base, f * 0.9))
    for y in (2, 6, 10, 14):
        x = 4 + int(2.4 * math.sin(y * 0.6))
        d.polygon([(x - 2, y), (x, y - 2), (x + 1, y)], fill=shade(base, 1.25))
    return img


def axe():
    """A single-bit axe, head up, seen side on.

    The first draft drew the head as a rotated quad and it read as a white pennant — at 16px a
    silhouette either survives or it does not, and 'axe' and 'flag' are the same blob once the
    blade is angled. Squaring the head against the pixel grid and keeping the handle a clear
    vertical costs realism and buys recognition, which is the only thing that matters here.
    """
    img = new()
    d = ImageDraw.Draw(img)
    handle = (124, 82, 46)
    metal = (198, 204, 214)

    # Handle: straight, two pixels wide, with a lit and a shaded edge.
    d.rectangle([7, 6, 8, 15], fill=handle + (255,))
    d.line([(7, 6), (7, 15)], fill=shade(handle, 1.3))
    d.line([(8, 6), (8, 15)], fill=shade(handle, 0.7))
    d.rectangle([6, 13, 9, 15], fill=shade(handle, 0.85))

    # Head: a solid block at the haft, widening into the blade.
    d.rectangle([6, 2, 9, 6], fill=shade(metal, 0.82), outline=shade(metal, 0.5))
    d.polygon([(9, 1), (14, 3), (14, 6), (9, 7)], fill=metal + (255,),
              outline=shade(metal, 0.5))
    d.polygon([(10, 3), (12, 4), (12, 6), (10, 6)], fill=shade(metal, 1.22))
    # Cutting edge, the brightest thing in the icon.
    d.line([(14, 3), (14, 6)], fill=(255, 255, 255, 255))
    d.point((13, 2), fill=(255, 255, 255, 255))
    return img


def switch(base, glyph, pressed):
    img = studded(base, shade(base, 0.55)[:3], 71 if pressed else 70, 1)
    d = ImageDraw.Draw(img)
    face = shade(base, 0.78) if pressed else shade(base, 1.22)
    d.rectangle([3, 3, 12, 12], fill=face, outline=shade(base, 0.5))
    ink = (250, 250, 250, 255)
    if glyph == "P":
        d.line([(6, 5), (6, 11)], fill=ink)
        d.line([(6, 5), (9, 5)], fill=ink)
        d.line([(9, 5), (9, 8)], fill=ink)
        d.line([(9, 8), (6, 8)], fill=ink)
    else:
        d.line([(8, 5), (8, 9)], fill=ink)
        d.point((8, 11), fill=ink)
    if pressed:
        d.line([(3, 3), (12, 3)], fill=shade(base, 0.45))
    return img


def semisolid(base, seed):
    """A platform whose top edge is the whole message.

    A semisolid is passed through from below and landed on from above, so the player has to be
    able to tell at a glance which surface is which. The top gets a heavy lit lip and the sides get
    a hanging underside that reads as unsupported - deliberately unlike every other block in the
    set, all of which are solid all the way round.
    """
    img = plain(base, seed, 0.06)
    d = ImageDraw.Draw(img)
    # Plank grain on the face.
    for y in (5, 10, 15):
        d.line([(0, y), (S - 1, y)], fill=shade(base, 0.66))
    # The lip: three rows of it, much brighter than the house convention, because this is the one
    # edge that has to be unmistakable.
    d.rectangle([0, 0, S - 1, 2], fill=shade(base, 1.42))
    d.line([(0, 3), (S - 1, 3)], fill=shade(base, 1.16))
    # Hanging underside: broken, so the bottom does not read as resting on anything.
    for x in range(0, S, 4):
        d.rectangle([x, S - 2, x + 1, S - 1], fill=CLEAR)
    return img


def semisolid_top(base, seed):
    """The walking surface: boards across, with a bright rim all round."""
    img = plain(base, seed, 0.05)
    d = ImageDraw.Draw(img)
    for x in (4, 9, 14):
        d.line([(x, 0), (x, S - 1)], fill=shade(base, 0.68))
    d.rectangle([0, 0, S - 1, S - 1], outline=shade(base, 1.35))
    return img


def note_block(base):
    """A Mario note block: white face, black quaver, and — unlike the version this replaces — a
    frame and the house lighting.

    The concept was right and is kept. What was missing is that a flat white square with a glyph on
    it is a sticker, not a block: with no darker frame and no lit top row it has no thickness, so a
    row of them in a course reads as one white wall. The note is what identifies it; the frame and
    the shading are what make it an object.
    """
    img = new(base)
    d = ImageDraw.Draw(img)
    grain(img, base, 81, 0.02)
    d.rectangle([0, 0, S - 1, S - 1], outline=shade(base, 0.42))
    d.rectangle([1, 1, S - 2, S - 2], outline=shade(base, 0.72))
    ink = (26, 24, 34, 255)
    # Quaver: filled head, stem, flag.
    d.ellipse([4, 9, 8, 13], fill=ink)
    d.rectangle([7, 3, 8, 11], fill=ink)
    d.polygon([(9, 3), (12, 5), (12, 8), (9, 6)], fill=ink)
    return lit(img, base)


def dirt(base, seed):
    """Clumped soil.

    Replaces a version drawn as horizontal bands, which is the visual cue for *planks* — the block
    it has to sit under is grass, and a striped brown block under a grass top reads as a wooden
    crate with a lawn on it. Dirt has no direction, so the noise here is clumped and unaligned.
    """
    img = plain(base, seed, 0.10)
    d = ImageDraw.Draw(img)
    for cx, cy, r in ((3, 4, 2), (11, 3, 2), (7, 9, 2), (13, 11, 2), (2, 12, 1), (9, 13, 1)):
        d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=shade(base, 0.82))
    for cx, cy in ((5, 2), (12, 7), (4, 10), (14, 14)):
        d.point((cx, cy), fill=shade(base, 1.22))
    grain(img, base, seed + 3, 0.13, 2)
    return lit(img, base)


def donut(base, seed):
    """A donut lift: a raised rim around a sunken centre.

    Must read as *a platform that will fall*, which means it needs a visible rim to stand on and a
    hole in the middle so it is never mistaken for solid ground. The version this replaces was a
    flat orange square with a darker orange square inside it — two tones, no rim, no hole, and the
    same silhouette as every other decorative block in the set.
    """
    img = plain(base, seed, 0.05)
    d = ImageDraw.Draw(img)
    d.ellipse([0, 0, S - 1, S - 1], fill=shade(base, 1.12), outline=shade(base, 0.5))
    d.ellipse([2, 2, 13, 13], outline=shade(base, 0.68))
    d.ellipse([5, 5, 10, 10], fill=shade(base, 0.34), outline=shade(base, 0.5))
    # Rivets on the rim, so the shaking state has something to read against.
    for cx, cy in ((8, 1), (14, 8), (8, 14), (1, 8)):
        d.point((cx, cy), fill=shade(base, 1.35))
    return lit(img, base)


# ------------------------------------------------------------------ catalogue


def build():
    out = {}

    # Structural and building blocks.
    out["course_hard_block"] = studded((150, 138, 118), (92, 84, 72), 11)
    out["course_wood_block"] = planks((150, 106, 62), 12)
    out["course_tile"] = tiles((214, 210, 200), (168, 166, 160), 13)
    out["course_crate"] = crate((162, 118, 68), 14)
    out["course_trim"] = trim((198, 176, 132), 15)
    out["course_pillar"] = pillar((206, 200, 186), 16)
    out["course_pillar_top"] = pillar_top((206, 200, 186), 17)
    out["course_lattice"] = lattice((136, 96, 54))
    out["course_banner"] = banner((198, 54, 62), (240, 214, 118), 19)
    out["course_lamp"] = lamp((252, 226, 150), (128, 96, 44))
    out["course_hedge"] = hedge((46, 112, 48), 21)
    out["course_cloud_block"] = cloud((238, 244, 255), 22)
    out["course_ice_block"] = ice((176, 216, 240))
    out["course_grass_block_top"] = grass_top((88, 158, 62), 24)
    out["course_grass_block"] = grass_side((124, 88, 58), (88, 158, 62), 25)
    out["course_dirt"] = dirt((124, 88, 58), 26)
    out["semisolid_platform"] = semisolid((178, 132, 74), 29)
    out["semisolid_platform_top"] = semisolid_top((198, 152, 92), 30)
    out["note_block"] = note_block((242, 242, 236))
    out["donut_block"] = donut((196, 130, 62), 28)

    # Interactive blocks.
    out["coin_block"] = coin_block_side((236, 182, 46), 31)
    out["coin_block_top"] = coin_block_top((236, 182, 46), 32)
    out["hidden_question_block"] = hidden_block()
    out["rotating_block"] = rotating(False, 34)
    out["rotating_block_spinning"] = rotating(True, 35)
    out["conveyor_belt_top"] = conveyor_top()
    out["conveyor_belt_side"] = conveyor_side()
    out["conveyor_belt"] = conveyor_end()
    out["trampoline"] = trampoline_side((222, 118, 40), 39)
    out["trampoline_top"] = trampoline_top((242, 152, 62))
    out["spring_pad_top"] = spring_top((214, 214, 222))
    out["warp_pipe_top"] = pipe_top((58, 176, 74))

    # Hazards.
    out["muncher"] = muncher()
    out["spike_block"] = spikes((186, 190, 200), (58, 58, 70))
    out["bullet_bill_cannon"] = cannon_side((44, 44, 52), 45)
    out["bullet_bill_cannon_front"] = cannon_front((44, 44, 52))

    # Goal and progress.
    out["flag_pole"] = pole((178, 184, 196))
    out["flag_pole_base"] = pedestal((150, 146, 140), (232, 196, 72), 52)
    out["flag_pole_flag"] = pennant()
    out["checkpoint_beacon"] = beacon((72, 176, 96), False)
    out["checkpoint_beacon_lit"] = beacon((250, 206, 72), True)
    out["coin_ring_block"] = ring((248, 206, 72), True)
    out["coin_ring_block_used"] = ring((132, 132, 138), False)
    out["course_vine"] = vine((66, 142, 56))
    out["axe_block"] = axe()

    # Switches.
    out["p_switch"] = switch((62, 104, 220), "P", False)
    out["p_switch_pressed"] = switch((44, 70, 156), "P", True)
    out["on_off_switch"] = switch((214, 78, 62), "!", False)
    out["on_off_switch_powered"] = switch((96, 200, 232), "!", True)

    return out


def main():
    if len(sys.argv) < 2:
        print(__doc__)
        return 1
    target = sys.argv[1]
    os.makedirs(target, exist_ok=True)
    made = build()
    for name, img in sorted(made.items()):
        img.save(os.path.join(target, name + ".png"))
    print("wrote %d block textures to %s" % (len(made), target))
    return 0


if __name__ == "__main__":
    sys.exit(main())
