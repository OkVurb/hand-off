#!/usr/bin/env python3
"""Synthesise the mod's music in the modern New Super Mario Bros. idiom.

Why this exists
---------------
This mod imitates the New Super Mario Bros. line -- Wii, Wii U -- and that music has a specific,
copyable set of habits, none of which are the 8-bit ones:

* **Swing.** Straight sixteenths sound like the NES. NSMB is shuffled, and it is the single change
  that moves a track forty years forward.
* **Off-beat brass stabs.** The bass lands on the beat, a short brass chord answers between beats.
  That call-and-response is the texture people actually recognise.
* **Major sixths and ninths.** Plain triads sound like a fanfare. Adding the sixth is what makes a
  chord sound like a holiday.
* **A marimba or steel-drum lead**, not a square wave.
* **Percussion that is felt rather than heard** -- soft kick, brushed snare, busy closed hats.

Everything here is synthesised from oscillators and envelopes, so it is original work in a genre
rather than a copy of any particular piece, and it is generated rather than performed so a track
can be re-tuned by changing a number instead of being re-recorded.

What changed in this pass
-------------------------
1. **The loop gap is gone.** Every track was rendered into a buffer one second longer than the
   music (``Track(total + 1.0)``), so each loop ended with a full second of silence and audibly
   stopped before restarting. The buffer is now exactly the length of the music and voices near the
   end wrap around into the beginning, so a note or cymbal that rings past the final bar rings over
   bar one -- which is what makes a loop seamless rather than merely gapless.
2. **The brass filter was not a filter.** It fed the previous *output* sample -- already multiplied
   by its envelope and an output gain -- back in as filter state, and divided it by that gain to
   compensate. The brightness therefore tracked the envelope twice and collapsed on quiet notes.
   It keeps proper filter state now.
3. **Songs have sections.** Every track used to be one four-chord loop with two lead phrases
   alternating, which is fine for eight bars and wearing by the third minute. Tracks are now built
   from an A section and a B section with their own progressions and melodies.
4. **Stereo.** The buffer was mono, written twice. Voices are placed across the field now, which is
   most of the difference between "chiptune" and "produced".
5. **A pad**, under everything, so the arrangement has a floor.

Requires ffmpeg on PATH for the WAV to OGG step (Minecraft will not load WAV).

Run:  python tools/MusicGen.py src/main/resources/assets/planeshift/sounds
      python tools/MusicGen.py <dir> music_hub      # one track only
"""

import math
import os
import struct
import subprocess
import sys
import wave

RATE = 44100

# Semitone offsets from A4 = 440 Hz, by note name.
NOTES = {"C": -9, "C#": -8, "D": -7, "D#": -6, "E": -5, "F": -4,
         "F#": -3, "G": -2, "G#": -1, "A": 0, "A#": 1, "B": 2}


def freq(name, octave=4):
    return 440.0 * (2.0 ** ((NOTES[name] + (octave - 4) * 12) / 12.0))


class Track:
    """A stereo buffer you mix voices into, exactly one loop long."""

    def __init__(self, seconds):
        self.n = int(RATE * seconds)
        self.left = [0.0] * self.n
        self.right = [0.0] * self.n

    def add(self, start, samples, gain=1.0, pan=0.0):
        """Mix a voice in at ``start`` seconds, wrapping past the end back to the beginning.

        The wrap is the whole point. A marimba note struck on the last sixteenth still has most of
        its decay left when the loop ends; without wrapping, that decay is simply cut off and the
        seam is audible every time the track repeats. Wrapping lets the tail ring over the top of
        bar one, which is how the loop stops sounding like a loop.
        """
        # Equal-power panning, so a voice does not get louder as it moves to the centre.
        gl = gain * math.sqrt((1.0 - pan) * 0.5)
        gr = gain * math.sqrt((1.0 + pan) * 0.5)
        i = int(start * RATE)
        n = self.n
        for k, v in enumerate(samples):
            j = (i + k) % n
            self.left[j] += v * gl
            self.right[j] += v * gr

    def save(self, path):
        peak = max(1e-6, max(max(abs(v) for v in self.left),
                             max(abs(v) for v in self.right)))
        # Leave headroom rather than normalising to full scale: these loop under sound effects, and
        # a track mastered to 0 dBFS buries every stomp and coin in the game.
        scale = 0.72 / peak
        frames = bytearray()
        for a, b in zip(self.left, self.right):
            l = int(max(-1.0, min(1.0, a * scale)) * 32767)
            r = int(max(-1.0, min(1.0, b * scale)) * 32767)
            frames += struct.pack("<hh", l, r)
        with wave.open(path, "wb") as w:
            w.setnchannels(2)
            w.setsampwidth(2)
            w.setframerate(RATE)
            w.writeframes(bytes(frames))


