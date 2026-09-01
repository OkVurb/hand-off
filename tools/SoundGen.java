import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Generates every PlaneShift sound as an original chiptune-style waveform.
 *
 * <p>Run with {@code java SoundGen.java <outDir>}. Writes 16-bit PCM WAV; the caller
 * encodes to OGG Vorbis. Nothing here samples or transcribes an existing work - every
 * waveform is synthesised from the oscillators below and every melody is written for
 * this project. The NES-style four-voice palette (two pulses, a triangle bass and an
 * LFSR noise channel) is a technique, not content.
 *
 * <p>SFX render mono because Minecraft only attenuates and positions mono sources.
 * The four music tracks render stereo: they play through {@code SimpleSoundInstance.forMusic},
 * which is non-positional.
 */
public final class SoundGen {

    static final int SR = 44100;

    // ---------------------------------------------------------------- oscillators

    static final int PULSE = 0;
    static final int TRIANGLE = 1;
    static final int NOISE = 2;
    static final int SAW = 3;

    static double pulse(double phase, double duty) {
        return (phase - Math.floor(phase)) < duty ? 1.0 : -1.0;
    }

    /** NES triangle: 16 quantised steps, which is what gives it the hollow bass tone. */
    static double triangle(double phase) {
        double p = phase - Math.floor(phase);
        double v = p < 0.5 ? (4.0 * p - 1.0) : (3.0 - 4.0 * p);
        return Math.round(v * 7.5) / 7.5;
    }

    static double saw(double phase) {
        double p = phase - Math.floor(phase);
        return 2.0 * p - 1.0;
    }

    // ---------------------------------------------------------------- note names

    private static final Map<String, Integer> SEMITONE = new HashMap<>();

    static {
        String[] names = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
        for (int i = 0; i < names.length; i++) {
            SEMITONE.put(names[i], i);
        }
        SEMITONE.put("Db", 1);
        SEMITONE.put("Eb", 3);
        SEMITONE.put("Gb", 6);
        SEMITONE.put("Ab", 8);
        SEMITONE.put("Bb", 10);
    }

    /** "A4" -> 440.0, with middle C at C4. */
    static double note(String name) {
        int split = name.length() - 1;
        String pitch = name.substring(0, split);
        int octave = Integer.parseInt(name.substring(split));
        Integer semi = SEMITONE.get(pitch);
        if (semi == null) {
            throw new IllegalArgumentException("bad note: " + name);
        }
        int midi = (octave + 1) * 12 + semi;
        return 440.0 * Math.pow(2.0, (midi - 69) / 12.0);
    }

    // ---------------------------------------------------------------- buffer

    static final class Buf {
        final double[] left;
        final double[] right;
        final int n;

        Buf(double seconds) {
            this.n = (int) Math.ceil(seconds * SR);
            this.left = new double[n];
            this.right = new double[n];
        }
    }

    // ---------------------------------------------------------------- tone

    static final class Tone {
        double start;
        double dur;
        double f0;
        double f1 = -1.0;          // exponential sweep target; -1 keeps f0
        int wave = PULSE;
        double duty = 0.5;
        double amp = 0.25;
        double attack = 0.004;
        double release = 0.03;
        double sustain = 1.0;
        double decay = 0.0;        // time to fall from 1.0 to sustain
        boolean expDecay = false;  // percussive: ignore sustain/release, decay exponentially
        double expRate = 6.0;
        double vibHz = 0.0;
        double vibCents = 0.0;
        double pan = 0.0;          // -1 left .. +1 right
        int noiseSeed = 0x7FFF;

        Tone at(double s) { this.start = s; return this; }
        Tone len(double d) { this.dur = d; return this; }
        Tone freq(double f) { this.f0 = f; return this; }
        Tone sweep(double to) { this.f1 = to; return this; }
        Tone wave(int w) { this.wave = w; return this; }
        Tone duty(double d) { this.duty = d; return this; }
        Tone amp(double a) { this.amp = a; return this; }
        Tone env(double a, double r) { this.attack = a; this.release = r; return this; }
        Tone perc(double rate) { this.expDecay = true; this.expRate = rate; return this; }
        Tone vib(double hz, double cents) { this.vibHz = hz; this.vibCents = cents; return this; }
        Tone pan(double p) { this.pan = p; return this; }
    }

    static Tone tone() {
        return new Tone();
    }

