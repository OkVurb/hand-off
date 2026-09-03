#!/usr/bin/env python3
"""Generate the shaped block models.

Why this exists
---------------
Most of the mod's blocks were ``minecraft:block/cube_all`` or ``minecraft:block/cross`` — a
painted cube or a flat X. That is wrong in two separate ways.

**It lies about collision.** ``TrampolineBlock`` has a 0.6-high shape and ``SpringPadBlock`` a
0.5-high one, yet both drew a full cube: the player stands visibly inside the block. The
checkpoint beacon is a 0.5-wide post drawn as a billboard. A platformer is a game about knowing
exactly where the geometry is, so a model that disagrees with the hitbox is not a cosmetic
problem, it is a control problem.

**It throws away the read.** A Bullet Bill blaster, a warp pipe and a stone pillar are three
different objects that happen to occupy one cube each. Drawn as cubes they are three differently
coloured cubes, and the player has to learn each one by touching it.

So the models here are built as boxes in 16ths of a block, matching each block's ``VoxelShape``
where it has one. The generator exists rather than hand-written JSON because the boxes need
consistent UVs and consistent lighting flags across forty files, and because a shape that has to
change to match a hitbox should be a one-line edit, not a hunt through JSON.

Run:  python tools/BlockModelGen.py src/main/resources/assets/planeshift/models/block
"""

import json
import os
import sys

FACES = ("down", "up", "north", "south", "east", "west")


