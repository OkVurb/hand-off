#!/usr/bin/env python3
"""Generate the 128x128 enemy texture sheets, one hand-drawn character at a time.

Why this exists
---------------
Every sheet used to be six copies of one colour. A Goomba was orange from cap to feet; a Boo was
*purple*; Bowser was a uniform dark red. The rigs underneath had a shell, a muzzle, horns and teeth
as separate parts, and every one of them sampled the same swatch — so none of them read as
anything. No amount of extra geometry fixes that: you cannot see a shell that is exactly the colour
of the back it sits on.

Deliberately not a table of palettes
------------------------------------
The obvious structure here is one parameterised function and eleven rows of colours. It was tried
and it is wrong. These are eleven *specific characters*, and what makes each one recognisable is
particular to it: the Goomba is its brow, the Koopa is the contrast between a yellow body and a
green shell, Bowser is the cream of his horns against orange hide. A shared function pushes every
creature toward the same amount of detail in the same places, which is exactly how they ended up
interchangeable the first time. So each one gets its own function and its own decisions.

How the sheet is laid out
-------------------------
128x128, six regions at fixed origins::

    (0,0)   body        (64,0)  head        (0,40)  limbs
    (64,40) shell/hard  (0,80)  muzzle      (64,80) trim/detail

A box in ``BespokeEnemyModel`` picks a *region*, not a picture — that is why every ``texOffs``
there is one of those six pairs. Whatever is painted in a region tiles across every part made of
it, so a region can carry material (scales, masonry, plating) but never anything with a fixed
position.

The exception is the face. Minecraft lays a box's net out with the front face at
``(u + depth, v + depth)`` sized ``width x height``, so a face painted for one box lands correctly
only on a box of exactly that size. That contract is written up in ``BespokeEnemyModel``'s javadoc
and each function below states which box it is painting for.

Run:  python tools/EnemyTextureGen.py src/main/resources/assets/planeshift/textures/entity
"""

import os
import sys

from PIL import Image, ImageDraw

S = 128

BODY, HEAD, LIMB, HARD, MUZZLE, TRIM = (0, 0), (64, 0), (0, 40), (64, 40), (0, 80), (64, 80)
REGION_SIZE = {BODY: (64, 36), HEAD: (64, 36), LIMB: (64, 36),
               HARD: (64, 36), MUZZLE: (64, 40), TRIM: (64, 40)}

WHITE = (250, 250, 252)
INK = (26, 24, 34)


# ---------------------------------------------------------------- helpers


def shade(c, f):
    r, g, b = c[:3]
    if f <= 1.0:
        return (int(r * f), int(g * f), int(b * f), 255)
    t = f - 1.0
    return (int(r + (255 - r) * t), int(g + (255 - g) * t), int(b + (255 - b) * t), 255)


def _hash(x, y, seed):
    k = (x * 374761393 + y * 668265263 + seed * 2246822519) & 0xFFFFFFFF
    return ((k ^ (k >> 13)) * 1274126177) & 0xFFFFFFFF


def base(img, region, colour, seed, ramp=0.30):
    """Fill a region with a colour, a light top-to-bottom ramp and a little grain.

    The ramp matters more than it looks. Minecraft shades whole faces, so without a baked
    gradient a cube-built creature is a stack of evenly lit slabs; a vertical ramp is what gives
    each part the suggestion of a rounded surface.
    """
    ox, oy = region
    w, h = REGION_SIZE[region]
    px = img.load()
    for y in range(oy, oy + h):
        t = (y - oy) / max(1, h - 1)
        f = (1.0 + ramp / 2) - ramp * t
        for x in range(ox, ox + w):
            k = _hash(x, y, seed)
            g = 1.0 + (0.035 if (k >> 16) & 1 else -0.035) if k % 3 == 0 else 1.0
            px[x, y] = shade(colour, f * g)


def rect(img, region, x, y, w, h, colour):
    ox, oy = region
    ImageDraw.Draw(img).rectangle([ox + x, oy + y, ox + x + w - 1, oy + y + h - 1], fill=colour)


def scutes(img, region, colour, rows=5, step=10):
    """Shell plates: a staggered grid with dark seams and a lit top edge."""
    ox, oy = region
    w, h = REGION_SIZE[region]
    dr = ImageDraw.Draw(img)
    for row, yy in enumerate(range(oy + 2, oy + h - 2, 7)):
        offset = 0 if row % 2 == 0 else step // 2
        for xx in range(ox + offset - step, ox + w, step):
            dr.rectangle([xx, yy, xx + step - 2, yy + 5], outline=shade(colour, 0.60))
            dr.line([(xx + 1, yy + 1), (xx + step - 3, yy + 1)], fill=shade(colour, 1.32))