    static void render(Buf buf, Tone t) {
        int i0 = (int) (t.start * SR);
        int len = (int) (t.dur * SR);
        if (len <= 0) {
            return;
        }
        double gl = Math.cos((t.pan + 1.0) * Math.PI / 4.0);
        double gr = Math.sin((t.pan + 1.0) * Math.PI / 4.0);

        double phase = 0.0;
        int lfsr = t.noiseSeed;
        double noiseAcc = 0.0;
        double noiseVal = 1.0;

        for (int i = 0; i < len; i++) {
            int idx = i0 + i;
            if (idx < 0 || idx >= buf.n) {
                continue;
            }
            double tt = i / (double) SR;
            double x = tt / t.dur;

            double f = (t.f1 < 0.0) ? t.f0 : t.f0 * Math.pow(t.f1 / t.f0, x);
            if (t.vibCents != 0.0) {
                f *= Math.pow(2.0, (t.vibCents / 1200.0) * Math.sin(2.0 * Math.PI * t.vibHz * tt));
            }

            double s;
            if (t.wave == NOISE) {
                noiseAcc += f;
                while (noiseAcc >= SR) {
                    noiseAcc -= SR;
                    int bit = (lfsr ^ (lfsr >> 1)) & 1;
                    lfsr = (lfsr >> 1) | (bit << 14);
                    noiseVal = ((lfsr & 1) == 0) ? 1.0 : -1.0;
                }
                s = noiseVal;
            } else {
                phase += f / SR;
                s = switch (t.wave) {
                    case TRIANGLE -> triangle(phase);
                    case SAW -> saw(phase);
                    default -> pulse(phase, t.duty);
                };
            }

            double e;
            if (t.expDecay) {
                e = Math.exp(-x * t.expRate);
                if (tt < t.attack) {
                    e *= tt / t.attack;
                }
            } else if (tt < t.attack) {
                e = tt / t.attack;
            } else if (t.decay > 0.0 && tt < t.attack + t.decay) {
                e = 1.0 - (1.0 - t.sustain) * ((tt - t.attack) / t.decay);
            } else if (tt > t.dur - t.release) {
                e = t.sustain * Math.max(0.0, (t.dur - tt) / t.release);
            } else {
                e = t.sustain;
            }

            double v = s * e * t.amp;
            buf.left[idx] += v * gl;
            buf.right[idx] += v * gr;
        }
    }

    // ---------------------------------------------------------------- sequencer

    /**
     * Places one token per step. "." rests, "-" extends the previous note, anything else
     * is a note name. {@code gate} is the fraction of the step the note actually sounds.
     */
    static void seq(Buf buf, double startSec, double stepSec, String pattern,
                    int wave, double duty, double amp, double pan, double gate) {
        String[] tokens = pattern.trim().split("\\s+");
        int i = 0;
        while (i < tokens.length) {
            String tok = tokens[i];
            if (tok.equals(".") || tok.equals("-")) {
                i++;
                continue;
            }
            int held = 1;
            while (i + held < tokens.length && tokens[i + held].equals("-")) {
                held++;
            }
            double dur = stepSec * held * gate;
            render(buf, tone().at(startSec + i * stepSec).len(dur).freq(note(tok))
                    .wave(wave).duty(duty).amp(amp).pan(pan)
                    .env(0.006, Math.min(0.04, dur * 0.4)));
            i += held;
        }
    }

    /** Noise percussion. "x" snare, "o" kick, "h" hat, "." rest. */
    static void drums(Buf buf, double startSec, double stepSec, String pattern, double amp) {
        String[] tokens = pattern.trim().split("\\s+");
        for (int i = 0; i < tokens.length; i++) {
            double at = startSec + i * stepSec;
            switch (tokens[i]) {
                case "x" -> render(buf, tone().at(at).len(0.12).freq(5200).wave(NOISE)
                        .amp(amp * 0.85).perc(15.0));
                case "o" -> {
                    render(buf, tone().at(at).len(0.13).freq(150).sweep(48)
                            .wave(TRIANGLE).amp(amp * 1.3).perc(11.0));
                    render(buf, tone().at(at).len(0.05).freq(1400).wave(NOISE)
                            .amp(amp * 0.30).perc(28.0));
                }
                case "h" -> render(buf, tone().at(at).len(0.045).freq(11000).wave(NOISE)
                        .amp(amp * 0.34).perc(42.0));
                default -> { }
            }
        }
    }

    // ---------------------------------------------------------------- output

    /** Peak-normalises to {@code peak}, then writes 16-bit PCM WAV. */
    static void write(Path out, Buf buf, boolean stereo, double peak) throws IOException {
        double max = 1e-9;
        for (int i = 0; i < buf.n; i++) {
            max = Math.max(max, Math.abs(buf.left[i]));
            if (stereo) {
                max = Math.max(max, Math.abs(buf.right[i]));
            }
        }
        double gain = peak / max;

        int channels = stereo ? 2 : 1;
        int dataBytes = buf.n * channels * 2;
        ByteBuffer bb = ByteBuffer.allocate(44 + dataBytes).order(ByteOrder.LITTLE_ENDIAN);
        bb.put("RIFF".getBytes()).putInt(36 + dataBytes).put("WAVE".getBytes());
        bb.put("fmt ".getBytes()).putInt(16).putShort((short) 1).putShort((short) channels)
          .putInt(SR).putInt(SR * channels * 2).putShort((short) (channels * 2)).putShort((short) 16);
        bb.put("data".getBytes()).putInt(dataBytes);

        for (int i = 0; i < buf.n; i++) {
            if (stereo) {
                bb.putShort(clip(buf.left[i] * gain));
                bb.putShort(clip(buf.right[i] * gain));
            } else {
                // Mono: fold the pair down so panned layers survive the mixdown.
                bb.putShort(clip((buf.left[i] + buf.right[i]) * 0.5 * gain));
            }
        }
        Files.createDirectories(out.getParent());
        Files.write(out, bb.array());
        System.out.printf(Locale.ROOT, "  %-30s %6.2fs %s%n",
                out.getFileName(), buf.n / (double) SR, stereo ? "stereo" : "mono");
    }

