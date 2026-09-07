#!/usr/bin/env python3
"""Index the non-gameplay moments in a long walkthrough recording.

Why this is not "find the cutscenes"
------------------------------------
The obvious approach is to look for frames with no HUD, on the theory that gameplay has a coin
counter and a timer and a cutscene does not. Checked against this footage, that is simply false:
every frame has HUD, including the castle rescue scenes, which are still in-level. There is no
HUD-free cutscene to find.

What does exist, and is reliably detectable, is one thing:

``card``    A near-black frame carrying the level name. These bracket every level in the video, so
            indexing them gives a table of contents -- which is the thing actually wanted when
            navigating four hours of footage.

Banner detection was tried and dropped
--------------------------------------
The announcement boxes ("a new world has appeared") looked like the obvious second target: a large
block of saturated yellow near the middle of the frame. It does not work, and the numbers are worth
recording so nobody spends the afternoon again.

Measured against a real banner and four false positives, the real one had a *lower* gold fraction
(0.054) than every false positive (0.18 to 0.26), and its flatness sat in the middle of theirs.
The reason is obvious in hindsight: this game's grass levels are full of enormous yellow-striped
hills that fill the centre of the frame, and the banner is a small box on a busy background. Colour
statistics cannot separate them, and a detector firing forty times an hour wrongly is worse than no
detector. Finding a banner needs template matching against the box art, which is a different tool.

The point of the index
----------------------
Two plan entries in this project were written from single frames that turned out to be a cutscene
and a one-off encounter. Knowing which timestamps are *not* ordinary gameplay is how that mistake
gets caught before it becomes code. Pair this with ``VideoFrames.py dense`` to read a specific
stretch closely once you know what it is.

Run:  python tools/CutsceneIndex.py <frames-dir> <seconds-per-frame> [--gap 3]
      (frames-dir holds fNNNNN.png sampled at a fixed interval)
"""

import os
import sys
from collections import namedtuple

try:
    from PIL import Image
except ImportError:  # pragma: no cover - the tool is useless without it
    print("needs Pillow: pip install pillow")
    raise SystemExit(1)


Hit = namedtuple("Hit", "index kind mean")

# A title card is nearly black. Sampled cards sit far below this; the darkest ordinary gameplay --
# an unlit cave, a ghost house interior -- sits comfortably above it.
CARD_MAX_MEAN = 26.0


def classify(path):
    """Return ('card' | None, mean brightness)."""
    image = Image.open(path).convert("RGB")
    pixels = list(image.getdata())
    mean = sum(sum(p) for p in pixels) / (3.0 * len(pixels))
    return ("card" if mean <= CARD_MAX_MEAN else None), mean


def group(hits, gap):
    """Merge hits of the same kind that are within ``gap`` samples of each other."""
    runs = []
    for hit in hits:
        if runs and hit.kind == runs[-1][0] and hit.index - runs[-1][2] <= gap:
            runs[-1][2] = hit.index
        else:
            runs.append([hit.kind, hit.index, hit.index])
    return runs


def clock(seconds):
    seconds = int(seconds)
    return "%d:%02d:%02d" % (seconds // 3600, (seconds % 3600) // 60, seconds % 60)


def main():
    if len(sys.argv) < 3:
        print(__doc__.strip())
        return 1
    folder, step = sys.argv[1], float(sys.argv[2])
    gap = 3
    if "--gap" in sys.argv:
        gap = int(sys.argv[sys.argv.index("--gap") + 1])

    frames = sorted(f for f in os.listdir(folder) if f.endswith(".png"))
    print("scanning %d frames at %gs each (%s of video)"
          % (len(frames), step, clock(len(frames) * step)))

    hits = []
    for i, name in enumerate(frames):
        kind, mean = classify(os.path.join(folder, name))
        if kind:
            hits.append(Hit(i, kind, mean))

    runs = group(hits, gap)
    cards = [r for r in runs if r[0] == "card"]
    banners = [r for r in runs if r[0] == "banner"]
    print("found %d title cards, %d banners\n" % (len(cards), len(banners)))

    for kind, first, last in runs:
        start, end = first * step, (last + 1) * step
        print("%-7s %8s -> %-8s  (%ds)" % (kind, clock(start), clock(end), end - start))
    return 0


if __name__ == "__main__":
    sys.exit(main())
