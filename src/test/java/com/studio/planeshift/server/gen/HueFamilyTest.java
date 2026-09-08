package com.studio.planeshift.server.gen;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.studio.planeshift.common.course.CourseTheme;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A world is one hue family.
 *
 * <p>§5.3, and the reference confirms it hardest in its gold underground: one olive-and-gold family
 * across walls, ledges, pipes and terrain. A theme built from materials that disagree about their
 * hue does not read as a place, it reads as a kit.
 *
 * <p>Measures the shipped PNGs rather than a table of colours in Java, for the reason
 * {@code KoopalingPaletteTest} gives at length: the palette lives in {@code BlockTextureGen.py} and
 * reaches the game as an image, and a second copy of it in Java would be a rival source of truth.
 * The sheet is what the player looks at.
 *
 * <p>Only the surface, the fill and the platform are checked. The accent is deliberately exempt —
 * the entry is "one hue family <em>plus one or two rare accents</em>", and a brick block in a cave
 * or a gold trim in the sky is exactly that.
 */
class HueFamilyTest {

    /**
     * How far apart two of a theme's structural materials may sit, in degrees of hue.
     *
     * <p>Sixty is a generous family — warm browns through to yellow-greens all fit inside it. The
     * failures this was written for were not marginal: a volcano floored with the castle block put
     * hue 223 against hue 8, and a ghost house did the same. Anything that trips this is a material
     * from another world, not a shade of this one.
     */
    private static final double MAX_SPREAD = 60.0D;

    /** Below this saturation a texture has no hue worth talking about, and is skipped. */
    private static final double GREY = 0.10D;

    @Test
    @DisplayName("every theme's terrain agrees about its hue")
    void themesAreOneFamily() throws Exception {
        List<String> offenders = new ArrayList<>();
        for (CourseTheme theme : CourseTheme.values()) {
            GenContext.Palette palette = GenContext.Palette.forTheme(theme);
            List<Double> hues = new ArrayList<>();
            List<String> named = new ArrayList<>();
            for (BlockState state : List.of(palette.surface(), palette.fill(), palette.platform())) {
                String name = net.minecraft.core.registries.BuiltInRegistries.BLOCK
                        .getKey(state.getBlock()).getPath();
                Double hue = hueOf(name);
                if (hue == null) {
                    // Genuinely achromatic -- snow and cloud are white, and a white block belongs
                    // to every family. Not the same thing as a texture we failed to find, which
                    // texturesOf() refuses to let happen silently.
                    named.add(name + "=grey");
                    continue;
                }
                hues.add(hue);
                named.add(name + "=" + Math.round(hue));
            }
            double spread = spread(hues);
            if (spread > MAX_SPREAD) {
                offenders.add(theme + " spans " + Math.round(spread) + " degrees: " + named);
            }
        }
        assertTrue(offenders.isEmpty(),
                "these themes are built from materials that disagree about what world they are "
                        + "in: " + offenders);
    }

    /**
     * An indoor back wall belongs to the room it is the back of.
     *
     * <p>Separate from the terrain check because the wall is not terrain: it is the largest single
     * surface in an indoor course, it sits at the backdrop depth, and it is chosen by
     * {@code CourseDecorator} rather than by the palette. It was the half of §5.3 that the palette
     * fix did not reach -- both indoor themes had their floors moved into the world's family and
     * their walls left as shared blue-grey castle stone.
     *
     * <p>Compared against the theme's own surface, which is the material the player is standing on
     * while looking at the wall.
     */
    @Test
    @DisplayName("an indoor back wall is in its room's hue family")
    void indoorWallsMatchTheirRoom() throws Exception {
        List<String> offenders = new ArrayList<>();
        // The two themes CourseDecorator.backdrop sends to backWall. UNDERGROUND draws nothing and
        // everything else draws a hazed skyline, which is exempt by design.
        for (CourseTheme theme : List.of(CourseTheme.GHOST_HOUSE, CourseTheme.LAVA)) {
            String wall = net.minecraft.core.registries.BuiltInRegistries.BLOCK
                    .getKey(CourseDecorator.backWallMass(theme).getBlock()).getPath();
            String floor = net.minecraft.core.registries.BuiltInRegistries.BLOCK
                    .getKey(GenContext.Palette.forTheme(theme).surface().getBlock()).getPath();
            Double wallHue = hueOf(wall);
            Double floorHue = hueOf(floor);
            if (wallHue == null || floorHue == null) {
                continue;
            }
            double spread = spread(List.of(wallHue, floorHue));
            if (spread > MAX_SPREAD) {
                offenders.add(theme + " puts a " + wall + "=" + Math.round(wallHue)
                        + " wall behind a " + floor + "=" + Math.round(floorHue)
                        + " floor, " + Math.round(spread) + " degrees apart");
            }
        }
        assertTrue(offenders.isEmpty(), "indoor walls from another world: " + offenders);
    }