def env(n, attack, decay, sustain, release):
    """A sampled ADSR curve of length n."""
    a = int(n * attack)
    d = int(n * decay)
    r = int(n * release)
    s = max(0, n - a - d - r)
    out = []
    for i in range(a):
        out.append(i / max(1, a))
    for i in range(d):
        out.append(1.0 - (1.0 - sustain) * (i / max(1, d)))
    out += [sustain] * s
    for i in range(r):
        out.append(sustain * (1.0 - i / max(1, r)))
    return (out + [0.0] * n)[:n]


# Rendering the same note twice is common enough that caching it roughly halves the run time.
_CACHE = {}


def cached(fn, *args):
    key = (fn.__name__,) + tuple(round(a, 4) if isinstance(a, float) else a for a in args)
    hit = _CACHE.get(key)
    if hit is None:
        hit = fn(*args)
        _CACHE[key] = hit
    return hit


def marimba(f, dur):
    """Struck wooden bar: a fast attack, a hard decay, and a strong second partial."""
    n = int(RATE * dur)
    e = env(n, 0.004, 0.35, 0.22, 0.5)
    out = []
    for i in range(n):
        t = i / RATE
        v = (math.sin(2 * math.pi * f * t)
             + 0.45 * math.sin(2 * math.pi * f * 4.0 * t)
             + 0.18 * math.sin(2 * math.pi * f * 9.2 * t))
        out.append(v * e[i] * 0.33)
    return out


def brass(f, dur):
    """A short stab: a saw whose brightness falls away as the note decays.

    The lowpass keeps its own state. The previous version filtered the *output* -- already scaled
    by the envelope and the output gain -- and divided by that gain to undo it, which meant the
    cutoff tracked the envelope a second time and the tone collapsed on anything quiet.
    """
    n = int(RATE * dur)
    e = env(n, 0.02, 0.20, 0.55, 0.45)
    out = []
    phase = 0.0
    low = 0.0
    for i in range(n):
        phase += f / RATE
        saw = 2.0 * (phase % 1.0) - 1.0
        cutoff = 0.25 + 0.55 * e[i]
        low += cutoff * (saw - low)
        out.append(low * e[i] * 0.30)
    return out


def pad(f, dur):
    """Two detuned saws, soft and slow. The floor the rest of the arrangement stands on."""
    n = int(RATE * dur)
    e = env(n, 0.25, 0.2, 0.75, 0.35)
    out = []
    p1 = 0.0
    p2 = 0.0
    low = 0.0
    for i in range(n):
        p1 += f / RATE
        p2 += (f * 1.004) / RATE
        saw = (2.0 * (p1 % 1.0) - 1.0) + (2.0 * (p2 % 1.0) - 1.0)
        low += 0.06 * (saw * 0.5 - low)
        out.append(low * e[i] * 0.16)
    return out


def bass(f, dur):
    """Round and short, so it drives without muddying the low end."""
    n = int(RATE * dur)
    e = env(n, 0.006, 0.25, 0.5, 0.4)
    out = []
    for i in range(n):
        t = i / RATE
        v = math.sin(2 * math.pi * f * t) + 0.28 * math.sin(2 * math.pi * f * 2 * t)
        out.append(v * e[i] * 0.42)
    return out


def kick(dur=0.16):
    n = int(RATE * dur)
    out = []
    for i in range(n):
        t = i / RATE
        f = 110.0 * math.exp(-t * 26.0) + 42.0
        out.append(math.sin(2 * math.pi * f * t) * math.exp(-t * 14.0) * 0.75)
    return out


def _noise(seed):
    state = seed & 0xFFFFFFFF
    while True:
        state = (1103515245 * state + 12345) & 0x7FFFFFFF
        yield (state / 0x3FFFFFFF) - 1.0


def snare(dur=0.14, seed=7):
    n = int(RATE * dur)
    gen = _noise(seed)
    out = []
    for i in range(n):
        t = i / RATE
        out.append((next(gen) * 0.7 + math.sin(2 * math.pi * 185 * t) * 0.3)
                   * math.exp(-t * 26.0) * 0.42)
    return out


def hat(dur=0.05, seed=11, gain=0.20):
    n = int(RATE * dur)
    gen = _noise(seed)
    return [next(gen) * math.exp(-(i / RATE) * 90.0) * gain for i in range(n)]


