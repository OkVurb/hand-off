package com.studio.planeshift.common.block;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Every pipe colour needs a variant, a model and two textures on disk.
 *
 * <p>A blockstate property with no matching variant does not fail: Minecraft renders the missing
 * model as the black-and-magenta placeholder and carries on. So the failure mode for getting this
 * wrong is a pipe that looks like a bug report, discovered by walking into one, and nothing in the
 * build says a word.
 *
 * <p>Checked against the enum rather than a hardcoded list, so adding a sixth colour and forgetting
 * its art fails here rather than in the world.
 */
class PipeColourTest {

    private static Path root() {
        Path dir = Path.of("").toAbsolutePath();
        while (dir != null && !Files.isDirectory(dir.resolve("src"))) {
            dir = dir.getParent();
        }
        if (dir == null) {
            throw new IllegalStateException("could not find the repo root");
        }
        return dir;
    }

    private static Path assets() {
        return root().resolve("src/main/resources/assets/planeshift");
    }

    @Test
    @DisplayName("the blockstate names every colour the enum declares")
    void blockstateCoversTheEnum() throws IOException {
        String json = Files.readString(assets().resolve("blockstates/warp_pipe.json"),
                StandardCharsets.UTF_8);
        List<String> missing = new ArrayList<>();
        for (WarpPipeBlock.Colour colour : WarpPipeBlock.Colour.values()) {
            if (!json.contains("colour=" + colour.getSerializedName())) {
                missing.add(colour.getSerializedName());
            }
        }
        assertTrue(missing.isEmpty(),
                "blockstate has no variant for " + missing
                        + "; those pipes render as the missing-model placeholder");
    }

    @Test
    @DisplayName("every colour has its model and both textures")
    void artExistsForEveryColour() {
        for (WarpPipeBlock.Colour colour : WarpPipeBlock.Colour.values()) {
            // Green is the original art and keeps the unsuffixed names.
            String stem = colour == WarpPipeBlock.Colour.GREEN
                    ? "warp_pipe" : "warp_pipe_" + colour.getSerializedName();
            assertTrue(Files.exists(assets().resolve("models/block/" + stem + ".json")),
                    "missing model for " + colour);
            assertTrue(Files.exists(assets().resolve("textures/block/" + stem + ".png")),
                    "missing side texture for " + colour);
            assertTrue(Files.exists(assets().resolve("textures/block/" + stem + "_top.png")),
                    "missing top texture for " + colour);
        }
    }

    @Test
    @DisplayName("green stays the default, so existing courses are unchanged")
    void greenIsFirst() {
        assertEquals(WarpPipeBlock.Colour.GREEN, WarpPipeBlock.Colour.values()[0]);
    }
}
