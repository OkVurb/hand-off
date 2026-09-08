"""Emit blockstate and model JSON for every ConnectedBlock.

A connected block needs seventeen files it cannot share with anything else: one blockstate mapping
all sixteen boolean combinations onto a model, and sixteen models each pointing at its own texture.
The castle's set was written once by hand, which was fine for one block and is not fine for six --
the mapping from a state to a mask is a contract with ConnectedBlock.mask(), and a contract encoded
by hand in ninety-six lines of JSON is a contract waiting to drift.

So the bit order lives here in one place, and it is the same order the texture generator uses:
bit 0 up, bit 1 down, bit 2 west, bit 3 east.

    python tools/ConnectedAssets.py src/main/resources/assets/planeshift
"""

import io
import json
import os
import sys

# Must match ConnectedBlock.mask() and BlockTextureGen.edged().
BITS = [("up", 1), ("down", 2), ("west", 4), ("east", 8)]

# Kept in step with BlockTextureGen.CONNECTED by the test that compares the two.
BLOCKS = [
    "course_castle_block",
    "course_grass_block",
    "course_dirt_block",
    "course_sand_block",
    "course_sandstone",
    "course_basalt",
    "course_deepstone",
]


def mask_of(values):
    """The four-bit mask for a {name: bool} connection state."""
    m = 0
    for name, bit in BITS:
        if values[name]:
            m |= bit
    return m


def variant_key(values):
    """Minecraft orders blockstate variant keys alphabetically by property name."""
    return ",".join("%s=%s" % (k, str(values[k]).lower()) for k in sorted(values))


def blockstate(name):
    variants = {}
    for combo in range(16):
        values = {n: bool(combo & b) for n, b in BITS}
        variants[variant_key(values)] = {
            "model": "planeshift:block/%s_%d" % (name, mask_of(values))
        }
    return {"variants": variants}


def model(name, mask, has_top):
    """One mask's model.

    Blocks with a distinct top face get ``cube_bottom_top`` rather than ``cube_all``, and that is a
    bug fix rather than a refinement. Grass shipped with ``cube_all``, so every face -- including the
    one you look down on -- drew the *side* art: a dirt tile with a green band along its top edge.
    Standing on a grass block you saw dirt with a stripe near one edge, and the grass top texture
    that has existed all along was drawn by nothing.

    The mask belongs to the sides only. Connection edges are drawn where the material stops in the
    plane the camera sees, and the top face of a block does not have those edges -- it has grass on
    it or it does not.
    """
    texture = "planeshift:block/%s_%d" % (name, mask)
    if not has_top:
        return {"parent": "minecraft:block/cube_all", "textures": {"all": texture}}
    return {
        "parent": "minecraft:block/cube_bottom_top",
        "textures": {
            "top": "planeshift:block/%s_top" % name,
            "bottom": "planeshift:block/%s" % name,
            "side": texture,
        },
    }


def write(path, payload):
    io.open(path, "w", encoding="utf-8").write(
        json.dumps(payload, indent=2, ensure_ascii=False) + "\n")


def main():
    if len(sys.argv) != 2:
        print(__doc__.strip())
        return 2
    root = sys.argv[1]
    states = os.path.join(root, "blockstates")
    models = os.path.join(root, "models", "block")
    os.makedirs(states, exist_ok=True)
    os.makedirs(models, exist_ok=True)

    for name in BLOCKS:
        write(os.path.join(states, name + ".json"), blockstate(name))
        has_top = os.path.isfile(os.path.join(root, "textures", "block", name + "_top.png"))
        for mask in range(16):
            write(os.path.join(models, "%s_%d.json" % (name, mask)), model(name, mask, has_top))
        # The plain model stays: it is the item icon, and the fallback for anything that places
        # this block without going through getStateForPlacement.
        write(os.path.join(models, name + ".json"),
              model(name, None, False) if False else
              ({"parent": "minecraft:block/cube_bottom_top",
                "textures": {"top": "planeshift:block/%s_top" % name,
                             "bottom": "planeshift:block/" + name,
                             "side": "planeshift:block/" + name}}
               if has_top else
               {"parent": "minecraft:block/cube_all",
                "textures": {"all": "planeshift:block/" + name}}))

    print("wrote %d blockstates and %d models"
          % (len(BLOCKS), len(BLOCKS) * 17))
    return 0


if __name__ == "__main__":
    sys.exit(main())
