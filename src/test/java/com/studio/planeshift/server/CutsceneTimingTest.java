package com.studio.planeshift.server;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A scene is beats separated by silence, and the silence is the part that can be lost.
 *
 * <p>The castle clear used to fire the card, the fanfare and Toad in one tick, which is a
 * notification with three parts. What makes it a scene is the pause after the boss falls and the
 * pause after the card — and those pauses are the easiest thing in the world for someone to tune
 * away, one beat at a time, until it is a notification again.
 *
 * <p>So this reads the beat offsets out of the source and asserts they are actually spaced. It
 * cannot run the scene: that needs a player and a level.
 */
class CutsceneTimingTest {

    private static final Path SOURCE = findRoot().resolve(
            "src/main/java/com/studio/planeshift/server/CutsceneService.java");

    private static Path findRoot() {
        Path dir = Path.of("").toAbsolutePath();
        while (dir != null && !Files.isDirectory(dir.resolve("tools"))) {
            dir = dir.getParent();
        }
        if (dir == null) {
            throw new IllegalStateException("could not find repo root");
        }
        return dir;
    }

    private static List<Integer> beats() throws IOException {
        String source = Files.readString(SOURCE, StandardCharsets.UTF_8);
        List<Integer> offsets = new ArrayList<>();
        Matcher matcher = Pattern.compile("new Beat[(]([0-9]+),").matcher(source);
        while (matcher.find()) {
            offsets.add(Integer.parseInt(matcher.group(1)));
        }
        return offsets;
    }

    @Test
    @DisplayName("the castle scene has more than one moment in it")
    void thereAreBeats() throws IOException {
        List<Integer> beats = beats();
        assertTrue(beats.size() >= 4,
                "a scene with " + beats.size() + " beats is a notification with parts");
    }

    @Test
    @DisplayName("and they are spaced far enough apart to read as separate")
    void theSilencesSurvive() throws IOException {
        List<Integer> beats = beats();
        int longest = 0;
        for (int i = 1; i < beats.size(); i++) {
            longest = Math.max(longest, beats.get(i) - beats.get(i - 1));
        }
        // One gap of at least a second. Without a pause somewhere the beats blur into each other
        // and the sequencing has bought nothing.
        assertTrue(longest >= 20,
                "the longest silence in the scene is " + longest + " ticks; at that spacing the "
                        + "beats blur together and this is a notification again");
    }

    @Test
    @DisplayName("and the bars come up before the scene runs out")
    void theGameIsHandedBack() throws IOException {
        List<Integer> beats = beats();
        String source = Files.readString(SOURCE, StandardCharsets.UTF_8);
        Matcher matcher = Pattern.compile("letterbox[(]p, ([0-9]+)[)]").matcher(source);
        assertTrue(matcher.find(), "the scene never lowers the bars");
        int held = Integer.parseInt(matcher.group(1));
        int last = beats.get(beats.size() - 1);
        assertTrue(last <= held,
                "the last beat is at " + last + " but the bars come up at " + held + "; the scene "
                        + "would end with the player locked in place and nothing happening");
    }
}
