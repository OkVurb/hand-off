#!/usr/bin/env python3
"""Generate the 16x16 item textures.

Why this exists
---------------
Every item texture was, at one point, the same 16x16 banana with a couple of pixels altered per
file. That defeated ``checkTextureAssets`` — which only rejects *byte-identical* files — while
leaving the fire flower, the ice flower, the coin, the star and all twelve spawn eggs visually
indistinguishable in the hotbar. An icon that does not identify its item is worse than an obvious
placeholder, because a placeholder at least admits what it is.

The rule here is that an item must be identifiable by **silhouette first, colour second**. Someone
playing at GUI scale 2 sees roughly a centimetre of screen; they read the shape. So the generator
draws real shapes per category — a domed mushroom, a five-petal flower, a struck star, an egg, a
faceted gem — and only then colours them. Two mushrooms differ by palette; a mushroom and a flower
differ by outline, which survives being small.

Everything is drawn from geometry rather than sampled, so it stays original work and regenerates
identically.

Run:  python tools/ItemTextureGen.py src/main/resources/assets/planeshift/textures/item
"""

import os
import sys
from PIL import Image, ImageDraw

S = 16
CLEAR = (0, 0, 0, 0)
OUTLINE = (26, 20, 34, 255)


def shade(c, f):
    """Scale a colour toward black (f<1) or white (f>1), keeping alpha."""
    r, g, b = c
    if f <= 1.0:
        return (int(r * f), int(g * f), int(b * f), 255)
    t = f - 1.0
    return (int(r + (255 - r) * t), int(g + (255 - g) * t), int(b + (255 - b) * t), 255)


def new():
    return Image.new("RGBA", (S, S), CLEAR)


def mushroom(cap, stem=(245, 232, 205), spots=(255, 255, 255), spotted=True):
    """Domed cap on a short stem. The most common silhouette, so it must be crisp."""
    img = new()
    d = ImageDraw.Draw(img)
    d.ellipse([1, 2, 14, 11], fill=cap, outline=OUTLINE)
    # Stem, narrower than the cap so the two shapes stay distinct at 16px.
    d.rectangle([5, 9, 10, 14], fill=stem, outline=OUTLINE)
    d.rectangle([6, 10, 9, 13], fill=shade(stem[:3], 1.05))
    if spotted:
        d.ellipse([3, 4, 6, 7], fill=spots)
        d.ellipse([9, 3, 12, 6], fill=spots)
        d.ellipse([6, 6, 8, 8], fill=spots)
    # Highlight along the top-left of the dome.
    d.arc([2, 3, 10, 9], 170, 250, fill=shade(cap, 1.35))
    return img


def flower(petal, centre):
    """Five petals around a core — reads as a flower even as a blur."""
    img = new()
    d = ImageDraw.Draw(img)
    for cx, cy in [(8, 3), (3, 7), (13, 7), (5, 12), (11, 12)]:
        d.ellipse([cx - 3, cy - 3, cx + 3, cy + 3], fill=petal, outline=OUTLINE)
    d.ellipse([5, 5, 10, 10], fill=centre, outline=OUTLINE)
    d.ellipse([6, 6, 8, 8], fill=shade(centre, 1.4))
    d.line([8, 12, 8, 15], fill=(60, 130, 60, 255), width=2)
    return img


def coin(face, rim):
    """A struck disc: rim, face, and an engraved bar."""
    img = new()
    d = ImageDraw.Draw(img)
    d.ellipse([2, 1, 13, 14], fill=rim, outline=OUTLINE)
    d.ellipse([4, 3, 11, 12], fill=face, outline=shade(rim, 0.8))
    d.rectangle([7, 5, 8, 10], fill=shade(rim, 0.75))
    d.arc([3, 2, 12, 13], 150, 230, fill=shade(face, 1.4))
    return img


def star(body):
    """Five-point star, drawn as a polygon so the points stay sharp."""
    img = new()
    d = ImageDraw.Draw(img)
    pts = [(8, 0), (10, 5), (15, 5), (11, 9), (13, 15), (8, 11), (3, 15), (5, 9), (1, 5), (6, 5)]
    d.polygon(pts, fill=body, outline=OUTLINE)
    d.polygon([(8, 3), (9, 6), (12, 6), (10, 8), (8, 7), (6, 8), (4, 6), (7, 6)],
              fill=shade(body, 1.35))
    return img


def egg(base, spot):
    """Spawn egg: tall oval with two spot rows. Colour is the only per-mob difference,
    which is correct — they are all the same kind of object."""
    img = new()
    d = ImageDraw.Draw(img)
    d.ellipse([3, 1, 12, 14], fill=base, outline=OUTLINE)
    for cx, cy in [(6, 4), (10, 6), (5, 9), (9, 11), (7, 7)]:
        d.ellipse([cx - 1, cy - 1, cx + 1, cy + 1], fill=spot)
    d.arc([4, 2, 11, 12], 165, 240, fill=shade(base, 1.35))
    return img