    static short clip(double v) {
        double s = Math.max(-1.0, Math.min(1.0, v));
        return (short) Math.round(s * 32767.0);
    }

    // ================================================================= sound effects

    static Buf coin() {
        Buf b = new Buf(0.42);
        // Two-note flick: short grace note, then a fifth above held and ringing out.
        render(b, tone().at(0.0).len(0.055).freq(note("B5")).wave(PULSE).duty(0.5).amp(0.34));
        render(b, tone().at(0.055).len(0.34).freq(note("F#6")).wave(PULSE).duty(0.5)
                .amp(0.34).perc(5.5));
        return b;
    }

    static Buf powerUp() {
        Buf b = new Buf(0.72);
        // Rising run through a stacked-fourths shape, doubled an octave down.
        String[] run = {"D4", "G4", "C5", "F5", "A5", "D6", "G6"};
        for (int i = 0; i < run.length; i++) {
            double at = i * 0.075;
            render(b, tone().at(at).len(0.16).freq(note(run[i])).wave(PULSE).duty(0.25)
                    .amp(0.26).perc(9.0));
            render(b, tone().at(at).len(0.16).freq(note(run[i]) / 2.0).wave(TRIANGLE)
                    .amp(0.18).perc(9.0));
        }
        render(b, tone().at(0.53).len(0.19).freq(note("B6")).wave(PULSE).duty(0.5)
                .amp(0.30).perc(7.0));
        return b;
    }

    static Buf brickBreak() {
        Buf b = new Buf(0.36);
        // Crack, then tumbling debris.
        render(b, tone().at(0.0).len(0.20).freq(9000).sweep(1200).wave(NOISE).amp(0.5).perc(14.0));
        render(b, tone().at(0.0).len(0.14).freq(220).sweep(70).wave(TRIANGLE).amp(0.42).perc(13.0));
        for (int i = 0; i < 4; i++) {
            render(b, tone().at(0.09 + i * 0.055).len(0.07).freq(3400 - i * 520)
                    .wave(NOISE).amp(0.16).perc(26.0));
        }
        return b;
    }

    static Buf questionBump() {
        Buf b = new Buf(0.20);
        // Hollow knock with a quick upward bend, like a struck box.
        render(b, tone().at(0.0).len(0.16).freq(320).sweep(520).wave(PULSE).duty(0.125)
                .amp(0.40).perc(12.0));
        render(b, tone().at(0.0).len(0.06).freq(2600).wave(NOISE).amp(0.14).perc(30.0));
        return b;
    }

    static Buf stomp() {
        Buf b = new Buf(0.26);
        render(b, tone().at(0.0).len(0.16).freq(420).sweep(58).wave(PULSE).duty(0.5)
                .amp(0.44).perc(15.0));
        render(b, tone().at(0.0).len(0.11).freq(2200).sweep(600).wave(NOISE).amp(0.26).perc(20.0));
        return b;
    }

    static Buf enemyDefeat() {
        Buf b = new Buf(0.46);
        // Flattening squash: pitch collapses while the pulse width narrows in steps.
        double[] duties = {0.5, 0.375, 0.25, 0.125};
        for (int i = 0; i < duties.length; i++) {
            render(b, tone().at(i * 0.085).len(0.11).freq(700 * Math.pow(0.62, i))
                    .wave(PULSE).duty(duties[i]).amp(0.32).perc(8.0));
        }
        render(b, tone().at(0.30).len(0.16).freq(1500).sweep(300).wave(NOISE).amp(0.18).perc(14.0));
        return b;
    }

    static Buf fireball() {
        Buf b = new Buf(0.34);
        render(b, tone().at(0.0).len(0.30).freq(1500).sweep(340).wave(NOISE).amp(0.40).perc(7.0));
        render(b, tone().at(0.0).len(0.22).freq(880).sweep(180).wave(SAW).amp(0.22).perc(9.0));
        return b;
    }

    static Buf iceshot() {
        Buf b = new Buf(0.46);
        // Glassy: high pulse with shimmer, plus a ringing fifth above.
        render(b, tone().at(0.0).len(0.40).freq(note("E6")).sweep(note("B5"))
                .wave(PULSE).duty(0.125).amp(0.30).vib(26.0, 34.0).perc(5.0));
        render(b, tone().at(0.02).len(0.34).freq(note("B6")).wave(PULSE).duty(0.0625)
                .amp(0.15).vib(19.0, 26.0).perc(6.5));
        render(b, tone().at(0.0).len(0.09).freq(12000).wave(NOISE).amp(0.10).perc(24.0));
        return b;
    }

    static Buf hammerThrow() {
        Buf b = new Buf(0.40);
        // Air being cut: band of noise sweeping up then away.
        render(b, tone().at(0.0).len(0.36).freq(700).sweep(4200).wave(NOISE).amp(0.34).perc(3.4));
        render(b, tone().at(0.0).len(0.28).freq(210).sweep(440).wave(TRIANGLE).amp(0.24).perc(5.0));
        return b;
    }

