# -*- coding: utf-8 -*-
"""Add the pipe colour set by recolouring the existing green pipe art.

The side texture predates BlockTextureGen and is not generated, so redrawing it would have meant
inventing a second pipe style and hoping it matched. Recolouring keeps the shading, the highlight
and the rim exactly as they are and changes only the hue -- which is the whole finding: the sheets
show pipes of several colours reading as different objects, not as different materials.

Green is left untouched, so nothing that already looks right changes.
"""
import io
import json
import os

from PIL import Image

ROOT = 'C:/Dev/PlaneShift/'
TEX = ROOT + 'src/main/resources/assets/planeshift/textures/block/'
A = ROOT + 'src/main/resources/assets/planeshift/'

# Hue, in the order the blockstate property will list them. Green is the existing art and is not
# regenerated; the rest are recoloured from it.
COLOURS = {
    'yellow': (226, 190, 52),
    'blue': (62, 122, 206),
    'red': (204, 66, 62),
    'magenta': (198, 74, 158),
}


def luminance(px):
    r, g, b = px[:3]
    return (r * 30 + g * 59 + b * 11) // 100


def recolour(img, target):
    """Map each pixel's brightness onto a ramp built from the target hue.

    Brightness carries all the form in these textures -- the rim, the highlight arc, the dark
    mouth -- so preserving it and replacing only the hue keeps every one of those reads intact.
    The ramp runs from a heavily darkened target to a heavily lightened one, matching the range
    the green original already uses.
    """
    out = img.convert('RGBA').copy()
    px = out.load()
    w, h = out.size
    tr, tg, tb = target
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            t = luminance((r, g, b)) / 255.0
            # Below mid, blend toward black; above, toward white. Same curve either side so the
            # midtone lands exactly on the target colour.
            if t <= 0.5:
                k = t * 2.0
                nr, ng, nb = tr * k, tg * k, tb * k
            else:
                k = (t - 0.5) * 2.0
                nr = tr + (255 - tr) * k
                ng = tg + (255 - tg) * k
                nb = tb + (255 - tb) * k
            px[x, y] = (int(nr), int(ng), int(nb), a)
    return out


def write_json(path, payload):
    io.open(path, 'w', encoding='utf-8').write(
        json.dumps(payload, indent=2, ensure_ascii=False) + '\n')


def main():
    side = Image.open(TEX + 'warp_pipe.png')
    top = Image.open(TEX + 'warp_pipe_top.png')

    for name, colour in COLOURS.items():
        recolour(side, colour).save(TEX + 'warp_pipe_%s.png' % name)
        recolour(top, colour).save(TEX + 'warp_pipe_%s_top.png' % name)
        write_json(A + 'models/block/warp_pipe_%s.json' % name, {
            "parent": "minecraft:block/cube_bottom_top",
            "textures": {
                "particle": "planeshift:block/warp_pipe_%s" % name,
                "side": "planeshift:block/warp_pipe_%s" % name,
                "top": "planeshift:block/warp_pipe_%s_top" % name,
                "bottom": "planeshift:block/warp_pipe_%s" % name,
            },
        })
    print('recoloured %d pipe pairs' % len(COLOURS))

    # One blockstate, five variants.
    variants = {"colour=green": {"model": "planeshift:block/warp_pipe"}}
    for name in COLOURS:
        variants["colour=%s" % name] = {"model": "planeshift:block/warp_pipe_%s" % name}
    write_json(A + 'blockstates/warp_pipe.json', {"variants": variants})
    print('blockstate written with %d variants' % len(variants))

    lp = A + 'lang/en_us.json'
    d = json.load(io.open(lp, encoding='utf-8'))
    d['block.planeshift.warp_pipe'] = 'Warp Pipe'
    io.open(lp, 'w', encoding='utf-8').write(
        json.dumps(d, indent=2, ensure_ascii=False) + '\n')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