    /** The widest gap between any two hues, measured the short way round the circle. */
    private static double spread(List<Double> hues) {
        double worst = 0.0D;
        for (int i = 0; i < hues.size(); i++) {
            for (int j = i + 1; j < hues.size(); j++) {
                double d = Math.abs(hues.get(i) - hues.get(j)) % 360.0D;
                worst = Math.max(worst, Math.min(d, 360.0D - d));
            }
        }
        return worst;
    }

    /**
     * The saturation-weighted mean hue of a block texture, or null if it is effectively grey.
     *
     * <p>Weighted by saturation so a mostly-white snow block is not given a hue by a handful of
     * faint pixels, and averaged around the circle rather than linearly — hue 359 and hue 1 are
     * neighbours, and a linear mean of them is cyan.
     */
    private static Double hueOf(String block) throws IOException {
        double x = 0.0D;
        double y = 0.0D;
        double weight = 0.0D;
        for (Path texture : texturesOf(block)) {
            BufferedImage image = ImageIO.read(texture.toFile());
            assertTrue(image != null, "could not decode " + texture);
            for (int px = 0; px < image.getWidth(); px++) {
                for (int py = 0; py < image.getHeight(); py++) {
                    int rgb = image.getRGB(px, py);
                    if (((rgb >> 24) & 0xFF) == 0) {
                        continue;
                    }
                    float[] hsb = java.awt.Color.RGBtoHSB(
                            (rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, null);
                    if (hsb[1] < GREY || hsb[2] < 0.06F) {
                        continue;
                    }
                    double radians = hsb[0] * 2.0D * Math.PI;
                    x += Math.cos(radians) * hsb[1];
                    y += Math.sin(radians) * hsb[1];
                    weight += hsb[1];
                }
            }
        }
        if (weight <= 0.0D) {
            return null;
        }
        return (Math.toDegrees(Math.atan2(y, x)) + 360.0D) % 360.0D;
    }

    /**
     * Every {@code planeshift:} texture a block's model actually references.
     *
     * <p>Resolved through the model rather than by assuming the texture is named after the block,
     * because on this codebase it frequently is not: {@code course_dirt_block} is drawn with
     * {@code course_dirt.png}, and {@code COURSE_EMBER_BLOCK} is registered as
     * {@code course_magma_block}. The first version of this test guessed the filename and returned
     * null when the guess missed, which the caller treated as "grey, skip it" -- so the two blocks
     * above were never measured, and a check that looked at nothing passed. Anything unresolvable
     * fails here instead, loudly, because a silent skip in a check like this is worse than no check
     * at all: it reports safety it never established.
     */
    private static Set<Path> texturesOf(String block) throws IOException {
        Path model = assets().resolve("models/block/" + block + ".json");
        assertTrue(Files.exists(model), "no block model for " + block + " at " + model);
        JsonObject json = JsonParser.parseString(
                Files.readString(model, StandardCharsets.UTF_8)).getAsJsonObject();

        Set<Path> found = new LinkedHashSet<>();
        if (json.has("textures")) {
            for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("textures").entrySet()) {
                if (!entry.getValue().isJsonPrimitive()) {
                    continue;
                }
                String reference = entry.getValue().getAsString();
                if (!reference.startsWith("planeshift:block/")) {
                    continue;
                }
                Path texture = assets().resolve(
                        "textures/block/" + reference.substring("planeshift:block/".length()) + ".png");
                assertTrue(Files.exists(texture),
                        block + " model references " + reference + ", which is not on disk");
                found.add(texture);
            }
        }
        assertTrue(!found.isEmpty(), block + " model references no planeshift texture: " + model);
        return found;
    }

    private static Path assets() {
        Path dir = Path.of("").toAbsolutePath();
        while (dir != null && !Files.isDirectory(dir.resolve("src"))) {
            dir = dir.getParent();
        }
        if (dir == null) {
            throw new IllegalStateException("could not find the repo root");
        }
        return dir.resolve("src/main/resources/assets/planeshift");
    }
}