    static Buf boomerangThrow() {
        Buf b = new Buf(0.62);
        // Spin: deep vibrato on a narrow pulse so it warbles as it travels.
        render(b, tone().at(0.0).len(0.58).freq(note("A4")).wave(PULSE).duty(0.25)
                .amp(0.30).vib(17.0, 260.0).perc(2.8));
        render(b, tone().at(0.0).len(0.50).freq(1800).wave(NOISE).amp(0.09).perc(4.0));
        return b;
    }

    static Buf gameOver() {
        Buf b = new Buf(2.10);
        // Original descending minor cadence: Dm - Bb - Gm - A, ending unresolved.
        String[] lead = {"D5", "C5", "Bb4", "A4", "G4", "F4", "E4", "D4"};
        for (int i = 0; i < lead.length; i++) {
            double at = i * 0.20;
            double dur = (i == lead.length - 1) ? 0.72 : 0.19;
            render(b, tone().at(at).len(dur).freq(note(lead[i])).wave(PULSE).duty(0.5)
                    .amp(0.26).env(0.006, 0.05));
            render(b, tone().at(at).len(dur).freq(note(lead[i]) / 2.0).wave(TRIANGLE)
                    .amp(0.22).env(0.006, 0.05));
        }
        render(b, tone().at(1.60).len(0.48).freq(note("A3")).wave(PULSE).duty(0.25)
                .amp(0.18).perc(3.2));
        return b;
    }

    static Buf courseClear() {
        Buf b = new Buf(2.60);
        // Original fanfare: rising triad calls answered by a held major sixth.
        String[] calls = {"C5", "E5", "G5", "C6", "G5", "C6", "E6"};
        double[] times = {0.00, 0.13, 0.26, 0.39, 0.60, 0.73, 0.86};
        for (int i = 0; i < calls.length; i++) {
            render(b, tone().at(times[i]).len(0.16).freq(note(calls[i])).wave(PULSE).duty(0.5)
                    .amp(0.26).env(0.005, 0.04));
            render(b, tone().at(times[i]).len(0.16).freq(note(calls[i]) / 2.0).wave(PULSE)
                    .duty(0.25).amp(0.14).env(0.005, 0.04));
        }
        String[] chord = {"C5", "E5", "G5", "A5"};
        for (String c : chord) {
            render(b, tone().at(1.10).len(1.35).freq(note(c)).wave(PULSE).duty(0.5)
                    .amp(0.16).env(0.02, 0.45));
        }
        render(b, tone().at(1.10).len(1.35).freq(note("C3")).wave(TRIANGLE).amp(0.28)
                .env(0.02, 0.45));
        drums(b, 1.10, 0.13, "x . h . x . h . o", 0.5);
        return b;
    }

    static Buf checkpoint() {
        Buf b = new Buf(0.80);
        // Bright two-note confirmation, fourth then octave, with a soft bell tail.
        render(b, tone().at(0.0).len(0.14).freq(note("G5")).wave(PULSE).duty(0.5).amp(0.30));
        render(b, tone().at(0.13).len(0.62).freq(note("C6")).wave(PULSE).duty(0.5)
                .amp(0.30).perc(3.6));
        render(b, tone().at(0.13).len(0.62).freq(note("E6")).wave(PULSE).duty(0.25)
                .amp(0.15).perc(4.2));
        return b;
    }

    static Buf damage() {
        Buf b = new Buf(0.40);
        // Harsh: detuned pair falling together, so it beats against itself.
        render(b, tone().at(0.0).len(0.34).freq(600).sweep(190).wave(PULSE).duty(0.125)
                .amp(0.34).perc(6.0));
        render(b, tone().at(0.0).len(0.34).freq(618).sweep(196).wave(PULSE).duty(0.125)
                .amp(0.30).perc(6.0));
        render(b, tone().at(0.0).len(0.12).freq(3000).wave(NOISE).amp(0.20).perc(18.0));
        return b;
    }

    static Buf oneUp() {
        Buf b = new Buf(1.05);
        // Cheerful original lift: pentatonic climb settling on the octave.
        String[] run = {"E5", "G5", "A5", "C6", "D6", "E6"};
        for (int i = 0; i < run.length; i++) {
            render(b, tone().at(i * 0.105).len(0.14).freq(note(run[i])).wave(PULSE).duty(0.5)
                    .amp(0.28).env(0.005, 0.04));
        }
        render(b, tone().at(0.63).len(0.40).freq(note("A6")).wave(PULSE).duty(0.25)
                .amp(0.26).perc(4.5));
        render(b, tone().at(0.63).len(0.40).freq(note("A4")).wave(TRIANGLE).amp(0.22).perc(4.5));
        return b;
    }

