# Reference footage timeline

Title-card index of the walkthrough recording, produced by `tools/CutsceneIndex.py` over frames
sampled every two seconds. A title card is the near-black frame naming a level, so these are the
scene boundaries of the whole four hours.

## How to read it

**249 cards total.** Most are short: 164 last two seconds and 71 last four. Those are ordinary
transitions -- a level starting, a death, a pipe.

**14 last six seconds or longer, and those are the structural moments.** Verified by dense-sampling
the longest: the sixteen-second card at 3:06:28 is the ending sequence, running from the finale
scene through the bonus-world unlock into that world's first level. A card that lingers is a card
with something on either side of it.

## What this is for

Two entries in `WORK_PLAN.md` were written from single frames that turned out to be a cutscene and a
one-off encounter, and both became code before being caught. This index says which timestamps are
boundaries rather than gameplay, so the next thing read closely is read for what it is. Pair it with
`VideoFrames.py dense <start> <end>` to look at any stretch properly.

## The fourteen long cards

    card     0:26:46 -> 0:26:56   (10s)
    card     0:50:22 -> 0:50:30   (8s)
    card     0:53:12 -> 0:53:20   (8s)
    card     1:09:10 -> 1:09:16   (6s)
    card     1:23:00 -> 1:23:08   (8s)
    card     1:23:20 -> 1:23:28   (8s)
    card     2:22:06 -> 2:22:16   (10s)
    card     2:30:50 -> 2:30:56   (6s)
    card     2:38:56 -> 2:39:06   (10s)
    card     2:59:04 -> 2:59:12   (8s)
    card     3:06:28 -> 3:06:44   (16s)
    card     3:29:56 -> 3:30:04   (8s)
    card     3:46:08 -> 3:46:14   (6s)
    card     3:51:08 -> 3:51:14   (6s)

## Full index

See `REFERENCE_TIMELINE.txt` for all 249.
