package com.studio.planeshift.common.block;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
 * Keeps the three places that describe a connected block from drifting apart.
 *
 * <p>A connected block is spelled out in three unrelated languages: {@link ConnectedBlock#mask} in
 * Java says which bit means which side, {@code BlockTextureGen.CONNECTED} in Python draws sixteen
 * textures against that numbering, and {@code ConnectedAssets.BLOCKS} writes the JSON that maps a
 * blockstate onto one of them. Nothing in the build makes them agree. Get the bit order wrong in
 * one of them and the game does not fail — it renders a wall with the shadows on the wrong sides,
 * which is a bug you find by squinting rather than by running anything.
 *
 * <p>So this test reads the two Python files as text and checks them against the Java. It is an
 * unusual thing for a unit test to do, and it is the only place the contract can actually be
 * checked, because two thirds of it live outside the JVM.
 */
class ConnectedAssetsTest {

    /**
     * Repo root, found by walking up from the working directory until {@code tools/} appears.
     *
     * <p>Gradle runs tests with the module directory as the working directory, not the repo root,
     * and this test is unusual in reading files that live outside the source set at all.
     */
    private static final Path ROOT = findRoot();
    private static final Path TOOLS = ROOT.resolve("tools");
    private static final Path ASSETS = ROOT.resolve("src/main/resources/assets/planeshift");

    private static Path findRoot() {
        Path dir = Path.of("").toAbsolutePath();
        while (dir != null && !Files.isDirectory(dir.resolve("tools"))) {
            dir = dir.getParent();
        }
        if (dir == null) {
            throw new IllegalStateException("could not find repo root (no tools/ above "
                    + Path.of("").toAbsolutePath() + ")");
        }
        return dir;
    }

    private static String read(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    /** The block names listed in {@code ConnectedAssets.BLOCKS}. */
    private static List<String> assetBlocks() throws IOException {
        String src = read(TOOLS.resolve("ConnectedAssets.py"));
        String body = src.substring(src.indexOf("BLOCKS = ["), src.indexOf("]", src.indexOf("BLOCKS = [")));
        return quoted(body);
    }

    /** The block names keyed in {@code BlockTextureGen.CONNECTED}. */
    private static List<String> textureBlocks() throws IOException {
        String src = read(TOOLS.resolve("BlockTextureGen.py"));
        int start = src.indexOf("CONNECTED = {");
        int end = src.indexOf("\n}", start);
        String body = src.substring(start, end);
        List<String> names = new ArrayList<>();
        Matcher m = Pattern.compile("\"(course_[a-z_]+)\":").matcher(body);
        while (m.find()) {
            names.add(m.group(1));
        }
        return names;
    }

    private static List<String> quoted(String body) {
        List<String> out = new ArrayList<>();
        Matcher m = Pattern.compile("\"(course_[a-z_]+)\"").matcher(body);
        while (m.find()) {
            out.add(m.group(1));
        }
        return out;
    }

    @Test
    @DisplayName("every block with generated textures also has generated blockstate JSON")
    void listsAgree() throws IOException {
        List<String> textures = textureBlocks();
        List<String> assets = assetBlocks();
        assertTrue(!textures.isEmpty(), "found no connected blocks in BlockTextureGen");
        assertEquals(textures.stream().sorted().toList(), assets.stream().sorted().toList(),
                "BlockTextureGen.CONNECTED and ConnectedAssets.BLOCKS disagree; a block in one "
                        + "and not the other renders as a missing texture or a stale flat model");
    }

    @Test
    @DisplayName("the Python bit order is the one ConnectedBlock.mask uses")
    void bitOrderMatches() throws IOException {
        String assetsSrc = read(TOOLS.resolve("ConnectedAssets.py"));
        // BITS is the emitter's copy of the contract. Written out longhand rather than parsed
        // loosely, because a test that accepts a near-miss here is worth nothing.
        assertTrue(assetsSrc.contains("BITS = [(\"up\", 1), (\"down\", 2), (\"west\", 4), (\"east\", 8)]"),
                "ConnectedAssets.BITS no longer matches ConnectedBlock.mask (up=1, down=2, "
                        + "west=4, east=8)");
    }

    @Test
    @DisplayName("each connected block has all sixteen models and a blockstate on disk")
    void filesExist() throws IOException {
        for (String name : assetBlocks()) {
            assertTrue(Files.exists(ASSETS.resolve("blockstates").resolve(name + ".json")),
                    "missing blockstate for " + name);
            for (int mask = 0; mask < 16; mask++) {
                Path model = ASSETS.resolve("models/block").resolve(name + "_" + mask + ".json");
                assertTrue(Files.exists(model), "missing model " + model);
                Path texture = ASSETS.resolve("textures/block").resolve(name + "_" + mask + ".png");
                assertTrue(Files.exists(texture), "missing texture " + texture);
            }
        }
    }
}