def box(frm, to, textures, uv=None, shade=True, cullfaces=()):
    """One cuboid.

    ``textures`` maps a face name (or ``"*"``) to a texture variable. UVs default to the box's own
    footprint on each axis, which is what Blockbench calls auto-UV and is almost always what you
    want: it means a box half the width of the block samples half the texture rather than
    squashing the whole image into it.

    Auto-UV is exactly wrong for a texture that is mostly transparent, though — a 4-wide post
    inside a 16px sheet would sample whatever happens to sit at those coordinates, which for the
    checkpoint beacon is empty space, giving an invisible post. Those models pass ``uv``
    explicitly, per face or as ``"*"`` for all of them.
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
        tex = textures.get(face, textures.get("*"))
        if tex is None:
            continue
        if uv and face in uv:
            entry = {"uv": uv[face], "texture": tex}
        elif uv and "*" in uv:
            entry = {"uv": uv["*"], "texture": tex}
        else:
            entry = {"uv": auto[face], "texture": tex}
        if face in cullfaces:
            entry["cullface"] = face
        faces[face] = entry
    element = {"from": list(frm), "to": list(to), "faces": faces}
    if not shade:
        element["shade"] = False
    return element


def model(textures, elements, render_type=None, parent="minecraft:block/block"):
    """Assemble a model.

    Always parented to ``minecraft:block/block``. That parent contributes no geometry — what it
    contributes is the ``display`` block, so the same file also renders correctly as an item in
    the hand and in the inventory. Custom-element models written without it end up as items lying
    flat and half off the slot, which is how the flag pole looked.
    """
    out = {"parent": parent}
    if render_type:
        out["render_type"] = render_type
    out["textures"] = textures
    out["elements"] = elements
    return out


def t(name):
    return "planeshift:block/" + name


# ------------------------------------------------------------------ definitions


def trampoline():
    """Matches TrampolineBlock.SHAPE: 10/16 high, full footprint.

    The legs are inset and unshaded so the pad reads as sprung off the floor rather than as a
    slab, which is the only visual cue that stepping on it will launch you.
    """
    tex = {"particle": t("trampoline"), "top": t("trampoline_top"), "side": t("trampoline")}
    return model(tex, [
        box((0, 6, 0), (16, 10, 16),
            {"up": "#top", "down": "#side", "*": "#side"}),
        box((1, 0, 1), (4, 6, 4), {"*": "#side"}),
        box((12, 0, 1), (15, 6, 4), {"*": "#side"}),
        box((1, 0, 12), (4, 6, 15), {"*": "#side"}),
        box((12, 0, 12), (15, 6, 15), {"*": "#side"}),
    ])


def spring_pad():
    """Matches SpringPadBlock.SHAPE: 8/16 high. Drawn as a coil, three tapering rings."""
    tex = {"particle": t("spring_pad"), "top": t("spring_pad_top"), "side": t("spring_pad")}
    return model(tex, [
        box((1, 0, 1), (15, 2, 15), {"up": "#side", "down": "#side", "*": "#side"},
            cullfaces=("down",)),
        box((3, 2, 3), (13, 4, 13), {"*": "#side", "up": "#side"}),
        box((2, 4, 2), (14, 6, 14), {"*": "#side", "up": "#side"}),
        box((1, 6, 1), (15, 8, 15), {"up": "#top", "down": "#side", "*": "#side"}),
    ])


def muncher():
    """A bulb with teeth. Full-height collision, so the model fills the cube minus a 1px margin."""
    tex = {"particle": t("muncher"), "all": t("muncher")}
    elements = [box((2, 0, 2), (14, 11, 14), {"*": "#all"})]
    # Four fangs around the rim, alternating height so the silhouette is jagged, not a crown.
    # Explicit UV: the fangs live at x 4-12, y 4-9 in the sheet. Auto-UV would sample the empty
    # top corners of the texture and the teeth would simply not be there.
    for x, z, h in ((2, 2, 16), (11, 2, 14), (2, 11, 14), (11, 11, 16)):
        elements.append(box((x, 11, z), (x + 3, h, z + 3), {"*": "#all"},
                            uv={"*": [4, 4, 8, 9]}))
    return model(tex, elements, "minecraft:cutout")


def cannon():
    """Blaster tube. The muzzle is the north face because the blockstate rotates from north."""
    tex = {"particle": t("bullet_bill_cannon"),
           "side": t("bullet_bill_cannon"),
           "front": t("bullet_bill_cannon_front")}
    return model(tex, [
        box((2, 0, 0), (14, 14, 3), {"north": "#front", "*": "#side"}, cullfaces=("north",)),
        box((1, 0, 3), (15, 16, 16), {"*": "#side"}, cullfaces=("south", "down", "up")),
    ])


def beacon(lit):
    """Matches CheckpointBeaconBlock.SHAPE: a 0.25-0.75 post, full height, with a flag."""
    name = "checkpoint_beacon_lit" if lit else "checkpoint_beacon"
    tex = {"particle": t(name), "post": t(name), "flag": t(name)}
    return model(tex, [
        # The post is drawn at x 6-10 in the sheet; sample exactly that on every side.
        box((4, 0, 4), (12, 16, 12), {"*": "#post"},
            uv={"*": [6, 0, 10, 16], "up": [6, 6, 10, 10], "down": [6, 6, 10, 10]}),
        # Flag panel, one pixel thick, hung off the +X side at head height.
        box((12, 9, 7), (16, 15, 8), {"*": "#flag"}, shade=False,
            uv={"*": [10, 2, 15, 8]}),
    ], "minecraft:cutout")


def ring(used):
    """A coin ring drawn as a square annulus in the XY plane.

    Four bars rather than a billboard, because the player passes *through* it: a flat X would
    disappear edge-on at exactly the moment they line up to fly through it.
    """
    name = "coin_ring_block_used" if used else "coin_ring_block"
    tex = {"particle": t(name), "all": t(name)}
    return model(tex, [
        box((2, 1, 7), (14, 3, 9), {"*": "#all"}),
        box((2, 13, 7), (14, 15, 9), {"*": "#all"}),
        box((2, 3, 7), (4, 13, 9), {"*": "#all"}),
        box((12, 3, 7), (14, 13, 9), {"*": "#all"}),
    ], "minecraft:cutout")


def axe():
    """The axe on its block: a stone plinth with the axe standing in it.

    The axe itself is one full-texture panel rather than a shaft box plus a head box. Splitting it
    up sounds more three-dimensional, but each sub-box then has to be UV'd by hand onto its own
    part of the sheet, and any drift between the drawing and those numbers shows up as a floating
    axe head. One panel cannot drift.
    """
    tex = {"particle": t("axe_block"), "all": t("axe_block"), "stone": t("course_hard_block")}
    return model(tex, [
        box((3, 0, 3), (13, 3, 13), {"*": "#stone"}, cullfaces=("down",)),
        box((0, 0, 7), (16, 16, 9), {"*": "#all"}, shade=False,
            uv={"*": [0, 0, 16, 16]}),
    ], "minecraft:cutout")


def warp_pipe():
    """A pipe: a wide rim on a narrower barrel. The rim is the whole visual idea."""
    tex = {"particle": t("warp_pipe"), "side": t("warp_pipe"), "top": t("warp_pipe_top")}
    return model(tex, [
        box((2, 0, 2), (14, 12, 14), {"*": "#side", "up": "#top", "down": "#top"},
            cullfaces=("down",)),
        box((0, 12, 0), (16, 16, 16), {"*": "#side", "up": "#top", "down": "#side"},
            cullfaces=("up", "north", "south", "east", "west")),
    ])


def pillar():
    """Column with a cap and a base, so a stack reads as one shaft with ends."""
    tex = {"particle": t("course_pillar"), "side": t("course_pillar"),
           "end": t("course_pillar_top")}
    return model(tex, [
        box((0, 0, 0), (16, 2, 16), {"*": "#side", "up": "#end", "down": "#end"},
            cullfaces=("down",)),
        box((2, 2, 2), (14, 14, 14), {"*": "#side", "up": "#end", "down": "#end"}),
        box((0, 14, 0), (16, 16, 16), {"*": "#side", "up": "#end", "down": "#end"},
            cullfaces=("up",)),
    ])


def lattice():
    """A thin trellis panel: something to see through, which is the point of a lattice."""
    tex = {"particle": t("course_lattice"), "all": t("course_lattice")}
    return model(tex, [box((0, 0, 7), (16, 16, 9), {"*": "#all"})], "minecraft:cutout")


def banner():
    """Hanging cloth, one pixel thick, drawn unshaded so both sides read the same."""
    tex = {"particle": t("course_banner"), "all": t("course_banner")}
    return model(tex, [
        box((0, 0, 7), (16, 16, 8), {"*": "#all"}, shade=False),
        box((0, 14, 6), (16, 16, 9), {"*": "#all"}),
    ], "minecraft:cutout")


def p_switch(pressed):
    """A button. Pressed is genuinely lower, so its state is visible from across the room."""
    name = "p_switch_pressed" if pressed else "p_switch"
    tex = {"particle": t(name), "all": t(name)}
    height = 4 if pressed else 11
    return model(tex, [
        box((1, 0, 1), (15, 2, 15), {"*": "#all"}, cullfaces=("down",)),
        box((3, 2, 3), (13, height, 13), {"*": "#all"}),
    ])


def on_off_switch(powered):
    """The exclamation switch: a small block on a stubby post."""
    name = "on_off_switch_powered" if powered else "on_off_switch"
    tex = {"particle": t(name), "all": t(name)}
    return model(tex, [
        box((6, 0, 6), (10, 5, 10), {"*": "#all"}, cullfaces=("down",)),
        box((3, 5, 3), (13, 14, 13), {"*": "#all"}),
    ])


def spike_block():
    """Four spikes on a plinth, each a stack of shrinking boxes.

    Stepped rather than tapered because Minecraft models have no tapering; the steps are what
    make it read as sharp instead of as four posts.
    """
    tex = {"particle": t("spike_block"), "all": t("spike_block")}
    elements = [box((0, 0, 0), (16, 4, 16), {"*": "#all"},
                    cullfaces=("down", "north", "south", "east", "west"))]
    for x, z in ((2, 2), (10, 2), (2, 10), (10, 10)):
        elements.append(box((x, 4, z), (x + 4, 8, z + 4), {"*": "#all"}))
        elements.append(box((x + 1, 8, z + 1), (x + 3, 12, z + 3), {"*": "#all"}))
        elements.append(box((x + 1, 12, z + 1), (x + 2, 15, z + 2), {"*": "#all"}))
    return model(tex, elements, "minecraft:cutout")


def flag_pole_top():
    """The pennant at the top of the pole.

    Was pointing ``#flag`` at ``flag_pole`` — the pole texture — so the 'waving checkered pennant'
    was a grey rectangle. It now has a texture of its own.
    """
    tex = {"particle": t("flag_pole"), "pole": t("flag_pole"), "flag": t("flag_pole_flag"),
           "finial": t("flag_pole_base")}
    return model(tex, [
        box((7, 0, 7), (9, 13, 9), {"*": "#pole"}),
        box((6, 13, 6), (10, 16, 10), {"*": "#finial"}),
        box((9, 5, 7.5), (16, 12, 8.5), {"*": "#flag"}, shade=False),
    ], "minecraft:cutout")


def shift_gate():
    """Matches ShiftGateBlock.SHAPE_NS/EW: a 2/16-thick panel, not a cube.

    The third model that disagreed with its own hitbox (see R9). A shift gate is a doorway the
    player walks through to change plane; drawn as a solid cube it looks like a wall you are
    somehow passing into, which is the opposite of the thing it is trying to communicate.
    """
    tex = {"particle": t("shift_gate"), "all": t("shift_gate")}
    return model(tex, [
        # The frame reads first, so it gets the outer band and the panel sits recessed behind it.
        box((0, 0, 6), (16, 2, 10), {"*": "#all"}),
        box((0, 14, 6), (16, 16, 10), {"*": "#all"}),
        box((0, 2, 6), (2, 14, 10), {"*": "#all"}),
        box((14, 2, 6), (16, 14, 10), {"*": "#all"}),
        box((2, 2, 7), (14, 14, 9), {"*": "#all"}, shade=False),
    ], "minecraft:cutout")


def axe_block_taken():
    """The empty plinth, after the axe has been taken.

    Was a cross model still pointing at an axe-shaped texture, so a taken axe looked exactly like
    an untaken one. The state exists precisely so the player can see the bridge is already
    collapsing; drawing it identically throws that away.
    """
    tex = {"particle": t("axe_block_taken"), "stone": t("course_hard_block"),
           "mark": t("axe_block_taken")}
    return model(tex, [
        box((3, 0, 3), (13, 3, 13), {"*": "#stone", "up": "#mark"}, cullfaces=("down",)),
    ])


BUILDERS = {
    "trampoline": trampoline,
    "spring_pad": spring_pad,
    "muncher": muncher,
    "bullet_bill_cannon": cannon,
    "checkpoint_beacon": lambda: beacon(False),
    "checkpoint_beacon_lit": lambda: beacon(True),
    "coin_ring_block": lambda: ring(False),
    "coin_ring_block_used": lambda: ring(True),
    "axe_block": axe,
    "warp_pipe": warp_pipe,
    "course_pillar": pillar,
    "course_lattice": lattice,
    "course_banner": banner,
    "p_switch": lambda: p_switch(False),
    "p_switch_pressed": lambda: p_switch(True),
    "on_off_switch": lambda: on_off_switch(False),
    "on_off_switch_powered": lambda: on_off_switch(True),
    "spike_block": spike_block,
    "flag_pole_top": flag_pole_top,
    "shift_gate": shift_gate,
    "axe_block_taken": axe_block_taken,
}


def main():
    if len(sys.argv) < 2:
        print(__doc__)
        return 1
    target = sys.argv[1]
    os.makedirs(target, exist_ok=True)
    for name, builder in sorted(BUILDERS.items()):
        path = os.path.join(target, name + ".json")
        with open(path, "w", encoding="utf-8") as handle:
            json.dump(builder(), handle, indent=2)
            handle.write("\n")
    print("wrote %d block models to %s" % (len(BUILDERS), target))
    return 0


if __name__ == "__main__":
    sys.exit(main())