def ribs(img, region, colour, step=4):
    """Horizontal plating, the way a turtle's underside reads."""
    ox, oy = region
    w, h = REGION_SIZE[region]
    dr = ImageDraw.Draw(img)
    for yy in range(oy + 1, oy + h - 1, step):
        dr.line([(ox, yy), (ox + w - 1, yy)], fill=shade(colour, 0.74))
        dr.line([(ox, yy + 1), (ox + w - 1, yy + 1)], fill=shade(colour, 1.20))


def scales(img, region, colour):
    """Overlapping arcs: hide, for the parts of a creature that are not shell."""
    ox, oy = region
    w, h = REGION_SIZE[region]
    dr = ImageDraw.Draw(img)
    for row, yy in enumerate(range(oy + 2, oy + h - 1, 4)):
        offset = 0 if row % 2 == 0 else 3
        for xx in range(ox + offset, ox + w, 6):
            dr.arc([xx, yy - 2, xx + 5, yy + 2], 200, 340, fill=shade(colour, 0.78))


def front(region, w, h, d):
    """Top-left of a box's front face inside the sheet — where a face has to be painted."""
    return region[0] + d, region[1] + d


def eyes(img, at, w, h, sclera=WHITE, pupil=INK, angry=True, pupil_in=True):
    """Two eyes on a face rect, with brows when the creature should look hostile.

    The brows do most of the work. Two dots read as a toy; two dots under a hard diagonal read as
    something that intends to walk into you.
    """
    dr = ImageDraw.Draw(img)
    x, y = at
    ew = max(2, w // 4)
    eh = max(2, h // 2)
    ey = y + max(1, h // 5)
    lx = x + max(1, w // 8)
    rx = x + w - ew - max(1, w // 8)
    for side, ex in ((-1, lx), (1, rx)):
        dr.rectangle([ex, ey, ex + ew - 1, ey + eh - 1], fill=sclera)
        pw = max(1, ew // 2)
        px0 = ex + (ew - pw) if (side < 0) == (not pupil_in) else ex
        dr.rectangle([px0, ey + eh // 3, px0 + pw - 1, ey + eh - 1], fill=pupil)
    if angry:
        for side, ex in ((1, lx), (-1, rx)):
            for i in range(ew + 1):
                step = i if side > 0 else ew - i
                dr.point((ex + i, ey - 1 - step // 2), fill=pupil)


def fangs(img, at, w, h, count, colour, upper=True):
    dr = ImageDraw.Draw(img)
    x, y = at
    step = max(3, w // count)
    yy = y if upper else y + h - 2
    for i in range(count):
        tx = x + 1 + i * step
        dr.polygon([(tx, yy), (tx + step - 2, yy), (tx + (step - 2) // 2, yy + (2 if upper else -2))],
                   fill=colour)


def new_sheet():
    return Image.new("RGBA", (S, S), (0, 0, 0, 0))


# ---------------------------------------------------------------- characters


def goomba():
    """Brown cap, cream underside, and a scowl.

    A Goomba is almost entirely face, so nearly all of the work is in one region. The cap gets a
    scaled hide and a dark rim so the dome reads as a dome; the face plate carries the real
    expression, and the brow region is near-black because the brow is the character. Give a Goomba
    round eyes and no brow and it turns into a mushroom with a smile.

    Face boxes: the 12x9x8 body at (0,0), and the 10x6x1 plate at (0,80) that sits in front of it.
    """
    img = new_sheet()
    cap = (150, 96, 54)
    base(img, BODY, cap, 11)
    scales(img, BODY, cap)
    # Dark rim around the bottom of the cap, where it overhangs the face.
    rect(img, BODY, 0, 30, 64, 6, shade(cap, 0.66))
    eyes(img, front(BODY, 12, 9, 8), 12, 9)

    base(img, HEAD, cap, 12)
    scales(img, HEAD, cap)

    # Feet: darker than the cap, and flat — they are always in shadow under the body.
    base(img, LIMB, (86, 52, 28), 13, ramp=0.18)

    # Fangs and the head stalk share this region, so it is bone-cream.
    base(img, HARD, (246, 236, 212), 14, ramp=0.16)

    # The face plate. Cream, with the scowl painted on.
    plate = (238, 222, 190)
    base(img, MUZZLE, plate, 15, ramp=0.14)
    at = front(MUZZLE, 10, 6, 1)
    eyes(img, at, 10, 6)
    fangs(img, (at[0], at[1] + 4), 10, 2, 3, WHITE, upper=False)

    # Brows and the brow ridge. Near-black on purpose.
    base(img, TRIM, (48, 30, 16), 16, ramp=0.20)
    return img


def koopa():
    """Yellow body, green shell, white beak.

    The whole character is one contrast: a soft yellow creature carrying a hard green shell. When
    both were the same colour — which is what these sheets used to be — the shell vanished and a
    Koopa became a green blob with legs. So the shell region gets scutes and a much darker green,
    and the body gets belly ribbing so the two materials cannot be confused even in shadow.

    Face box: the 8x6x7 head at (64,0). Eyes are calm rather than browed; a Koopa is not angry,
    it is oblivious, which is why stomping one feels different from stomping a Goomba.
    """
    img = new_sheet()
    hide = (242, 210, 74)
    base(img, BODY, hide, 21)
    ribs(img, BODY, hide)

    base(img, HEAD, hide, 22, ramp=0.22)
    eyes(img, front(HEAD, 8, 6, 7), 8, 6, angry=False)

    base(img, LIMB, (232, 184, 48), 23, ramp=0.20)

    shell = (62, 168, 62)
    base(img, HARD, shell, 24)
    scutes(img, HARD, shell)
    # A pale rim around the shell edge, which is what makes it read as a rim rather than a stripe.
    rect(img, HARD, 0, 0, 64, 2, shade(shell, 1.35))
    rect(img, HARD, 0, 34, 64, 2, shade(shell, 0.62))

    # Beak.
    base(img, MUZZLE, (247, 231, 168), 25, ramp=0.16)

    # Eyes and shell rim share the trim region, so it is white.
    base(img, TRIM, WHITE, 26, ramp=0.12)
    return img


def thwomp():
    """Cut blue-grey stone with a furious face.

    A Thwomp is the only enemy in the set that is architecture. It should look quarried — courses,
    seams, chipped corners — because the joke of the character is that a piece of the ceiling is
    personally angry with you. Flat grey made it a placeholder cube; masonry makes it a block that
    was built and then woke up.

    Face boxes: the 16x14x8 slab at (0,0), and the 10x7x1 plate at (0,80) in front of it.
    """
    img = new_sheet()
    stone = (107, 122, 153)

    def masonry(region, colour, seed, course=9, brick=16):
        base(img, region, colour, seed)
        ox, oy = region
        w, h = REGION_SIZE[region]
        dr = ImageDraw.Draw(img)
        for row, yy in enumerate(range(oy, oy + h, course)):
            dr.line([(ox, yy), (ox + w - 1, yy)], fill=shade(colour, 0.58))
            dr.line([(ox, yy + 1), (ox + w - 1, yy + 1)], fill=shade(colour, 1.22))
            offset = 0 if row % 2 == 0 else brick // 2
            for xx in range(ox + offset, ox + w, brick):
                dr.line([(xx, yy), (xx, min(oy + h - 1, yy + course - 1))], fill=shade(colour, 0.58))
        # Chipped grit, so the courses are not perfectly machined.
        for i in range(w * h // 30):
            k = _hash(i, seed, 7)
            dr.point((ox + k % w, oy + (k >> 9) % h), fill=shade(colour, 0.80))

    masonry(BODY, stone, 31)
    eyes(img, front(BODY, 16, 14, 8), 16, 14)

    masonry(HEAD, shade(stone, 1.08)[:3], 32)
    masonry(LIMB, shade(stone, 0.88)[:3], 33)

    # Studs, capstone and teeth: a paler dressed stone.
    base(img, HARD, (184, 194, 214), 34, ramp=0.22)

    # Face plate.
    plate = (136, 148, 173)
    base(img, MUZZLE, plate, 35, ramp=0.20)
    at = front(MUZZLE, 10, 7, 1)
    eyes(img, at, 10, 7)
    fangs(img, (at[0], at[1] + 5), 10, 2, 4, WHITE, upper=False)

    # Brow ridge, in the darkest stone there is.
    base(img, TRIM, (58, 66, 88), 36, ramp=0.20)
    return img


def bullet_bill():
    """Black cast iron with a hard white grin.

    Almost no colour at all, which is the point — a Bullet Bill is the only enemy that is a
    manufactured object, and it should look machined next to a set of creatures. So the body gets
    brushed horizontal highlights and a bright seam, and every scrap of contrast is spent on the
    face, because a black shape against a dark course is otherwise just a hole.

    Face box: the 10x10x5 nose cap at (64,0).
    """
    img = new_sheet()
    iron = (43, 43, 51)

    def brushed(region, colour, seed):
        base(img, region, colour, seed, ramp=0.34)
        ox, oy = region
        w, h = REGION_SIZE[region]
        dr = ImageDraw.Draw(img)
        for yy in range(oy + 1, oy + h - 1, 3):
            dr.line([(ox, yy), (ox + w - 1, yy)], fill=shade(colour, 1.22))
        dr.line([(ox, oy + 4), (ox + w - 1, oy + 4)], fill=shade(colour, 1.6))

    brushed(BODY, iron, 41)
    brushed(HEAD, iron, 42)
    at = front(HEAD, 10, 10, 5)
    eyes(img, at, 10, 10)
    # The grin. A Bullet Bill's mouth is a hard white bar, not teeth.
    rect(img, HEAD, at[0] - HEAD[0] + 1, at[1] - HEAD[1] + 7, 8, 2, WHITE)

    brushed(LIMB, (60, 60, 70), 43)
    brushed(HARD, (74, 74, 85), 44)
    brushed(MUZZLE, (52, 52, 62), 45)

    # Rivets, in the only bright material on the sheet.
    base(img, TRIM, WHITE, 46, ramp=0.12)
    return img


def boo():
    """White, not purple. Round, soft, and embarrassed.

    The old sheet had this creature in flat purple across every region, which is most of why it did
    not read as a ghost. It is white — the shading has to do all the shaping, so every region gets
    soft overlapping puffs rather than flat fill, and the trim region is pink because it is the
    tongue and nothing else.

    Face boxes: the 12x10x10 body at (0,0), and the 8x5x1 plate at (0,80) in front of it. Eyes are
    not browed: a Boo is shy, and the shyness is the joke.
    """
    img = new_sheet()
    pale = (242, 242, 247)

    def puffs(region, colour, seed):
        base(img, region, colour, seed, ramp=0.20)
        ox, oy = region
        w, h = REGION_SIZE[region]
        dr = ImageDraw.Draw(img)
        for i in range(16):
            k = _hash(i * 31, seed, 13)
            cx, cy, r = ox + k % w, oy + (k >> 7) % h, 3 + (k >> 14) % 4
            dr.ellipse([cx - r, cy - r, cx + r, cy + r],
                       fill=shade(colour, 1.05 if i % 2 else 0.93))

    puffs(BODY, pale, 51)
    eyes(img, front(BODY, 12, 10, 10), 12, 10, angry=False)

    puffs(HEAD, (246, 246, 250), 52)
    puffs(LIMB, (236, 236, 244), 53)
    puffs(HARD, (228, 228, 238), 54)

    plate = WHITE
    base(img, MUZZLE, plate, 55, ramp=0.14)
    at = front(MUZZLE, 8, 5, 1)
    eyes(img, at, 8, 5, angry=False)

    # Tongue.
    base(img, TRIM, (240, 150, 170), 56, ramp=0.18)
    return img


def lakitu():
    """A Koopa in goggles, sitting on a cloud.

    Two materials that could not be less alike, which is the whole silhouette: the cloud has to be
    soft and shapeless and the rider has to be hard and small. The body region is the cloud, so it
    gets puffs; the goggle region is near-black, because from any distance the goggles are the only
    part of the rider anyone can actually see.

    Face box: the 7x6x6 head at (64,0).
    """
    img = new_sheet()
    cloud = WHITE
    base(img, BODY, cloud, 61, ramp=0.22)
    dr = ImageDraw.Draw(img)
    for i in range(18):
        k = _hash(i * 37, 61, 3)
        cx, cy, r = k % 64, (k >> 7) % 36, 3 + (k >> 14) % 5
        dr.ellipse([cx - r, cy - r, cx + r, cy + r], fill=shade(cloud, 1.0 if i % 2 else 0.90))

    hide = (242, 210, 74)
    base(img, HEAD, hide, 62, ramp=0.22)
    eyes(img, front(HEAD, 7, 6, 6), 7, 6, angry=False)

    base(img, LIMB, hide, 63, ramp=0.20)

    shell = (62, 168, 62)
    base(img, HARD, shell, 64)
    scutes(img, HARD, shell)

    base(img, MUZZLE, (247, 231, 168), 65, ramp=0.16)

    # Goggles.
    base(img, TRIM, (52, 54, 68), 66, ramp=0.22)
    return img


def hammer_bro():
    """A Koopa that went to war.

    Everything that separates him from an ordinary Koopa is hard: a helmet and a shell in dark
    blue against the same yellow hide. Using the Koopa green here would have been the easy choice
    and it would have made two enemies that look identical at a glance and behave nothing alike,
    which is the worst possible outcome in a platformer.

    Face box: the 8x6x7 head at (64,0). Browed, unlike the Koopa — this one is aiming at you.
    """
    img = new_sheet()
    hide = (238, 204, 96)
    base(img, BODY, hide, 71)
    ribs(img, BODY, hide)

    base(img, HEAD, hide, 72, ramp=0.22)
    eyes(img, front(HEAD, 8, 6, 7), 8, 6)

    base(img, LIMB, (228, 186, 60), 73, ramp=0.20)

    armour = (46, 76, 138)
    base(img, HARD, armour, 74)
    scutes(img, HARD, armour)
    rect(img, HARD, 0, 0, 64, 2, shade(armour, 1.45))

    base(img, MUZZLE, (247, 231, 168), 75, ramp=0.16)
    base(img, TRIM, WHITE, 76, ramp=0.12)
    return img


def spiny():
    """Orange hide under a red spiked shell.

    The trim region is bone — it carries the spikes, and the spikes are the entire message this
    creature exists to send: do not jump on this. So they get the palest material on the sheet,
    against the darkest shell, because that contrast has to survive being read in a fifth of a
    second while falling toward it.

    Face boxes: the 14x7x10 body at (0,0), and the 8x4x1 plate at (0,80).
    """
    img = new_sheet()
    hide = (226, 96, 52)
    base(img, BODY, hide, 81)
    scales(img, BODY, hide)
    eyes(img, front(BODY, 14, 7, 10), 14, 7)

    base(img, HEAD, hide, 82)
    scales(img, HEAD, hide)

    base(img, LIMB, (208, 78, 40), 83, ramp=0.20)

    shell = (176, 52, 34)
    base(img, HARD, shell, 84)
    scutes(img, HARD, shell, step=8)

    plate = (240, 214, 176)
    base(img, MUZZLE, plate, 85, ramp=0.16)
    eyes(img, front(MUZZLE, 8, 4, 1), 8, 4)

    # Spikes: bone, with lengthwise striations so they read as horn rather than as cones.
    bone = (250, 244, 230)
    base(img, TRIM, bone, 86, ramp=0.24)
    dr = ImageDraw.Draw(img)
    for xx in range(TRIM[0], TRIM[0] + 64, 3):
        dr.line([(xx, TRIM[1]), (xx, TRIM[1] + 39)], fill=shade(bone, 0.86))
    return img


def buzzy_beetle():
    """A dark blue helmet with feet.

    Nearly the inverse of the Spiny: the shell here is the *safe* part — it is what makes the
    creature immune to fireballs and perfectly stompable — so it reads hard and glossy rather than
    threatening, and the bright material is a cool highlight on the rim instead of spikes.

    Face box: the 10x7x7 head at (64,0).
    """
    img = new_sheet()
    hide = (58, 74, 107)
    base(img, BODY, hide, 91, ramp=0.24)

    base(img, HEAD, (74, 90, 123), 92, ramp=0.22)
    eyes(img, front(HEAD, 10, 7, 7), 10, 7)

    base(img, LIMB, (106, 122, 155), 93, ramp=0.20)

    carapace = (30, 42, 68)
    base(img, HARD, carapace, 94, ramp=0.34)
    scutes(img, HARD, carapace, step=12)
    # A hard specular band near the top: this shell is polished, and that is what says "armoured
    # but harmless" rather than "spiked".
    rect(img, HARD, 0, 3, 64, 2, shade(carapace, 1.9))

    base(img, MUZZLE, (70, 86, 118), 95, ramp=0.18)
    base(img, TRIM, (138, 168, 216), 96, ramp=0.16)
    return img


def piranha_plant():
    """A red spotted maw on a green stem.

    The only enemy here with no eyes at all, so every scrap of character has to come from the
    mouth. White spots on red are what make it a plant rather than a wound, and the interior gets
    the darkest red on the sheet so that when the jaw opens there is a visible depth to it.

    Mouth box: the 12x7x10 maw at (64,0); the 10x8x1 interior plate at (0,80).
    """
    img = new_sheet()
    lip = (214, 56, 66)

    def spotted(region, colour, seed, count=11):
        base(img, region, colour, seed, ramp=0.26)
        ox, oy = region
        w, h = REGION_SIZE[region]
        dr = ImageDraw.Draw(img)
        for i in range(count):
            k = _hash(i * 53, seed, 5)
            cx, cy, r = ox + k % w, oy + (k >> 7) % h, 2 + (k >> 15) % 2
            dr.ellipse([cx - r, cy - r, cx + r, cy + r], fill=WHITE)

    spotted(BODY, lip, 101)
    spotted(HEAD, lip, 102)
    # The maw: dark interior with a ring of teeth top and bottom.
    at = front(HEAD, 12, 7, 10)
    rect(img, HEAD, at[0] - HEAD[0], at[1] - HEAD[1], 12, 7, (110, 22, 30))
    fangs(img, at, 12, 7, 4, WHITE, upper=True)
    fangs(img, at, 12, 7, 4, WHITE, upper=False)

    stem = (66, 158, 62)
    base(img, LIMB, stem, 103)
    ribs(img, LIMB, stem, step=5)

    # Teeth.
    base(img, HARD, WHITE, 104, ramp=0.18)

    interior = (128, 26, 34)
    base(img, MUZZLE, interior, 105, ramp=0.22)
    at = front(MUZZLE, 10, 8, 1)
    fangs(img, at, 10, 8, 4, WHITE, upper=True)
    fangs(img, at, 10, 8, 4, WHITE, upper=False)

    base(img, TRIM, WHITE, 106, ramp=0.14)
    return img


def bowser():
    """Orange hide, green shell, cream horns.

    Three materials that have to stay separate at a glance, because he is the only enemy the player
    ever fights for more than a second. The hide is scaled, the shell is heavily scuted and much
    darker, and the horn material is the palest thing on the sheet — horns, shell spikes, brow and
    wrist cuffs all draw from it, so they read as one set of bone against everything else.

    Face box: the 12x8x10 head at (64,0). Yellow sclera rather than white: it is the cheapest way
    to make one face in the roster look like it belongs to something in charge.
    """
    img = new_sheet()
    hide = (232, 160, 48)
    base(img, BODY, hide, 111)
    scales(img, BODY, hide)

    base(img, HEAD, hide, 112)
    scales(img, HEAD, hide)
    eyes(img, front(HEAD, 12, 8, 10), 12, 8, sclera=(250, 226, 90), pupil=INK)

    base(img, LIMB, (240, 176, 64), 113)
    scales(img, LIMB, (240, 176, 64))

    shell = (74, 154, 58)
    base(img, HARD, shell, 114, ramp=0.34)
    scutes(img, HARD, shell, step=12)
    rect(img, HARD, 0, 0, 64, 2, shade(shell, 1.35))

    # Muzzle, a lighter tan than the hide.
    muzzle = (232, 200, 154)
    base(img, MUZZLE, muzzle, 115, ramp=0.20)

    # Horns, spikes, brow and cuffs: one bone material with lengthwise striations.
    bone = (245, 234, 208)
    base(img, TRIM, bone, 116, ramp=0.26)
    dr = ImageDraw.Draw(img)
    for xx in range(TRIM[0], TRIM[0] + 64, 4):
        dr.line([(xx, TRIM[1]), (xx, TRIM[1] + 39)], fill=shade(bone, 0.84))
    return img


CHARACTERS = {
    "goomba": goomba,
    "koopa": koopa,
    "thwomp": thwomp,
    "bullet_bill": bullet_bill,
    "boo": boo,
    "lakitu": lakitu,
    "hammer_bro": hammer_bro,
    "spiny": spiny,
    "buzzy_beetle": buzzy_beetle,
    "piranha_plant": piranha_plant,
    "bowser": bowser,
}


def main():
    if len(sys.argv) < 2:
        print(__doc__)
        return 1
    target = sys.argv[1]
    os.makedirs(target, exist_ok=True)
    only = sys.argv[2] if len(sys.argv) > 2 else None
    written = 0
    for name, draw in sorted(CHARACTERS.items()):
        if only and name != only:
            continue
        draw().save(os.path.join(target, name + ".png"))
        written += 1
    print("wrote %d enemy sheets to %s" % (written, target))
    return 0


if __name__ == "__main__":
    sys.exit(main())
