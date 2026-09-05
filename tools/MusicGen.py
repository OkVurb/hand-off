#!/usr/bin/env python3
"""Synthesise the mod's music in the modern New Super Mario Bros. idiom.

Why this exists
---------------
The existing tracks were written for the wrong era. This mod imitates the New Super Mario Bros.
line — Wii, Wii U — and that music has a very specific and very copyable set of habits, none of
which are the 8-bit ones:

* **Swing.** Straight sixteenths sound like the NES. NSMB is shuffled, and it is the single change
  that moves a track forty years forward.
* **Off-beat brass stabs.** The bass lands on the beat, a short brass chord answers between beats.
  That call-and-response is the texture people actually recognise.
* **Major sixths and ninths.** Plain triads sound like a fanfare. Adding the sixth is what makes a
  chord sound like a holiday.
* **A marimba or steel-drum lead**, not a square wave.
* **Percussion that is felt rather than heard** — soft kick, brushed snare, busy closed hats.

Everything here is synthesised from oscillators and envelopes, so it is original work, and it is
generated rather than performed so a track can be re-tuned by changing a number instead of being
re-recorded.

Requires ffmpeg on PATH for the WAV to OGG step (Minecraft will not load WAV).

Run:  python tools/MusicGen.py src/main/resources/assets/planeshift/sounds
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
    """A mono buffer you mix voices into."""

    def __init__(self, seconds):
        self.n = int(RATE * seconds)
        self.buf = [0.0] * self.n

    def add(self, start, samples, gain=1.0):
        i = int(start * RATE)
        for k, v in enumerate(samples):
            j = i + k
            if 0 <= j < self.n:
                self.buf[j] += v * gain

    def save(self, path):
        peak = max(1e-6, max(abs(v) for v in self.buf))
        # Leave headroom rather than normalising to full scale: these loop under sound effects,
        # and a track mastered to 0 dBFS buries every stomp and coin in the game.
        scale = 0.72 / peak
        frames = bytearray()
        for v in self.buf:
            s = int(max(-1.0, min(1.0, v * scale)) * 32767)
            frames += struct.pack("<hh", s, s)
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
    """A short stab. Saw-ish, with the brightness falling away as the note decays."""
    n = int(RATE * dur)
    e = env(n, 0.02, 0.20, 0.55, 0.45)
    out = []
    phase = 0.0
    for i in range(n):
        t = i / RATE
        phase += f / RATE
        saw = 2.0 * (phase % 1.0) - 1.0
        # One-pole lowpass that closes as the envelope falls: the cheapest convincing brass cue.
        cutoff = 0.25 + 0.55 * e[i]
        if out:
            saw = out[-1] / 0.3 * (1 - cutoff) + saw * cutoff
        out.append(saw * e[i] * 0.30)
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


def chord(root, kind):
    """Chord tones in semitones. The sixth is deliberate — it is the 'holiday' interval."""
    if kind == "maj6":
        return [0, 4, 7, 9]
    if kind == "min7":
        return [0, 3, 7, 10]
    if kind == "dom9":
        return [0, 4, 10, 14]
    if kind == "dim":
        return [0, 3, 6]
    return [0, 4, 7]


def render(name, bpm, bars, progression, lead_notes, seed, minor=False, busy=True):
    """Assemble one track from a chord loop and a lead line."""
    beat = 60.0 / bpm
    sixteenth = beat / 4.0
    total = bars * 4 * beat
    tr = Track(total + 1.0)

    for bar in range(bars):
        root_name, root_oct, kind = progression[bar % len(progression)]
        root = freq(root_name, root_oct)
        bar_t = bar * 4 * beat

        # Bass on the beat, an octave answer on the and-of-two: the NSMB walking feel.
        for b in range(4):
            tr.add(bar_t + b * beat, bass(root / 2, beat * 0.55), 1.0)
            if b % 2 == 1:
                tr.add(bar_t + b * beat + beat * 0.5, bass(root, beat * 0.3), 0.6)

        # Brass stabs answering off the beat.
        for b in (0, 2):
            at = bar_t + b * beat + beat * 0.5
            for semi in chord(root, kind):
                tr.add(at, brass(root * (2 ** (semi / 12.0)), beat * 0.42), 0.5)

        # Percussion.
        for b in range(4):
            at = bar_t + b * beat
            if b in (0, 2):
                tr.add(at, kick(), 0.9)
            if b in (1, 3):
                tr.add(at, snare(seed=seed + b), 0.7)
            if busy:
                for s in range(4):
                    tr.add(at + swing(s) * sixteenth, hat(seed=seed + s + b * 4), 0.8)

        # Lead: marimba, one phrase per bar, swung.
        phrase = lead_notes[bar % len(lead_notes)]
        for step, semi in enumerate(phrase):
            if semi is None:
                continue
            at = bar_t + swing(step) * sixteenth
            tr.add(at, marimba(root * (2 ** (semi / 12.0)), sixteenth * 3.2), 0.85)

    out_wav = os.path.join(TARGET, name + ".wav")
    tr.save(out_wav)
    out_ogg = os.path.join(TARGET, name + ".ogg")
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", out_wav,
                    "-c:a", "libvorbis", "-qscale:a", "4", out_ogg], check=True)
    os.remove(out_wav)
    print("  %s  (%d bars @ %d bpm)" % (name, bars, bpm))


# ---------------------------------------------------------------- the tracks

# Progressions are (root, octave, chord kind). I-vi-IV-V with sixths is the backbone of the whole
# NSMB overworld sound; the minor variants below are the same trick with the thirds flattened.
OVERWORLD = [("C", 3, "maj6"), ("A", 2, "min7"), ("F", 3, "maj6"), ("G", 3, "dom9")]
ATHLETIC = [("D", 3, "maj6"), ("B", 2, "min7"), ("G", 3, "maj6"), ("A", 3, "dom9")]
UNDER = [("A", 2, "min7"), ("F", 2, "maj6"), ("C", 3, "maj6"), ("E", 3, "dom9")]
BOSS = [("D", 2, "min7"), ("D", 2, "dim"), ("A#", 2, "maj6"), ("A", 2, "dom9")]

# Lead phrases, in semitones above the chord root. None is a rest.
BOUNCE = [[0, None, 4, 7, None, 4, 0, None, 7, None, 9, 7, None, 4, None, None],
          [7, None, 4, 0, None, 4, 7, None, 9, None, 7, 4, None, 0, None, None]]
CLIMB = [[0, 4, 7, 12, None, 9, 7, 4, 0, None, 4, 7, None, 12, None, None],
         [12, 9, 7, 4, None, 7, 9, 12, None, 9, 7, None, 4, None, 0, None]]
STOMP = [[0, 0, None, 3, 3, None, 7, None, 0, 0, None, 3, None, 10, None, None]]
RUSH = [[0, 7, 12, 7, 0, 7, 12, 7, 0, 7, 12, 7, 0, 7, 12, 7]]

TRACKS = [
    # name, bpm, bars, progression, lead, seed, busy
    ("music_hub", 116, 16, OVERWORLD, BOUNCE, 3, True),
    ("music_course_2_5d", 148, 16, OVERWORLD, BOUNCE, 5, True),
    ("music_course_3d", 138, 16, ATHLETIC, CLIMB, 9, True),
    ("music_combat", 164, 12, UNDER, STOMP, 13, True),
    ("music_boss", 172, 12, BOSS, STOMP, 17, True),
    ("music_star_power", 200, 8, ATHLETIC, RUSH, 23, True),
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
    for name, bpm, bars, prog, lead, seed, busy in TRACKS:
        if only and name != only:
            continue
        render(name, bpm, bars, prog, lead, seed, busy=busy)
    return 0


if __name__ == "__main__":
    sys.exit(main())