def swing(step, amount=0.62):
    """Shuffle: odd sixteenths land late. This is most of what makes it not sound like the NES."""
    return (step // 2) * 2 + (amount * 2 if step % 2 else 0)


def chord(kind):
    """Chord tones in semitones. The sixth is deliberate -- it is the 'holiday' interval."""
    if kind == "maj6":
        return [0, 4, 7, 9]
    if kind == "min7":
        return [0, 3, 7, 10]
    if kind == "dom9":
        return [0, 4, 10, 14]
    if kind == "dim":
        return [0, 3, 6]
    return [0, 4, 7]


def render_section(tr, offset, bars, bpm, progression, lead_notes, seed, busy, bar0):
    """Lay one section of the arrangement into the track, starting at ``offset`` seconds."""
    beat = 60.0 / bpm
    sixteenth = beat / 4.0

    for bar in range(bars):
        root_name, root_oct, kind = progression[bar % len(progression)]
        root = freq(root_name, root_oct)
        bar_t = offset + bar * 4 * beat

        # A pad holding the chord for the whole bar, wide, quiet, underneath everything.
        for idx, semi in enumerate(chord(kind)):
            spread = -0.6 + 0.4 * idx
            tr.add(bar_t, cached(pad, root * (2 ** (semi / 12.0)), beat * 4.0), 0.5, spread)

        # Bass on the beat, an octave answer on the and-of-two: the NSMB walking feel.
        for b in range(4):
            tr.add(bar_t + b * beat, cached(bass, root / 2, beat * 0.55), 1.0, 0.0)
            if b % 2 == 1:
                tr.add(bar_t + b * beat + beat * 0.5,
                       cached(bass, root, beat * 0.3), 0.6, 0.0)

        # Brass stabs answering off the beat, spread across the field.
        for b in (0, 2):
            at = bar_t + b * beat + beat * 0.5
            tones = chord(kind)
            for idx, semi in enumerate(tones):
                pan = -0.5 + idx * (1.0 / max(1, len(tones) - 1))
                tr.add(at, cached(brass, root * (2 ** (semi / 12.0)), beat * 0.42), 0.5, pan)

        # Percussion.
        for b in range(4):
            at = bar_t + b * beat
            if b in (0, 2):
                tr.add(at, cached(kick), 0.9, 0.0)
            if b in (1, 3):
                tr.add(at, cached(snare, 0.14, seed + b), 0.7, 0.0)
            if busy:
                for s in range(4):
                    # Alternating narrow placement, so the hats shimmer instead of sitting in a
                    # single point in the middle of the mix.
                    pan = 0.22 if (s % 2) else -0.22
                    tr.add(at + swing(s) * sixteenth,
                           cached(hat, 0.05, seed + s + b * 4, 0.20), 0.8, pan)

        # Lead: marimba, one phrase per bar, swung.
        phrase = lead_notes[(bar0 + bar) % len(lead_notes)]
        for step, semi in enumerate(phrase):
            if semi is None:
                continue
            at = bar_t + swing(step) * sixteenth
            tr.add(at, cached(marimba, root * (2 ** (semi / 12.0)), sixteenth * 3.2), 0.85, 0.18)


def render(name, bpm, sections, seed):
    """Assemble one track from a list of (bars, progression, lead, busy) sections."""
    beat = 60.0 / bpm
    total = sum(s[0] for s in sections) * 4 * beat
    # Exactly one loop long. Voices that ring past the end wrap into the start; see Track.add.
    tr = Track(total)

    offset = 0.0
    bar0 = 0
    for bars, progression, lead, busy in sections:
        render_section(tr, offset, bars, bpm, progression, lead, seed, busy, bar0)
        offset += bars * 4 * beat
        bar0 += bars

    out_wav = os.path.join(TARGET, name + ".wav")
    tr.save(out_wav)
    out_ogg = os.path.join(TARGET, name + ".ogg")
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", out_wav,
                    "-c:a", "libvorbis", "-qscale:a", "5", out_ogg], check=True)
    os.remove(out_wav)
    print("  %-22s %5.1fs  %2d bars @ %d bpm" % (name, total, bar0, bpm))


# ---------------------------------------------------------------- the tracks