def leaf(body, vein):
    img = new()
    d = ImageDraw.Draw(img)
    d.polygon([(13, 2), (14, 8), (9, 14), (3, 13), (2, 7), (7, 2)], fill=body, outline=OUTLINE)
    d.line([12, 3, 4, 12], fill=vein, width=1)
    for a, b in [((10, 4), (8, 4)), ((9, 6), (6, 6)), ((7, 8), (5, 9))]:
        d.line([a, b], fill=vein)
    return img


def gem(body):
    """Charm: a faceted stone. Angular, so it never reads as a mushroom or a coin."""
    img = new()
    d = ImageDraw.Draw(img)
    d.polygon([(8, 1), (14, 6), (11, 14), (5, 14), (2, 6)], fill=body, outline=OUTLINE)
    d.polygon([(8, 1), (11, 6), (5, 6)], fill=shade(body, 1.4))
    d.polygon([(5, 6), (11, 6), (8, 13)], fill=shade(body, 0.75))
    return img


def heart(body):
    img = new()
    d = ImageDraw.Draw(img)
    d.ellipse([2, 3, 8, 9], fill=body, outline=OUTLINE)
    d.ellipse([7, 3, 13, 9], fill=body, outline=OUTLINE)
    d.polygon([(2, 7), (13, 7), (8, 14)], fill=body, outline=OUTLINE)
    d.ellipse([4, 5, 6, 7], fill=shade(body, 1.4))
    return img


def hammer_icon():
    img = new()
    d = ImageDraw.Draw(img)
    d.line([4, 13, 10, 5], fill=(126, 84, 46, 255), width=3)
    d.line([4, 13, 10, 5], fill=(158, 110, 62, 255), width=1)
    d.polygon([(8, 2), (14, 5), (12, 9), (6, 6)], fill=(168, 176, 186, 255), outline=OUTLINE)
    d.polygon([(9, 3), (13, 5), (12, 7)], fill=(214, 222, 230, 255))
    return img


def boomerang_icon():
    img = new()
    d = ImageDraw.Draw(img)
    d.polygon([(2, 3), (7, 8), (13, 3), (14, 6), (8, 13), (1, 6)],
              fill=(236, 198, 74, 255), outline=OUTLINE)
    d.line([3, 5, 7, 9], fill=(255, 236, 160, 255), width=1)
    return img


def suit(body, trim):
    """Tanooki suit: a garment outline, distinct from every creature and pickup."""
    img = new()
    d = ImageDraw.Draw(img)
    d.rectangle([5, 4, 10, 12], fill=body, outline=OUTLINE)
    d.rectangle([2, 5, 5, 10], fill=body, outline=OUTLINE)
    d.rectangle([10, 5, 13, 10], fill=body, outline=OUTLINE)
    d.ellipse([6, 1, 9, 5], fill=body, outline=OUTLINE)
    d.rectangle([6, 12, 7, 15], fill=trim, outline=OUTLINE)
    d.rectangle([8, 12, 9, 15], fill=trim, outline=OUTLINE)
    d.ellipse([6, 6, 9, 9], fill=trim)
    return img


def block_icon(face, band):
    """Item form of a block: an isometric-ish cube face."""
    img = new()
    d = ImageDraw.Draw(img)
    d.rectangle([2, 2, 13, 13], fill=face, outline=OUTLINE)
    d.rectangle([4, 4, 11, 11], outline=band)
    d.line([3, 3, 3, 12], fill=shade(face, 1.3))
    d.line([3, 3, 12, 3], fill=shade(face, 1.3))
    return img


def numbered(base_img, digits, colour=(40, 30, 20, 255)):
    """Stamps 3 or 5 onto a 1-Up style mushroom so the multi-life items differ at a glance."""
    d = ImageDraw.Draw(base_img)
    if digits == "3":
        d.line([6, 4, 9, 4], fill=colour)
        d.line([9, 4, 8, 6], fill=colour)
        d.line([7, 6, 9, 6], fill=colour)
        d.line([9, 6, 9, 8], fill=colour)
        d.line([6, 8, 9, 8], fill=colour)
    else:
        d.line([9, 4, 6, 4], fill=colour)
        d.line([6, 4, 6, 6], fill=colour)
        d.line([6, 6, 9, 6], fill=colour)
        d.line([9, 6, 9, 8], fill=colour)
        d.line([6, 8, 9, 8], fill=colour)
    return base_img


