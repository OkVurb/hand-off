#!/usr/bin/env python3
"""Sample a long gameplay video into readable contact sheets.

Why this exists
---------------
Reference footage is the best input for level design work -- pacing, layout, how far apart hazards
sit, when a mechanic is introduced versus when it is tested -- and none of that survives being
described second hand. But a four hour video is around 430,000 frames, and the useful unit is not
a frame, it is a *sheet*: a grid of frames far enough apart to show how a level changes.

So this does two things and keeps them separate:

``sheets``  Coarse pass. One frame every N seconds, tiled into grids. This is for skimming a whole
            video and finding the parts worth looking at properly.
``at``      Fine pass. One full-resolution frame at an exact timestamp, for when a sheet shows
            something worth reading closely.

The two-tier split matters because looking at images is the expensive step, not extracting them.
Extracting 240 frames costs seconds; reading 240 images is not possible. Ten sheets is.

Notes on what this is for
-------------------------
Design reference: composition, colour, spacing, silhouette, the shape of a level's difficulty
curve. It is a tool for taking notes from footage, not for lifting art out of it -- the textures
and models in this mod are all generated from primitives, and that is deliberate.

Requires ffmpeg and ffprobe on PATH.

Run:  python tools/VideoFrames.py sheets <video> <outdir> [--every 60] [--cols 6] [--rows 4]
      python tools/VideoFrames.py dense <video> <outdir> <start> <end> [--every 1] [--tag name]
      python tools/VideoFrames.py at <video> <outdir> <seconds> [<seconds> ...]
"""

import os
import subprocess
import sys


def duration(video):
    out = subprocess.run(
        ["ffprobe", "-v", "error", "-show_entries", "format=duration",
         "-of", "default=nw=1:nk=1", video],
        capture_output=True, text=True, check=True).stdout.strip()
    return float(out)


def dimensions(video):
    out = subprocess.run(
        ["ffprobe", "-v", "error", "-select_streams", "v:0",
         "-show_entries", "stream=width,height", "-of", "csv=p=0:s=x", video],
        capture_output=True, text=True, check=True).stdout.strip()
    return out


def dense(video, outdir, start, end, every=1, cols=6, rows=4, cell=360, tag="dense"):
    """Sheets of one slice of the video, sampled finely.

    The coarse pass covers the whole video at one frame every few seconds, which is right for
    finding *where* something happens and useless for reading *how* it works. A boss fight lasting
    forty seconds is four frames at that rate -- enough to see that a boss exists, not enough to see
    that it charges, stuns itself on a wall, and then fires between charges.

    That gap has cost real work. Two plan entries were written from single frames of a boss and both
    were wrong: one was a cutscene read as a fight, the other a one-off encounter read as the rule.
    Finer sampling would not have prevented the second -- no number of frames tells you a rule --
    but it would have made the first obvious.

    So this exists for movesets: pick the seconds a fight occupies and sample them at one or two
    frames a second.
    """
    span = end - start
    frames = max(1, int(span // every))
    per_sheet = cols * rows
    total_sheets = (frames + per_sheet - 1) // per_sheet
    os.makedirs(outdir, exist_ok=True)
    print("dense: %ds..%ds (%ds) at 1 frame/%ds -> %d frames -> %d sheets of %dx%d"
          % (start, end, span, every, frames, total_sheets, cols, rows))

    for index in range(total_sheets):
        offset = start + index * per_sheet * every
        out = os.path.join(outdir, "%s_%03d.jpg" % (tag, index))
        cmd = ["ffmpeg", "-y", "-loglevel", "error",
               "-ss", str(offset), "-t", str(per_sheet * every), "-i", video,
               "-vf", "fps=1/%s,scale=%d:-1,tile=%dx%d" % (every, cell, cols, rows),
               "-frames:v", "1", out]
        subprocess.run(cmd, check=True)
        print("  %s  %.1f s -> %.1f s" % (os.path.basename(out), offset,
                                          offset + per_sheet * every))
    return total_sheets


def sheets(video, outdir, every=60, cols=6, rows=4, cell=360):
    """Tile one frame every ``every`` seconds into grids of ``cols`` x ``rows``."""
    os.makedirs(outdir, exist_ok=True)
    total = duration(video)
    per_sheet = cols * rows
    frames = int(total // every)
    made = 0

    print("video   : %s" % os.path.basename(video))
    print("length  : %.0fs (%.2f h)   %s" % (total, total / 3600.0, dimensions(video)))
    print("sampling: 1 frame / %ds  ->  %d frames  ->  %d sheets of %dx%d"
          % (every, frames, (frames + per_sheet - 1) // per_sheet, cols, rows))

    index = 0
    while index * per_sheet < frames:
        start = index * per_sheet * every
        out = os.path.join(outdir, "sheet_%03d.jpg" % index)
        # -ss before -i seeks by keyframe, which is fast enough to matter over four hours.
        # fps=1/every resamples the decoded stream; tile packs the result into one image.
        cmd = ["ffmpeg", "-v", "error", "-ss", str(start), "-i", video,
               "-vf", "fps=1/%d,scale=%d:-1,tile=%dx%d" % (every, cell, cols, rows),
               "-frames:v", "1", "-qscale:v", "3", "-y", out]
        subprocess.run(cmd, check=False)
        if os.path.exists(out) and os.path.getsize(out) > 0:
            mins = start / 60.0
            print("  sheet_%03d.jpg  covers %.1f min -> %.1f min"
                  % (index, mins, mins + (per_sheet * every) / 60.0))
            made += 1
        else:
            break
        index += 1
    print("wrote %d sheets to %s" % (made, outdir))
    return made


def at(video, outdir, seconds):
    """Pull one full-resolution frame per timestamp."""
    os.makedirs(outdir, exist_ok=True)
    for sec in seconds:
        out = os.path.join(outdir, "at_%08.2f.png" % sec)
        subprocess.run(["ffmpeg", "-v", "error", "-ss", str(sec), "-i", video,
                        "-frames:v", "1", "-y", out], check=False)
        ok = os.path.exists(out) and os.path.getsize(out) > 0
        print("  %s  %s" % (os.path.basename(out), "ok" if ok else "FAILED (past end of file?)"))


def main():
    if len(sys.argv) < 4:
        print(__doc__)
        return 1
    mode, video, outdir = sys.argv[1], sys.argv[2], sys.argv[3]
    if not os.path.isfile(video):
        print("no such video: %s" % video)
        return 1

    if mode == "sheets":
        args = sys.argv[4:]
        every, cols, rows = 60, 6, 4
        for i, a in enumerate(args):
            if a == "--every":
                every = int(args[i + 1])
            elif a == "--cols":
                cols = int(args[i + 1])
            elif a == "--rows":
                rows = int(args[i + 1])
        sheets(video, outdir, every, cols, rows)
    elif mode == "dense":
        # dense <video> <outdir> <start> <end> [--every N] [--tag name]
        start, end = float(sys.argv[4]), float(sys.argv[5])
        args = sys.argv[6:]
        every, tag = 1, "dense"
        for i, a in enumerate(args):
            if a == "--every":
                every = float(args[i + 1])
            elif a == "--tag":
                tag = args[i + 1]
        dense(video, outdir, start, end, every, tag=tag)
    elif mode == "at":
        at(video, outdir, [float(a) for a in sys.argv[4:]])
    else:
        print(__doc__)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