    static Buf spring() {
        Buf b = new Buf(0.40);
        // Boing: fast rise with a wobble that settles as it goes.
        render(b, tone().at(0.0).len(0.36).freq(180).sweep(1500).wave(PULSE).duty(0.25)
                .amp(0.36).vib(21.0, 130.0).perc(4.5));
        render(b, tone().at(0.0).len(0.10).freq(1600).wave(NOISE).amp(0.10).perc(22.0));
        return b;
    }

    static Buf warp() {
        Buf b = new Buf(0.85);
        // Pipe travel: descending wobble, then a rebound up and out.
        render(b, tone().at(0.0).len(0.48).freq(1300).sweep(210).wave(PULSE).duty(0.5)
                .amp(0.30).vib(13.0, 90.0).perc(2.6));
        render(b, tone().at(0.44).len(0.40).freq(240).sweep(1700).wave(PULSE).duty(0.125)
                .amp(0.26).vib(16.0, 70.0).perc(3.4));
        render(b, tone().at(0.0).len(0.80).freq(90).wave(TRIANGLE).amp(0.20).perc(3.0));
        return b;
    }

    static Buf bowserRoar() {
        Buf b = new Buf(1.60);
        // Big and detuned: three saws a few cents apart over a growling noise bed.
        double[] detune = {1.0, 1.008, 0.993};
        for (double d : detune) {
            render(b, tone().at(0.0).len(1.50).freq(78 * d).sweep(46 * d).wave(SAW)
                    .amp(0.20).vib(7.5, 55.0).env(0.05, 0.35));
        }
        render(b, tone().at(0.0).len(1.45).freq(430).sweep(150).wave(NOISE).amp(0.24)
                .vib(5.0, 200.0).env(0.06, 0.40));
        render(b, tone().at(0.0).len(1.55).freq(39).wave(TRIANGLE).amp(0.30).env(0.04, 0.40));
        return b;
    }

    // ================================================================= music

    /**
     * All four tracks are built the same way: a lead pulse, a harmony pulse panned
     * opposite, a triangle bass and a noise kit. Bars are 16 steps of a sixteenth note,
     * and every track loops cleanly because the last bar leads back to the first.
     */

    static Buf musicHub() {
        double bpm = 104.0;
        double step = 60.0 / bpm / 4.0;
        double bar = step * 16;
        Buf b = new Buf(bar * 16 + 0.3);

        // Warm and unhurried: I - vi - IV - V in F major, two bars per chord.
        String[] leadBars = {
            "F5 . A5 . C6 . A5 . F5 . . . C6 . . .",
            "A5 . . . G5 . F5 . E5 . . . . . . .",
            "D5 . F5 . A5 . F5 . D5 . . . A5 . . .",
            "C6 . . . A5 . G5 . F5 . . . . . . .",
            "Bb4 . D5 . F5 . D5 . Bb4 . . . F5 . . .",
            "D5 . . . C5 . Bb4 . A4 . . . . . . .",
            "C5 . E5 . G5 . E5 . C5 . . . G5 . . .",
            "E5 . . . F5 . G5 . A5 . . . C6 . . ."
        };
        String[] bassBars = {
            "F2 . . . F2 . . . C3 . . . F2 . . .",
            "F2 . . . A2 . . . C3 . . . A2 . . .",
            "D2 . . . D2 . . . A2 . . . D2 . . .",
            "D2 . . . F2 . . . A2 . . . F2 . . .",
            "Bb1 . . . Bb1 . . . F2 . . . Bb1 . . .",
            "Bb1 . . . D2 . . . F2 . . . D2 . . .",
            "C2 . . . C2 . . . G2 . . . C2 . . .",
            "C2 . . . E2 . . . G2 . . . Bb2 . . ."
        };
        String kit = "o . h . x . h . o . h . x . h h";

        for (int i = 0; i < 16; i++) {
            double t = i * bar;
            seq(b, t, step, leadBars[i % 8], PULSE, 0.5, 0.16, -0.25, 0.90);
            seq(b, t, step, bassBars[i % 8], TRIANGLE, 0.5, 0.26, 0.0, 0.95);
            // Harmony enters on the second half so the loop opens up.
            if (i >= 4) {
                seq(b, t + step * 2, step, leadBars[i % 8], PULSE, 0.25, 0.085, 0.30, 0.80);
            }
            drums(b, t, step, kit, i >= 2 ? 0.34 : 0.18);
        }
        return b;
    }