ITEMS = {
    # Mushrooms — palette is the only difference, which is right: they are one family.
    "super_mushroom":     lambda: mushroom((214, 62, 52)),
    "mega_mushroom":      lambda: mushroom((222, 100, 40), spots=(255, 244, 190)),
    "mini_mushroom":      lambda: mushroom((80, 176, 226), spots=(232, 250, 255)),
    "poison_mushroom":    lambda: mushroom((92, 56, 116), stem=(140, 132, 150),
                                           spots=(180, 240, 130)),
    "propeller_mushroom": lambda: mushroom((236, 186, 60), spots=(255, 250, 220)),
    # 3-Up and 5-Up differ by digit *and* palette. The digit alone is two pixels at hotbar size,
    # which is not a difference the player can act on.
    "three_up":           lambda: numbered(mushroom((70, 190, 96), spots=(240, 255, 240)), "3"),
    "five_up":            lambda: numbered(mushroom((228, 176, 52), spots=(255, 248, 214)), "5"),

    # Flowers
    "fire_flower":  lambda: flower((236, 92, 48), (250, 226, 120)),
    "ice_flower":   lambda: flower((150, 218, 246), (240, 252, 255)),
    "cloud_flower": lambda: flower((246, 250, 255), (196, 226, 244)),

    # Currency and the star
    "coin":       lambda: coin((252, 214, 88), (206, 152, 34)),
    "star_coin":  lambda: coin((250, 236, 150), (200, 168, 40)),
    "star_power": lambda: star((252, 232, 96)),

    # Nature
    "leaf":  lambda: leaf((208, 138, 54), (250, 216, 150)),
    "acorn": lambda: leaf((176, 118, 66), (232, 190, 140)),

    # Tools and garments
    "hammer":    hammer_icon,
    "boomerang": boomerang_icon,
    "tanooki":   lambda: suit((176, 118, 60), (240, 224, 190)),

    # Charms — angular gems, one hue each
    "barrier_charm": lambda: gem((120, 190, 236)),
    "ember_charm":   lambda: gem((232, 108, 56)),
    "gale_charm":    lambda: gem((160, 232, 196)),
    "magnet_charm":  lambda: gem((216, 96, 140)),

    # Health
    "extra_pip": lambda: heart((228, 84, 92)),

    # The only objective item in the game: it does nothing on its own and is worth having entirely
    # because of somewhere else in the level.
    "course_key": lambda: key_icon(),

    # Block item
    "hidden_question_block": lambda: block_icon((198, 158, 74), (120, 88, 32)),
}

# Spawn eggs: one shared silhouette, a distinct palette per mob taken from that mob's own colours.
EGGS = {
    "goomba_spawn_egg":          ((150, 96, 54), (86, 52, 28)),
    "koopa_spawn_egg":           ((88, 176, 76), (240, 216, 96)),
    "spiny_spawn_egg":           ((214, 92, 62), (250, 236, 190)),
    "buzzy_beetle_spawn_egg":    ((70, 74, 92), (150, 158, 178)),
    "boo_spawn_egg":             ((236, 238, 248), (150, 156, 180)),
    "lakitu_spawn_egg":          ((242, 246, 250), (110, 190, 236)),
    "hammer_bro_spawn_egg":      ((66, 148, 82), (232, 232, 232)),
    "thwomp_spawn_egg":          ((92, 100, 128), (52, 58, 76)),
    "piranha_plant_spawn_egg":   ((214, 66, 84), (250, 250, 250)),
    "bullet_bill_spawn_egg":     ((44, 44, 52), (198, 202, 212)),
    "bowser_spawn_egg":          ((226, 168, 56), (108, 158, 62)),
    "toad_spawn_egg":            ((250, 250, 250), (222, 68, 68)),
    "moving_platform_spawn_egg": ((150, 122, 92), (208, 186, 150)),
}
for name, (base, spot) in EGGS.items():
    ITEMS[name] = (lambda b=base, s=spot: egg(b, s))


def key_icon():
    """A key: bow, shaft, two wards."""
    img = new()
    d = ImageDraw.Draw(img)
    gold = (238, 196, 62)
    d.ellipse([3, 2, 10, 9], outline=shade(gold, 1.0), width=2)
    d.ellipse([5, 4, 8, 7], fill=CLEAR)
    d.rectangle([6, 8, 7, 14], fill=shade(gold, 1.0))
    d.rectangle([8, 10, 10, 11], fill=shade(gold, 1.0))
    d.rectangle([8, 13, 10, 14], fill=shade(gold, 1.0))
    d.line([(3, 3), (5, 2)], fill=shade(gold, 1.45))
    return img


def main():
    out = sys.argv[1] if len(sys.argv) > 1 else "src/main/resources/assets/planeshift/textures/item"
    os.makedirs(out, exist_ok=True)
    existing = {f[:-4] for f in os.listdir(out) if f.endswith(".png")}

    written = 0
    for name, make in sorted(ITEMS.items()):
        make().save(os.path.join(out, name + ".png"))
        written += 1

    missing = existing - set(ITEMS)
    extra = set(ITEMS) - existing
    print(f"wrote {written} item textures to {out}")
    if missing:
        print("  WARNING: existing textures this generator does not cover:", sorted(missing))
    if extra:
        print("  note: generated textures with no prior file:", sorted(extra))


if __name__ == "__main__":
    main()