# Progressions are (root, octave, chord kind). I-vi-IV-V with sixths is the backbone of the whole
# NSMB overworld sound; the minor variants below are the same trick with the thirds flattened.
OVERWORLD = [("C", 3, "maj6"), ("A", 2, "min7"), ("F", 3, "maj6"), ("G", 3, "dom9")]
# The B section moves to the subdominant, which is the ordinary way a bright loop finds somewhere
# else to be for eight bars without changing key.
OVERWORLD_B = [("F", 3, "maj6"), ("D", 3, "min7"), ("G", 3, "maj6"), ("G", 3, "dom9")]
ATHLETIC = [("D", 3, "maj6"), ("B", 2, "min7"), ("G", 3, "maj6"), ("A", 3, "dom9")]
ATHLETIC_B = [("G", 3, "maj6"), ("E", 3, "min7"), ("A", 3, "maj6"), ("A", 3, "dom9")]
UNDER = [("A", 2, "min7"), ("F", 2, "maj6"), ("C", 3, "maj6"), ("E", 3, "dom9")]
UNDER_B = [("D", 3, "min7"), ("A#", 2, "maj6"), ("F", 3, "maj6"), ("E", 3, "dom9")]
BOSS = [("D", 2, "min7"), ("D", 2, "dim"), ("A#", 2, "maj6"), ("A", 2, "dom9")]
BOSS_B = [("G", 2, "min7"), ("G", 2, "dim"), ("D#", 2, "maj6"), ("A", 2, "dom9")]

# Lead phrases, in semitones above the chord root. None is a rest.
BOUNCE = [[0, None, 4, 7, None, 4, 0, None, 7, None, 9, 7, None, 4, None, None],
          [7, None, 4, 0, None, 4, 7, None, 9, None, 7, 4, None, 0, None, None]]
# The answering phrase: same shape, higher, and it resolves downward so the A section can come
# back in without the join sounding abrupt.
BOUNCE_B = [[12, None, 9, 7, None, 9, 12, None, 16, None, 14, 12, None, 9, None, None],
            [9, None, 7, 4, None, 7, 9, None, 12, None, 9, 7, None, 4, None, None]]
CLIMB = [[0, 4, 7, 12, None, 9, 7, 4, 0, None, 4, 7, None, 12, None, None],
         [12, 9, 7, 4, None, 7, 9, 12, None, 9, 7, None, 4, None, 0, None]]
CLIMB_B = [[7, 12, 16, 19, None, 16, 12, 7, None, 12, 16, None, 19, None, None, None],
           [19, 16, 12, 9, None, 12, 16, 19, None, 16, 12, None, 7, None, None, None]]
STOMP = [[0, 0, None, 3, 3, None, 7, None, 0, 0, None, 3, None, 10, None, None]]
STOMP_B = [[0, 0, None, 5, 5, None, 10, None, 0, 0, None, 5, None, 12, None, None]]
RUSH = [[0, 7, 12, 7, 0, 7, 12, 7, 0, 7, 12, 7, 0, 7, 12, 7]]

TRACKS = [
    # name, bpm, [(bars, progression, lead, busy), ...], seed
    ("music_hub", 116, [(8, OVERWORLD, BOUNCE, True),
                        (8, OVERWORLD_B, BOUNCE_B, True),
                        (8, OVERWORLD, BOUNCE, True)], 3),
    ("music_course_2_5d", 148, [(8, OVERWORLD, BOUNCE, True),
                                (8, OVERWORLD_B, BOUNCE_B, True),
                                (8, OVERWORLD, BOUNCE, True)], 5),
    ("music_course_3d", 138, [(8, ATHLETIC, CLIMB, True),
                              (8, ATHLETIC_B, CLIMB_B, True),
                              (8, ATHLETIC, CLIMB, True)], 9),
    ("music_combat", 164, [(8, UNDER, STOMP, True),
                           (8, UNDER_B, STOMP_B, True)], 13),
    ("music_boss", 172, [(8, BOSS, STOMP, True),
                         (8, BOSS_B, STOMP_B, True)], 17),
    # Star power stays a single short loop on purpose: it plays for a few seconds at a time, so a
    # B section would never be heard.
    ("music_star_power", 200, [(8, ATHLETIC, RUSH, True)], 23),
]

TARGET = "."


def main():
    global TARGET
    if len(sys.argv) < 2:
        print(__doc__)
        return 1
    TARGET = sys.argv[1]
    os.makedirs(TARGET, exist_ok=True)
    only = sys.argv[2] if len(sys.argv) > 2 else None
    print("rendering:")
    for name, bpm, sections, seed in TRACKS:
        if only and name != only:
            continue
        _CACHE.clear()
        render(name, bpm, sections, seed)
    return 0


if __name__ == "__main__":
    sys.exit(main())