    static Buf musicCourse2_5D() {
        double bpm = 152.0;
        double step = 60.0 / bpm / 4.0;
        double bar = step * 16;
        Buf b = new Buf(bar * 16 + 0.3);

        // Driving side-scroll feel: A minor with a running sixteenth bass.
        String[] leadBars = {
            "A5 . A5 . C6 . B5 . A5 . G5 . E5 . . .",
            "F5 . G5 . A5 . . . G5 . E5 . D5 . . .",
            "C6 . C6 . E6 . D6 . C6 . B5 . G5 . . .",
            "A5 . B5 . C6 . . . B5 . G5 . A5 . . .",
            "D6 . . . C6 . B5 . A5 . . . G5 . . .",
            "E5 . G5 . A5 . B5 . C6 . . . B5 . . .",
            "A5 . E5 . A5 . C6 . E6 . . . D6 . . .",
            "C6 . B5 . A5 . G5 . E5 . D5 . C5 . . ."
        };
        String[] bassBars = {
            "A2 A2 A3 A2 A2 A2 A3 A2 A2 A2 A3 A2 A2 A2 A3 A2",
            "F2 F2 F3 F2 F2 F2 F3 F2 G2 G2 G3 G2 G2 G2 G3 G2",
            "C3 C3 C4 C3 C3 C3 C4 C3 C3 C3 C4 C3 C3 C3 C4 C3",
            "F2 F2 F3 F2 F2 F2 F3 F2 E2 E2 E3 E2 E2 E2 E3 E2",
            "D2 D2 D3 D2 D2 D2 D3 D2 D2 D2 D3 D2 D2 D2 D3 D2",
            "C3 C3 C4 C3 C3 C3 C4 C3 G2 G2 G3 G2 G2 G2 G3 G2",
            "A2 A2 A3 A2 A2 A2 A3 A2 A2 A2 A3 A2 A2 A2 A3 A2",
            "F2 F2 F3 F2 G2 G2 G3 G2 E2 E2 E3 E2 E2 E2 E3 E2"
        };
        String kit = "o . h h x . h . o . h h x . h h";
        String kitFill = "o . h h x . h . o . h h x x x x";

        for (int i = 0; i < 16; i++) {
            double t = i * bar;
            seq(b, t, step, leadBars[i % 8], PULSE, 0.25, 0.155, -0.28, 0.88);
            seq(b, t, step, bassBars[i % 8], TRIANGLE, 0.5, 0.24, 0.0, 0.75);
            if (i >= 2) {
                // Harmony a sixth below, quieter and off to the other side.
                seq(b, t, step, leadBars[i % 8], PULSE, 0.125, 0.070, 0.32, 0.70);
            }
            drums(b, t, step, (i % 8 == 7) ? kitFill : kit, 0.36);
        }
        return b;
    }

    static Buf musicCourse3D() {
        double bpm = 118.0;
        double step = 60.0 / bpm / 4.0;
        double bar = step * 16;
        Buf b = new Buf(bar * 16 + 0.4);

        // Open and exploratory: wide leaps, suspended chords, D mixolydian.
        String[] leadBars = {
            "D5 . . . A5 . . . G5 . . . E5 . . .",
            "F#5 . . . D6 . . . A5 . . . . . . .",
            "C6 . . . G5 . . . A5 . . . D5 . . .",
            "E5 . . . A5 . . . B5 . . . . . . .",
            "G5 . . . D6 . . . C6 . . . A5 . . .",
            "B5 . . . G5 . . . E5 . . . . . . .",
            "A5 . . . E6 . . . D6 . . . B5 . . .",
            "G5 . . . A5 . . . D6 . . . . . . ."
        };
        String[] padBars = {
            "A4 - - - - - - - G4 - - - - - - -",
            "A4 - - - - - - - D5 - - - - - - -",
            "E5 - - - - - - - C5 - - - - - - -",
            "A4 - - - - - - - B4 - - - - - - -",
            "B4 - - - - - - - G4 - - - - - - -",
            "D5 - - - - - - - B4 - - - - - - -",
            "C#5 - - - - - - - A4 - - - - - - -",
            "B4 - - - - - - - A4 - - - - - - -"
        };
        String[] bassBars = {
            "D2 . . . . . . . G2 . . . . . . .",
            "D2 . . . . . . . A2 . . . . . . .",
            "C2 . . . . . . . G2 . . . . . . .",
            "A2 . . . . . . . E2 . . . . . . .",
            "G2 . . . . . . . D2 . . . . . . .",
            "G2 . . . . . . . E2 . . . . . . .",
            "A2 . . . . . . . F#2 . . . . . . .",
            "G2 . . . . . . . A2 . . . . . . ."
        };
        String kit = "o . . . x . . . . . o . x . . h";

        for (int i = 0; i < 16; i++) {
            double t = i * bar;
            seq(b, t, step, leadBars[i % 8], PULSE, 0.5, 0.145, -0.20, 0.95);
            seq(b, t, step, padBars[i % 8], PULSE, 0.125, 0.055, 0.35, 0.98);
            seq(b, t, step, bassBars[i % 8], TRIANGLE, 0.5, 0.28, 0.0, 0.92);
            drums(b, t, step, kit, i >= 4 ? 0.28 : 0.14);
        }
        return b;
    }

