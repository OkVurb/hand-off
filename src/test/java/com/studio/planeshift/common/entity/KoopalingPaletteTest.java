package com.studio.planeshift.common.entity;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

/**
 * You can tell the eight tower bosses apart.
 *
 * <p>They share one rig, one hitbox and one silhouette, so colour is the entire identity. If two of
 * them read the same, the game has one boss with two names.
 *
 * <p>This measures the shipped PNGs rather than a table of colours in Java, deliberately. The
 * palette lives in {@code tools/EnemyTextureGen.py} and reaches the game as an image; a second copy
 * of it in Java would be a rival source of truth, which in this codebase is how
 * {@code CameraProfile.damping} and the {@code cameraSmoothing} config option both ended up dead —
 * two controls for one quantity, neither applied, unable to contradict each other. The sheet is
 * what the player looks at, so the sheet is what gets tested.
 *
 * <p>It has already earned itself three times. The first render put Larry and Ludwig 19 units
 * apart (both canonically blue-haired), Morton and Bowser Jr both white, and — found only by
 * checking every pair rather than the two suspected ones — Roy and Wendy 32 apart, both pink. All
 * three are faithful to the source material and all three were unusable at the distance this game
 * is played from.
 */
class KoopalingPaletteTest {

    /**
     * Where the crest is painted.
     *
     * <p>The trim region starts at (64, 80) on the 128x128 sheet, per the face contract every rig
     * shares. A few pixels in, to sample the material rather than its edge shading.
     */
    private static final int HAIR_X = 70;
    private static final int HAIR_Y = 86;

    /**
     * Minimum separation between any two crests, as plain RGB distance.
     *
     * <p>Sixty. The pairs that actually failed sat at 19 and 32, and the current worst pair is at
     * 78, so this sits clear of both — it fails the mistakes that were really made and passes the
     * palette as designed, without being so tight that any future retint trips it.
     */
    private static final double MIN_DISTANCE = 60.0D;

    /**
     * Loaded from the classpath, not from a source path.
     *
     * <p>The first version pointed at {@code src/main/resources/...} and every test failed,
     * because the test task's working directory is not the project root. Resources are on the test
     * runtime classpath, which also means this reads the sheet as it is packaged rather than as it
     * sits in the tree — closer to what actually ships.
     */
    private static final String DIR = "/assets/planeshift/textures/entity/";

    private static BufferedImage sheet(Koopaling koopaling) throws Exception {
        String path = DIR + "koopaling_" + koopaling.id() + ".png";
        try (InputStream in = KoopalingPaletteTest.class.getResourceAsStream(path)) {
            assertTrue(in != null, "no texture on the classpath for " + koopaling + ": " + path);
            return ImageIO.read(in);
        }
    }

    @Test
    void everySiblingHasASheet() throws Exception {
        for (Koopaling koopaling : Koopaling.values()) {
            assertTrue(sheet(koopaling) != null, "unreadable texture for " + koopaling);
        }
    }

    @Test
    void noTwoSiblingsShareACrestColour() throws Exception {
        Map<Koopaling, int[]> crests = new LinkedHashMap<>();
        for (Koopaling koopaling : Koopaling.values()) {
            int rgb = sheet(koopaling).getRGB(HAIR_X, HAIR_Y);
            crests.put(koopaling, new int[] {
                    (rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF});
        }

        List<String> tooClose = new ArrayList<>();
        List<Koopaling> all = List.of(Koopaling.values());
        for (int i = 0; i < all.size(); i++) {
            for (int j = i + 1; j < all.size(); j++) {
                double d = distance(crests.get(all.get(i)), crests.get(all.get(j)));
                if (d < MIN_DISTANCE) {
                    tooClose.add(String.format("%s vs %s (%.1f apart)",
                            all.get(i), all.get(j), d));
                }
            }
        }
        assertTrue(tooClose.isEmpty(),
                "these bosses look like each other at range: " + tooClose);
    }

    @Test
    void theCrestIsNotTransparent() throws Exception {
        // A sampled point that misses the painted region would read as fully transparent, and every
        // pair would then be zero apart from every other — the test would pass by measuring
        // nothing. Cheap guard against the sample point drifting off the trim region.
        for (Koopaling koopaling : Koopaling.values()) {
            int alpha = (sheet(koopaling).getRGB(HAIR_X, HAIR_Y) >> 24) & 0xFF;
            assertTrue(alpha > 200,
                    koopaling + ": crest sample point is transparent, so the sheet is not being "
                            + "measured where the hair is painted");
        }
    }

    private static double distance(int[] a, int[] b) {
        double dr = a[0] - b[0];
        double dg = a[1] - b[1];
        double db = a[2] - b[2];
        return Math.sqrt(dr * dr + dg * dg + db * db);
    }
}
