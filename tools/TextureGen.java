import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * Generates PlaneShift's placeholder (greybox) textures.
 *
 * <p>Run with {@code java TextureGen.java <assetsTexturesDir>}.
 *
 * <p>These are deliberately not art. A greybox has one job — let you tell things apart while
 * playtesting — and the previous hand-made set failed it: 24 of 73 files were byte-identical to
 * another, including {@code hammer_bro}/{@code koopa} and {@code brick_block}/{@code secret_passage}.
 * So each texture here gets a hue spread by the golden angle across a stable index, which cannot
 * collide, plus its name's initials drawn in a 4x5 pixel font so it is identifiable even when two
 * hues look close under world lighting.
 *
 * <p>Sizes follow what the game expects: 16x16 for blocks, items and particles, 64x32 for entity
 * skins, 18x18 for mob effect icons (vanilla's size — checked against the client resources jar).
 * Mob effect icons use the colour the effect itself declares in {@code ModEffects} rather than a
 * spread hue, so the HUD icon matches the aura tint.
 */
public final class TextureGen {

    // ---------------------------------------------------------------- 4x5 pixel font


    /** Five rows per glyph, four columns each. */
    private static final Map<Character, String[]> GLYPHS = new LinkedHashMap<>();

    private static void glyph(char c, String r0, String r1, String r2, String r3, String r4) {
        GLYPHS.put(c, new String[] {r0, r1, r2, r3, r4});
    }

    static {
        glyph('A', ".##.", "#..#", "####", "#..#", "#..#");
        glyph('B', "###.", "#..#", "###.", "#..#", "###.");
        glyph('C', ".###", "#...", "#...", "#...", ".###");
        glyph('D', "###.", "#..#", "#..#", "#..#", "###.");
        glyph('E', "####", "#...", "###.", "#...", "####");
        glyph('F', "####", "#...", "###.", "#...", "#...");
        glyph('G', ".###", "#...", "#.##", "#..#", ".###");
        glyph('H', "#..#", "#..#", "####", "#..#", "#..#");
        glyph('I', ".##.", "..#.", "..#.", "..#.", ".##.");
        glyph('J', "..##", "...#", "...#", "#..#", ".##.");
        glyph('K', "#..#", "#.#.", "##..", "#.#.", "#..#");
        glyph('L', "#...", "#...", "#...", "#...", "####");
        glyph('M', "#..#", "####", "####", "#..#", "#..#");
        glyph('N', "#..#", "##.#", "#.##", "#..#", "#..#");
        glyph('O', ".##.", "#..#", "#..#", "#..#", ".##.");
        glyph('P', "###.", "#..#", "###.", "#...", "#...");
        glyph('Q', ".##.", "#..#", "#..#", "#.#.", ".#.#");
        glyph('R', "###.", "#..#", "###.", "#.#.", "#..#");
        glyph('S', ".###", "#...", ".##.", "...#", "###.");
        glyph('T', "####", ".#..", ".#..", ".#..", ".#..");
        glyph('U', "#..#", "#..#", "#..#", "#..#", ".##.");
        glyph('V', "#..#", "#..#", "#..#", ".##.", "..#.");
        glyph('W', "#..#", "#..#", "####", "####", "#..#");
        glyph('X', "#..#", ".##.", "..#.", ".##.", "#..#");
        glyph('Y', "#..#", "#..#", ".##.", "..#.", "..#.");
        glyph('Z', "####", "...#", ".##.", "#...", "####");
        glyph('0', ".##.", "#.##", "####", "##.#", ".##.");
        glyph('1', "..#.", ".##.", "..#.", "..#.", ".###");
        glyph('2', ".##.", "#..#", "..#.", ".#..", "####");
        glyph('3', "###.", "...#", ".##.", "...#", "###.");
        glyph('4', "#..#", "#..#", "####", "...#", "...#");
        glyph('5', "####", "#...", "###.", "...#", "###.");
        glyph('6', ".##.", "#...", "###.", "#..#", ".##.");
        glyph('7', "####", "...#", "..#.", ".#..", ".#..");
        glyph('8', ".##.", "#..#", ".##.", "#..#", ".##.");
        glyph('9', ".##.", "#..#", ".###", "...#", ".##.");
    }

    // ---------------------------------------------------------------- colour

    /**
     * A hue spread by the golden angle. Consecutive indices land far apart on the wheel and no
     * two indices in a set of this size can coincide, which is exactly the property the old
     * hand-picked colours lacked.
     */
    private static int spreadColour(int index, float saturation, float brightness) {
        float hue = (index * 0.6180339887F) % 1.0F;
        return java.awt.Color.HSBtoRGB(hue, saturation, brightness) & 0xFFFFFF;
    }

    private static int shade(int rgb, double factor) {
        int r = (int) Math.round(Math.min(255, ((rgb >> 16) & 0xFF) * factor));
        int g = (int) Math.round(Math.min(255, ((rgb >> 8) & 0xFF) * factor));
        int b = (int) Math.round(Math.min(255, (rgb & 0xFF) * factor));
        return (r << 16) | (g << 8) | b;
    }

    /** Perceived luminance, for picking readable text over the fill. */
    private static double luminance(int rgb) {
        return (0.2126 * ((rgb >> 16) & 0xFF) + 0.7152 * ((rgb >> 8) & 0xFF) + 0.0722 * (rgb & 0xFF)) / 255.0;
    }

    // ---------------------------------------------------------------- drawing

    private static void fill(BufferedImage img, int rgb) {
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                img.setRGB(x, y, 0xFF000000 | rgb);
            }
        }
    }

    private static void border(BufferedImage img, int rgb) {
        int w = img.getWidth();
        int h = img.getHeight();
        for (int x = 0; x < w; x++) {
            img.setRGB(x, 0, 0xFF000000 | rgb);
            img.setRGB(x, h - 1, 0xFF000000 | rgb);
        }
        for (int y = 0; y < h; y++) {
            img.setRGB(0, y, 0xFF000000 | rgb);
            img.setRGB(w - 1, y, 0xFF000000 | rgb);
        }
    }

    private static void drawText(BufferedImage img, String text, int x0, int y0, int rgb) {
        int cx = x0;
        for (char c : text.toCharArray()) {
            String[] g = GLYPHS.get(c);
            if (g != null) {
                for (int row = 0; row < 5; row++) {
                    for (int col = 0; col < 4; col++) {
                        if (g[row].charAt(col) == '#') {
                            int px = cx + col;
                            int py = y0 + row;
                            if (px >= 0 && py >= 0 && px < img.getWidth() && py < img.getHeight()) {
                                img.setRGB(px, py, 0xFF000000 | rgb);
                            }
                        }
                    }
                }
            }
            cx += 5;
        }
    }

    /**
     * Initials for a name: first letter of each underscore-separated word, up to {@code max}.
     * "hammer_bro" -> "HB", "coin" -> "CO", so single-word names still get two characters.
     */
    private static String initials(String name, int max) {
        String[] parts = name.split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (!p.isEmpty() && sb.length() < max) {
                sb.append(Character.toUpperCase(p.charAt(0)));
            }
        }
        while (sb.length() < Math.min(2, name.length())) {
            sb.append(Character.toUpperCase(name.charAt(sb.length())));
        }
        return sb.toString();
    }

    /** Leading letters of the name with separators removed: "coin_block" -> "COI". */
    private static String squash(String name, int len) {
        String s = name.replace("_", "").toUpperCase(Locale.ROOT);
        return s.substring(0, Math.min(len, s.length()));
    }

    private static String strategy(String name, int step, int maxLen) {
        String label = switch (step) {
            case 0 -> initials(name, 2);
            case 1 -> initials(name, 3);
            case 2 -> squash(name, 3);
            case 3 -> initials(name, 4);
            default -> squash(name, 4);
        };
        return label.length() > maxLen ? label.substring(0, maxLen) : label;
    }

    /**
     * Assigns each name a short label that is unique within its category.
     *
     * <p>Plain initials are not enough on their own: {@code checkpoint_beacon},
     * {@code coin_block} and {@code conveyor_belt} all reduce to "CB", which defeats the point
     * of labelling them. Names that collide are advanced through progressively more specific
     * strategies until the category is unambiguous, and anything still tied after that gets a
     * numeric suffix so the guarantee is absolute rather than best-effort.
     */
    private static Map<String, String> uniqueLabels(List<String> names, int maxLen) {
        Map<String, Integer> step = new LinkedHashMap<>();
        Map<String, String> label = new LinkedHashMap<>();
        for (String n : names) {
            step.put(n, 0);
            label.put(n, strategy(n, 0, maxLen));
        }
        for (int pass = 0; pass < 6; pass++) {
            Map<String, Integer> counts = new LinkedHashMap<>();
            label.values().forEach(v -> counts.merge(v, 1, Integer::sum));
            List<String> clashing = names.stream().filter(n -> counts.get(label.get(n)) > 1).toList();
            if (clashing.isEmpty()) {
                return label;
            }
            for (String n : clashing) {
                int next = step.get(n) + 1;
                step.put(n, next);
                label.put(n, strategy(n, next, maxLen));
            }
        }
        // Still tied: differentiate by position so no two ever render identically.
        Map<String, Integer> counts = new LinkedHashMap<>();
        label.values().forEach(v -> counts.merge(v, 1, Integer::sum));
        int suffix = 1;
        for (String n : names) {
            if (counts.get(label.get(n)) > 1) {
                String base = label.get(n);
                String trimmed = base.length() >= maxLen ? base.substring(0, maxLen - 1) : base;
                label.put(n, trimmed + (char) ('0' + (suffix++ % 10)));
            }
        }
        return label;
    }

    private static BufferedImage tile(String label, int index, int w, int h, Integer forcedColour) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        int base = forcedColour != null ? forcedColour : spreadColour(index, 0.62F, 0.86F);
        fill(img, base);
        border(img, shade(base, 0.55));

        int ink = luminance(base) > 0.55 ? shade(base, 0.30) : 0xFFFFFF;
        int textWidth = label.length() * 5 - 1;
        drawText(img, label, (w - textWidth) / 2, (h - 5) / 2, ink);
        return img;
    }

    /** Widest label the tile can hold inside its border, at 5px per glyph including the gap. */
    private static int labelCapacity(int width) {
        return Math.min(4, Math.max(2, (width - 2 + 1) / 5));
    }

    // ---------------------------------------------------------------- content

    private static final List<String> BLOCKS = List.of(
            "brick_block", "checkpoint_beacon", "coin_block", "coin_ring_block", "conveyor_belt",
            "flag_pole", "hidden_question_block", "music_block", "note_block", "on_off_block",
            "on_off_switch", "p_switch", "prize_cache", "question_block", "secret_passage",
            "shift_gate", "spike_block", "spring_pad", "warp_pipe");

    private static final List<String> ENTITIES = List.of(
            "boo", "boomerang", "bowser", "bowser_fire", "bullet_bill", "buzzy_beetle",
            "ember_bolt", "fireball", "goomba", "hammer", "hammer_bro", "iceball", "koopa",
            "lakitu", "moving_platform", "piranha_plant", "spiny", "thwomp", "toad");

    /**
     * Textures referenced by a blockstate variant rather than by a registered block, so they
     * have no entry in {@code ModBlocks}. They still have to exist or the model fails to load.
     */
    private static final List<String> BLOCK_VARIANTS = List.of(
            "on_off_block_off", "p_switch_pressed", "prize_cache_opened", "question_block_used",
            "checkpoint_beacon_lit", "coin_ring_block_used", "on_off_switch_powered");

    private static final List<String> PARTICLES = List.of(
            "coin_sparkle", "hit_burst", "pickup_glow", "respawn_warp", "theme_dust");

    private static final List<String> ITEMS = List.of(
            "acorn", "barrier_charm", "boo_spawn_egg", "boomerang", "bowser_spawn_egg",
            "bullet_bill_spawn_egg", "buzzy_beetle_spawn_egg", "cloud_flower", "coin",
            "ember_charm", "extra_pip", "fire_flower", "five_up", "gale_charm",
            "goomba_spawn_egg", "hammer", "hammer_bro_spawn_egg", "hidden_question_block",
            "ice_flower", "koopa_spawn_egg", "lakitu_spawn_egg", "leaf", "magnet_charm",
            "mega_mushroom", "mini_mushroom", "moving_platform_spawn_egg",
            "piranha_plant_spawn_egg", "propeller_mushroom", "spiny_spawn_egg", "star_coin",
            "star_power", "super_mushroom", "tanooki", "three_up", "thwomp_spawn_egg",
            "toad_spawn_egg");

    /** Name to the colour the effect declares in ModEffects, so icon and aura tint agree. */
    private static final Map<String, Integer> EFFECTS = new LinkedHashMap<>();

    static {
        EFFECTS.put("fire_aura", 0xFF6633);
        EFFECTS.put("ice_aura", 0x88CCFF);
        EFFECTS.put("frozen", 0xCCFFFF);
        EFFECTS.put("leaf_aura", 0x55AA00);
        EFFECTS.put("star_power", 0xFFDD00);
        EFFECTS.put("mega_aura", 0xFF66AA);
        EFFECTS.put("mini_aura", 0x88CCFF);
        EFFECTS.put("propeller_aura", 0xFFD700);
        EFFECTS.put("acorn_aura", 0xCC8833);
        EFFECTS.put("cloud_aura", 0xAADDFF);
    }

    // ---------------------------------------------------------------- main

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("usage: java TextureGen.java <assetsTexturesDir>");
            System.exit(2);
        }
        Path root = Path.of(args[0]);
        int written = 0;

        // One shared counter across every 16x16/64x32 set, so a block and an item never land on
        // the same hue either.
        int index = 0;
        List<String> log = new ArrayList<>();

        List<String> allBlocks = new ArrayList<>(BLOCKS);
        allBlocks.addAll(BLOCK_VARIANTS);
        Map<String, String> blockLabels = uniqueLabels(allBlocks, labelCapacity(16));
        Map<String, String> itemLabels = uniqueLabels(ITEMS, labelCapacity(16));
        Map<String, String> particleLabels = uniqueLabels(PARTICLES, labelCapacity(16));
        Map<String, String> entityLabels = uniqueLabels(ENTITIES, labelCapacity(64));
        Map<String, String> effectLabels =
                uniqueLabels(List.copyOf(EFFECTS.keySet()), labelCapacity(18));

        for (String n : allBlocks) {
            write(root.resolve("block").resolve(n + ".png"),
                    tile(blockLabels.get(n), index++, 16, 16, null));
            written++;
        }
        for (String n : ITEMS) {
            write(root.resolve("item").resolve(n + ".png"),
                    tile(itemLabels.get(n), index++, 16, 16, null));
            written++;
        }
        for (String n : PARTICLES) {
            write(root.resolve("particle").resolve(n + ".png"),
                    tile(particleLabels.get(n), index++, 16, 16, null));
            written++;
        }
        for (String n : ENTITIES) {
            write(root.resolve("entity").resolve(n + ".png"),
                    tile(entityLabels.get(n), index++, 64, 32, null));
            written++;
        }
        // Mob effect icons: vanilla is 18x18, and the colour comes from the effect itself.
        for (Map.Entry<String, Integer> e : EFFECTS.entrySet()) {
            write(root.resolve("mob_effect").resolve(e.getKey() + ".png"),
                    tile(effectLabels.get(e.getKey()), index++, 18, 18, e.getValue()));
            written++;
        }

        log.add("labels: " + blockLabels + " " + itemLabels + " " + particleLabels
                + " " + entityLabels + " " + effectLabels);
        log.forEach(System.out::println);
        System.out.printf(Locale.ROOT,
                "wrote %d placeholder textures (%d block, %d item, %d particle, %d entity, %d mob_effect)%n",
                written, allBlocks.size(), ITEMS.size(), PARTICLES.size(), ENTITIES.size(), EFFECTS.size());
    }

    private static void write(Path out, BufferedImage img) throws IOException {
        Files.createDirectories(out.getParent());
        ImageIO.write(img, "PNG", out.toFile());
    }

    private TextureGen() {
    }
}