    static Buf musicCombat() {
        double bpm = 168.0;
        double step = 60.0 / bpm / 4.0;
        double bar = step * 16;
        Buf b = new Buf(bar * 16 + 0.3);

        // Tense: chromatic descent over a pedal, E phrygian.
        String[] leadBars = {
            "E5 . F5 . E5 . F5 . G5 . F5 . E5 . . .",
            "B5 . Bb5 . A5 . Ab5 . G5 . . . F5 . . .",
            "E5 . E5 . G5 . F5 . E5 . D5 . C5 . . .",
            "F5 . . . E5 . . . D#5 . . . E5 . . .",
            "C6 . B5 . Bb5 . A5 . G5 . . . E5 . . .",
            "A5 . Ab5 . G5 . F#5 . F5 . . . E5 . . .",
            "E6 . . . D6 . . . C6 . . . B5 . . .",
            "G5 . F5 . E5 . D5 . C5 . B4 . E5 . . ."
        };
        String[] bassBars = {
            "E2 E2 E2 . E2 E2 E2 . F2 F2 F2 . E2 E2 E2 .",
            "E2 E2 E2 . E2 E2 E2 . G2 G2 G2 . F2 F2 F2 .",
            "E2 E2 E2 . E2 E2 E2 . C2 C2 C2 . B1 B1 B1 .",
            "F2 F2 F2 . F2 F2 F2 . E2 E2 E2 . E2 E2 E2 .",
            "C2 C2 C2 . C2 C2 C2 . G2 G2 G2 . E2 E2 E2 .",
            "A2 A2 A2 . A2 A2 A2 . F2 F2 F2 . E2 E2 E2 .",
            "E2 E2 E2 . E2 E2 E2 . C2 C2 C2 . B1 B1 B1 .",
            "G2 G2 G2 . F2 F2 F2 . E2 E2 E2 . E2 E2 E2 ."
        };
        String kit = "o . h o . h x . o . h o . h x x";

        for (int i = 0; i < 16; i++) {
            double t = i * bar;
            seq(b, t, step, leadBars[i % 8], PULSE, 0.125, 0.150, -0.30, 0.85);
            // Tritone-shadowed second voice: dissonant on purpose, well under the lead.
            seq(b, t, step, leadBars[i % 8], PULSE, 0.25, 0.060, 0.34, 0.60);
            seq(b, t, step, bassBars[i % 8], TRIANGLE, 0.5, 0.26, 0.0, 0.80);
            drums(b, t, step, kit, 0.40);
        }
        return b;
    }


    /**
     * Boss music: slow, heavy and harmonically unstable.
     *
     * <p>Deliberately the opposite of every other track here. Where the course themes are bright
     * pulse leads over a walking bass, this is a low ostinato with a tritone sitting in it that
     * never resolves, so the arena feels like somewhere the player should not be standing. The
     * lead is sparse: the bass and the drums carry it, and the space between phrases is the point.
     */
    static Buf musicBoss() {
        double bpm = 96.0;
        double step = 60.0 / bpm / 4.0;
        double bar = step * 16;
        Buf b = new Buf(bar * 16 + 0.4);

        // D locrian-ish: the flattened fifth (Ab) is the whole character.
        String[] leadBars = {
            "D4 . . . Ab4 . . . D4 . . . . . . .",
            ". . . . F4 . Eb4 . D4 . . . . . . .",
            "Bb4 . . . Ab4 . . . G4 . . . . . . .",
            "F4 . Eb4 . D4 . . . . . . . . . . .",
            "D5 . . . Ab4 . . . Bb4 . . . . . . .",
            "C5 . Bb4 . Ab4 . . . G4 . . . . . . .",
            "Eb5 . . . D5 . . . Ab4 . . . . . . .",
            "F4 . . . Eb4 . . . D4 . . . . . . ."
        };
        String[] bassBars = {
            "D1 . D1 . . . D1 . Ab1 . . . D1 . . .",
            "D1 . D1 . . . D1 . Eb1 . . . D1 . . .",
            "Bb1 . Bb1 . . . Bb1 . Ab1 . . . G1 . . .",
            "D1 . D1 . . . Ab1 . D1 . . . . . . .",
            "D1 . D1 . . . D1 . Bb1 . . . D1 . . .",
            "Ab1 . Ab1 . . . Ab1 . G1 . . . D1 . . .",
            "Eb1 . Eb1 . . . Eb1 . D1 . . . Ab1 . . .",
            "D1 . D1 . Ab1 . Ab1 . D1 . . . . . . ."
        };
        String kit = "o . . . o . . x o . . . x . x x";

        for (int i = 0; i < 16; i++) {
            double t = i * bar;
            // A narrow pulse for the lead: thin and reedy, so it cuts without sounding heroic.
            seq(b, t, step, leadBars[i % 8], PULSE, 0.125, 0.130, -0.18, 0.90);
            seq(b, t, step, bassBars[i % 8], TRIANGLE, 0.5, 0.34, 0.0, 0.75);
            // The tritone shadow, quiet and wide. It is what makes the loop feel wrong.
            if (i >= 2) {
                seq(b, t, step, leadBars[i % 8], PULSE, 0.5, 0.045, 0.40, 0.70);
            }
            drums(b, t, step, kit, i >= 4 ? 0.46 : 0.30);
        }
        return b;
    }

