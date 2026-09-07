"""Textures for the three solid hazards: grinder, spiked ball, fire rock.

64x64 to match the projectile sheet layout BespokeProjectileModel already uses, with the same three
UV regions the meshes reference: (0,0) for the main mass, (32,0) and (32,32) for the smaller parts.

Flat regions rather than drawn detail. Every one of these is seen in motion at a distance -- a
spinning disc, a swinging ball, a falling rock -- and detail that cannot be resolved while the thing
moves costs texture memory to produce a smear. What reads is the silhouette and the value contrast,
so that is what these paint.
"""
import os
import sys

from PIL import Image, ImageDraw

S = 64


def shade(c, f):
    r, g, b = c[:3]
    if f <= 1.0:
        return (int(r * f), int(g * f), int(b * f), 255)
    t = f - 1.0
    return (int(r + (255 - r) * t), int(g + (255 - g) * t), int(b + (255 - b) * t), 255)


def region(img, at, base, lit=1.18, dark=0.72):
    """Fill a 32x32 region with a base colour, a lit top edge and a shadowed bottom."""
    d = ImageDraw.Draw(img)
    x, y = at
    d.rectangle([x, y, x + 31, y + 31], fill=base[:3] + (255,))
    d.rectangle([x, y, x + 31, y + 3], fill=shade(base, lit))
    d.rectangle([x, y + 28, x + 31, y + 31], fill=shade(base, dark))
    return img


def sheet():
    return Image.new("RGBA", (S, S), (0, 0, 0, 0))


def grinder():
    """Steel disc, brighter hub, near-white teeth so the rim reads while it spins."""
    img = sheet()
    region(img, (0, 0), (126, 134, 148))
    region(img, (32, 0), (86, 92, 104))
    region(img, (32, 32), (226, 232, 240))
    return img


def spiked_ball():
    """Dark iron with pale spikes. The contrast is the point: a uniformly dark ball
    swinging through a dark castle is a shape nobody can track."""
    img = sheet()
    region(img, (0, 0), (66, 68, 78))
    region(img, (32, 0), (198, 204, 214))
    region(img, (32, 32), (198, 204, 214))
    return img


def fire_rock():
    """Cooling basalt with hot chips, so it reads as thrown from somewhere molten."""
    img = sheet()
    region(img, (0, 0), (74, 58, 54))
    region(img, (32, 0), (208, 96, 48), lit=1.30, dark=0.86)
    region(img, (32, 32), (166, 72, 40), lit=1.28, dark=0.84)
    return img


SHEETS = {
    "grinder": grinder,
    "spiked_ball": spiked_ball,
    "fire_rock": fire_rock,
}


def main():
    if len(sys.argv) < 2:
        print(__doc__)
        return 1
    target = sys.argv[1]
    os.makedirs(target, exist_ok=True)
    for name, draw in sorted(SHEETS.items()):
        draw().save(os.path.join(target, name + ".png"))
    print("wrote %d hazard sheets to %s" % (len(SHEETS), target))
    return 0


if __name__ == "__main__":
    sys.exit(main())
