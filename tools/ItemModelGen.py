#!/usr/bin/env python3
"""Give the power-ups and collectibles real geometry instead of flat sprites.

Why this exists
---------------
`coin.json` was already hand-built as a 3D disc, and its comment says exactly why:

    "Coins spawn as world items in a side-on course, so a billboard vanishes edge-on every time
     the camera is square to the lane - which is most of the time."

That reasoning applies to every single power-up, and none of the others had been done. A mushroom
lying on a platform in a 2.5D course is seen from a camera pointed almost straight down the Z
axis; `minecraft:item/generated` builds a flat quad facing +Z with extruded edges, so from that
angle the player sees a one-pixel line. The item is *there* and is invisible.

So this generator does for the rest of the roster what the coin got by hand.

Two constructions, chosen by what the silhouette needs:

**Stacked boxes** for anything with real volume — the mushrooms. Three boxes of decreasing width
make a dome and a fourth makes the stem, each box textured from the band of the sprite that sits
at the same height. That works because the sprite is drawn side-on and the model is built side-on,
so a box spanning model rows 7-11 samples sprite rows 5-9 and the paint lands where it belongs.
Auto-UV gives that for free; the trick is only to build the boxes at the heights the art already
uses.

**Crossed panels** for anything essentially flat — flowers, leaves, suits, gems, the star. Two
perpendicular one-pixel panels, which is the same idea as `block/cross`. It is not sculpture, but
it is never edge-on, and for a 16x16 icon that is the whole requirement.

Spawn eggs stay flat on purpose. They are creative-mode inventory items that never lie on the
ground in a course, so they have nothing to gain and would only cost atlas work.

Run:  python tools/ItemModelGen.py src/main/resources/assets/planeshift/models/item
"""

import json
import os
import sys

FACES = ("down", "up", "north", "south", "east", "west")


def box(frm, to, texture, uv=None, shade=True):
    """One cuboid, auto-UV'd against its own footprint.

    Auto-UV is what makes the stacked construction work: a box at model height 7-11 samples sprite
    rows 5-9 without anyone having to write those numbers down, so moving a box moves the paint
    with it.
    """
    x0, y0, z0 = frm
    x1, y1, z1 = to
    auto = {
        "down": [x0, 16 - z1, x1, 16 - z0],
        "up": [x0, z0, x1, z1],
        "north": [16 - x1, 16 - y1, 16 - x0, 16 - y0],
        "south": [x0, 16 - y1, x1, 16 - y0],
        "west": [z0, 16 - y1, z1, 16 - y0],
        "east": [16 - z1, 16 - y1, 16 - z0, 16 - y0],
    }
    faces = {}
    for face in FACES:
        faces[face] = {"uv": (uv or auto)[face] if isinstance(uv, dict) else (uv or auto[face]),
                       "texture": texture}
    element = {"from": list(frm), "to": list(to), "faces": faces}
    if not shade:
        element["shade"] = False
    return element


def panel(axis, texture, low=0, high=16, thickness=1):
    """A flat panel through the middle of the item, facing one axis."""
    half = 8 - thickness / 2.0
    other = 8 + thickness / 2.0
    if axis == "z":
        frm, to = (0, low, half), (16, high, other)
    else:
        frm, to = (half, low, 0), (other, high, 16)
    full = [0, 16 - high, 16, 16 - low]
    faces = {}
    for face in FACES:
        faces[face] = {"uv": full, "texture": texture}
    return {"from": list(frm), "to": list(to), "faces": faces, "shade": False}


# Display transforms, matched to the hand-built coin so the whole item set sits consistently.
DISPLAY_UPRIGHT = {
    "gui": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.6, 0.6, 0.6]},
    "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
    "thirdperson_righthand": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.55, 0.55, 0.55]},
    "thirdperson_lefthand": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.55, 0.55, 0.55]},
    "firstperson_righthand": {"rotation": [0, 90, 0], "translation": [0, 2, 0], "scale": [0.6, 0.6, 0.6]},
    "firstperson_lefthand": {"rotation": [0, 90, 0], "translation": [0, 2, 0], "scale": [0.6, 0.6, 0.6]},
}


def model(name, elements):
    tex = "planeshift:item/" + name
    return {
        "render_type": "minecraft:cutout",
        "textures": {"layer0": tex, "particle": tex},
        "elements": elements,
        "display": DISPLAY_UPRIGHT,
        "gui_light": "front",
    }


def mushroom(name):
    """Dome plus stem, built from the bands the sprite is already drawn in.

    ``ItemTextureGen.mushroom`` draws the cap as an ellipse from sprite rows 2-11 and the stem as a
    rectangle from rows 9-14. The boxes below sit at the model heights those rows map to, so the
    cap paint lands on the cap and the stem paint on the stem with no hand-authored UVs at all.
    """
    tex = "#layer0"
    return model(name, [
        # Cap, three courses, narrowing upward so the silhouette domes instead of being a brick.
        box((4, 11, 4), (12, 14, 12), tex),
        box((2, 8, 2), (14, 11, 14), tex),
        box((1, 5, 1), (15, 8, 15), tex),
        # Stem, clearly narrower than the cap — that contrast is what says "mushroom" at 16px.
        box((5, 1, 5), (11, 5, 11), tex),
    ])


def disc(name):
    """A struck coin: a thin cylinder approximated by two stacked slabs, as in coin.json."""
    tex = "#layer0"
    return model(name, [
        box((5.5, 1, 7.5), (10.5, 14, 8.5), tex),
        box((4.5, 3, 7.5), (11.5, 12, 8.5), tex),
    ])


def crossed(name, low=0, high=16):
    """Two perpendicular panels. Never edge-on, which is the entire point."""
    tex = "#layer0"
    return model(name, [panel("z", tex, low, high), panel("x", tex, low, high)])


def flower(name):
    """Crossed petals on a solid stem, so it reads as a plant rather than as a decal."""
    tex = "#layer0"
    return model(name, [
        panel("z", tex, 4, 16),
        panel("x", tex, 4, 16),
        box((7, 0, 7), (9, 5, 9), tex),
    ])


MUSHROOMS = ["super_mushroom", "mega_mushroom", "mini_mushroom", "poison_mushroom",
             "propeller_mushroom", "three_up", "five_up"]
FLOWERS = ["fire_flower", "ice_flower", "cloud_flower"]
DISCS = ["star_coin"]
CROSSED = ["star_power", "leaf", "acorn", "tanooki", "cat_suit",
           "barrier_charm", "ember_charm", "gale_charm", "magnet_charm",
           "extra_pip", "hammer", "boomerang"]


def build():
    out = {}
    for name in MUSHROOMS:
        out[name] = mushroom(name)
    for name in FLOWERS:
        out[name] = flower(name)
    for name in DISCS:
        out[name] = disc(name)
    for name in CROSSED:
        out[name] = crossed(name)
    return out


def main():
    if len(sys.argv) < 2:
        print(__doc__)
        return 1
    target = sys.argv[1]
    made = build()
    missing = [n for n in made if not os.path.exists(os.path.join(target, n + ".json"))]
    if missing:
        # A model for an item that does not exist is dead weight that still loads; a typo here
        # would silently ship one.
        print("WARNING: no existing model for %s — check the item names" % ", ".join(missing))
    for name, data in sorted(made.items()):
        with open(os.path.join(target, name + ".json"), "w", encoding="utf-8") as handle:
            json.dump(data, handle, indent=2)
            handle.write("\n")
    print("wrote %d item models to %s" % (len(made), target))
    return 0


if __name__ == "__main__":
    sys.exit(main())