    /**
     * Star Power: the fastest thing in the mod, and short on purpose.
     *
     * <p>Invincibility music has one job — tell the player the clock is running. A long loop
     * would hide that, so this is a tight cycle that comes round often enough to feel like it is
     * counting down, with a rising chromatic run that resets every bar.
     */
    static Buf musicStarPower() {
        double bpm = 200.0;
        double step = 60.0 / bpm / 4.0;
        double bar = step * 16;
        Buf b = new Buf(bar * 8 + 0.3);

        String[] leadBars = {
            "C5 E5 G5 C6 G5 E5 C5 E5 G5 C6 E6 C6 G5 E5 C5 G4",
            "D5 F5 A5 D6 A5 F5 D5 F5 A5 D6 F6 D6 A5 F5 D5 A4",
            "E5 G5 B5 E6 B5 G5 E5 G5 B5 E6 G6 E6 B5 G5 E5 B4",
            "F5 A5 C6 F6 C6 A5 F5 A5 C6 F6 A6 F6 C6 A5 F5 C5"
        };
        String[] bassBars = {
            "C2 . C2 . G1 . G1 . C2 . C2 . G1 . G1 .",
            "D2 . D2 . A1 . A1 . D2 . D2 . A1 . A1 .",
            "E2 . E2 . B1 . B1 . E2 . E2 . B1 . B1 .",
            "F2 . F2 . C2 . C2 . F2 . F2 . C2 . C2 ."
        };
        String kit = "o h x h o h x h o h x h o h x x";

        for (int i = 0; i < 8; i++) {
            double t = i * bar;
            seq(b, t, step, leadBars[i % 4], PULSE, 0.25, 0.135, -0.22, 0.80);
            seq(b, t, step, leadBars[i % 4], PULSE, 0.5, 0.055, 0.36, 0.60);
            seq(b, t, step, bassBars[i % 4], TRIANGLE, 0.5, 0.28, 0.0, 0.85);
            drums(b, t, step, kit, 0.42);
        }
        return b;
    }

    /**
     * Toad's thank-you fanfare, played after a castle clear while he speaks.
     *
     * <p>A one-shot rather than a loop: it plays once under the dialogue and stops. Ends on the
     * tonic so the silence afterwards reads as resolution rather than a cut.
     */
    static Buf toadFanfare() {
        double bpm = 120.0;
        double step = 60.0 / bpm / 4.0;
        Buf b = new Buf(step * 40 + 0.6);

        String lead = "G4 . C5 . E5 . G5 . C6 . . . E6 . . . C6 - - - . . . . G5 - - - C6 - - - - - - -";
        String harmony = "E4 . G4 . C5 . E5 . G5 . . . C6 . . . G5 - - - . . . . E5 - - - G5 - - - - - - -";
        String bass = "C3 . . . C3 . . . G2 . . . G2 . . . C3 . . . . . . . G2 . . . C3 - - - - - - -";

        seq(b, 0, step, lead, PULSE, 0.5, 0.170, -0.16, 0.94);
        seq(b, 0, step, harmony, PULSE, 0.25, 0.085, 0.28, 0.94);
        seq(b, 0, step, bass, TRIANGLE, 0.5, 0.26, 0.0, 0.90);
        return b;
    }

    // ================================================================= main

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("usage: java SoundGen.java <outDir>");
            System.exit(2);
        }
        Path out = Path.of(args[0]);

        System.out.println("sfx:");
        write(out.resolve("coin.wav"), coin(), false, 0.80);
        write(out.resolve("power_up.wav"), powerUp(), false, 0.80);
        write(out.resolve("brick_break.wav"), brickBreak(), false, 0.85);
        write(out.resolve("question_bump.wav"), questionBump(), false, 0.80);
        write(out.resolve("stomp.wav"), stomp(), false, 0.85);
        write(out.resolve("enemy_defeat.wav"), enemyDefeat(), false, 0.82);
        write(out.resolve("fireball.wav"), fireball(), false, 0.78);
        write(out.resolve("iceshot.wav"), iceshot(), false, 0.78);
        write(out.resolve("hammer_throw.wav"), hammerThrow(), false, 0.76);
        write(out.resolve("boomerang_throw.wav"), boomerangThrow(), false, 0.76);
        write(out.resolve("game_over.wav"), gameOver(), false, 0.82);
        write(out.resolve("course_clear.wav"), courseClear(), false, 0.84);
        write(out.resolve("checkpoint.wav"), checkpoint(), false, 0.80);
        write(out.resolve("damage.wav"), damage(), false, 0.84);
        write(out.resolve("1up.wav"), oneUp(), false, 0.80);
        write(out.resolve("spring.wav"), spring(), false, 0.82);
        write(out.resolve("warp.wav"), warp(), false, 0.80);
        write(out.resolve("bowser_roar.wav"), bowserRoar(), false, 0.88);
        write(out.resolve("toad_fanfare.wav"), toadFanfare(), false, 0.84);

        System.out.println("music:");
        write(out.resolve("music_hub.wav"), musicHub(), true, 0.72);
        write(out.resolve("music_course_2_5d.wav"), musicCourse2_5D(), true, 0.74);
        write(out.resolve("music_course_3d.wav"), musicCourse3D(), true, 0.72);
        write(out.resolve("music_combat.wav"), musicCombat(), true, 0.74);
        write(out.resolve("music_boss.wav"), musicBoss(), true, 0.76);
        write(out.resolve("music_star_power.wav"), musicStarPower(), true, 0.74);
    }

    private SoundGen() {
    }
}
