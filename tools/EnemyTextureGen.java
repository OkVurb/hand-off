import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * Converts the original enemy material atlas into exact 128x128 UV sheets for
 * {@code BespokeEnemyModel}.
 *
 * <p>The image generator is used for material language, where it is strong; this program owns
 * dimensions, UV placement, pixel density and faces, where generative images are unreliable.
 * Run with:
 *
 * <pre>java tools/EnemyTextureGen.java tools/art_sources/enemy_material_atlas.png
 *     src/main/resources/assets/planeshift/textures/entity</pre>
 */
public final class EnemyTextureGen {

    private record Design(int cell, Color base, Color dark, Color light, Color glow, Face face) { }
    private enum Face { SPROUT, GECKO, CRUSHER, TORPEDO, WISP, RIDER, PANGOLIN, CRAB, BEETLE, PLANT, BOSS }

    private static final Map<String, Design> DESIGNS = new LinkedHashMap<>();

    static {
        DESIGNS.put("goomba", new Design(0, c(0xB9682F), c(0x4B2721), c(0xE0A14B), c(0x69D7D0), Face.SPROUT));
        DESIGNS.put("koopa", new Design(1, c(0x557B43), c(0x263B2C), c(0xB99235), c(0xA8F0E7), Face.GECKO));
        DESIGNS.put("thwomp", new Design(2, c(0x343A42), c(0x171C23), c(0x626B73), c(0x36E7EB), Face.CRUSHER));
        DESIGNS.put("bullet_bill", new Design(3, c(0x30313E), c(0x151620), c(0xB88728), c(0x42E6E0), Face.TORPEDO));
        DESIGNS.put("boo", new Design(4, c(0x8662D5), c(0x392A76), c(0xC7A8FF), c(0x55F3FF), Face.WISP));
        DESIGNS.put("lakitu", new Design(5, c(0x9ED3DC), c(0x467C89), c(0xF1E8C6), c(0xD6A238), Face.RIDER));
        DESIGNS.put("hammer_bro", new Design(6, c(0x1F625B), c(0x133638), c(0xB8872D), c(0x71E3D6), Face.PANGOLIN));
        DESIGNS.put("spiny", new Design(7, c(0xC84E43), c(0x572B42), c(0xEE8A62), c(0x61DDE9), Face.CRAB));
        DESIGNS.put("buzzy_beetle", new Design(8, c(0x263B67), c(0x111B36), c(0x97712B), c(0x4CE3EC), Face.BEETLE));
        DESIGNS.put("piranha_plant", new Design(9, c(0x4F852E), c(0x24451F), c(0xD44E50), c(0xF1E4B8), Face.PLANT));
        DESIGNS.put("bowser", new Design(10, c(0x29282A), c(0x111214), c(0xA54528), c(0xFF7B22), Face.BOSS));
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: java tools/EnemyTextureGen.java <material-atlas.png> <entity-texture-dir>");
        }
        BufferedImage atlas = ImageIO.read(new File(args[0]));
        if (atlas == null) {
            throw new IllegalArgumentException("Unreadable atlas: " + args[0]);
        }
        File output = new File(args[1]);
        if (!output.isDirectory() && !output.mkdirs()) {
            throw new IllegalStateException("Could not create " + output);
        }

        for (Map.Entry<String, Design> entry : DESIGNS.entrySet()) {
            BufferedImage skin = make(atlas, entry.getValue());
            File file = new File(output, entry.getKey() + ".png");
            ImageIO.write(skin, "png", file);
            System.out.println("wrote " + file + " (128x128)");
        }
    }

    private static BufferedImage make(BufferedImage atlas, Design d) {
        BufferedImage out = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setColor(d.dark());
        g.fillRect(0, 0, 128, 128);

        int cellW = atlas.getWidth() / 4;
        int cellH = atlas.getHeight() / 3;
        int sx = (d.cell() % 4) * cellW;
        int sy = (d.cell() / 4) * cellH;

        // Six independent swatches align with the six stable UV material regions in the model.
        material(g, atlas, sx, sy, cellW, cellH, 0, 0, 64, 36, 0, d);
        material(g, atlas, sx, sy, cellW, cellH, 64, 0, 64, 36, 1, d);
        material(g, atlas, sx, sy, cellW, cellH, 0, 40, 64, 36, 2, d);
        material(g, atlas, sx, sy, cellW, cellH, 64, 40, 64, 36, 3, d);
        material(g, atlas, sx, sy, cellW, cellH, 0, 80, 64, 40, 4, d);
        material(g, atlas, sx, sy, cellW, cellH, 64, 80, 64, 40, 5, d);

        // Four unused rows make region boundaries obvious when inspecting the raw UV sheet.
        g.setColor(d.dark());
        g.fillRect(0, 36, 128, 4);
        g.fillRect(0, 76, 128, 4);
        paintFace(g, d);
        g.dispose();
        return out;
    }

    private static void material(Graphics2D target, BufferedImage atlas, int sx, int sy, int sw, int sh,
                                 int dx, int dy, int dw, int dh, int variant, Design d) {
        // Collapse to a coarse intermediate first. Upscaling that with nearest-neighbour keeps
        // the generated material's character but guarantees stable Minecraft-sized pixels.
        BufferedImage pixels = new BufferedImage(16, 10, BufferedImage.TYPE_INT_ARGB);
        Graphics2D pg = pixels.createGraphics();
        pg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        int insetX = (variant * 23) % Math.max(1, sw / 4);
        int insetY = (variant * 17) % Math.max(1, sh / 4);
        pg.drawImage(atlas, 0, 0, 16, 10, sx + insetX, sy + insetY,
                sx + sw - insetX, sy + sh - insetY, null);
        pg.dispose();

        // Generated concept art is displayed against a dark navy studio background.  Sampling
        // it literally made the tiny in-world models collapse to black.  Blend every material
        // toward its authored gameplay colour and lift the value before nearest-neighbour
        // expansion.  This is deliberate side-camera readability, not post-processing at run time.
        for (int y = 0; y < pixels.getHeight(); y++) {
            for (int x = 0; x < pixels.getWidth(); x++) {
                Color raw = new Color(pixels.getRGB(x, y), true);
                pixels.setRGB(x, y, grade(raw, d.base()).getRGB());
            }
        }

        target.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        target.drawImage(pixels, dx, dy, dx + dw, dy + dh, 0, 0, 16, 10, null);

        // Controlled edge lighting keeps tiny moving parts readable under Minecraft lighting.
        target.setColor(withAlpha(d.light(), 72));
        target.fillRect(dx, dy, dw, 2);
        target.fillRect(dx, dy, 2, dh);
        target.setColor(withAlpha(d.dark(), 110));
        target.fillRect(dx, dy + dh - 2, dw, 2);
        target.fillRect(dx + dw - 2, dy, 2, dh);
    }

    private static void paintFace(Graphics2D g, Design d) {
        switch (d.face()) {
            case SPROUT -> {
                face(g, 1, 81, 10, 6, d.dark(), d.glow(), true);
                sideFace(g, 0, 0, 12, 9, 8, d.dark(), d.glow(), true);
            }
            case GECKO -> {
                face(g, 71, 7, 8, 6, d.dark(), d.glow(), false);
                muzzle(g, 4, 84, 6, 3, d.light(), d.dark());
                sideFace(g, 64, 0, 8, 6, 7, d.dark(), d.glow(), false);
            }
            case CRUSHER -> {
                face(g, 1, 81, 10, 7, d.dark(), d.glow(), false);
                rune(g, 70, 86, d.glow());
                sideFace(g, 0, 0, 16, 14, 8, d.dark(), d.glow(), false);
            }
            case TORPEDO -> {
                face(g, 69, 5, 10, 10, d.dark(), d.glow(), false);
                sideFace(g, 64, 0, 10, 10, 5, d.dark(), d.glow(), false);
            }
            case WISP -> {
                face(g, 1, 81, 8, 5, d.dark(), d.glow(), false);
                sideFace(g, 0, 0, 12, 10, 10, d.dark(), d.glow(), false);
            }
            case RIDER -> {
                face(g, 70, 6, 7, 6, d.dark(), d.glow(), false);
                muzzle(g, 2, 82, 5, 3, d.light(), d.dark());
                sideFace(g, 64, 0, 7, 6, 6, d.dark(), d.glow(), false);
            }
            case PANGOLIN -> {
                face(g, 71, 7, 8, 6, d.dark(), d.light(), false);
                muzzle(g, 4, 84, 6, 3, c(0xC89454), d.dark());
                sideFace(g, 64, 0, 8, 6, 7, d.dark(), d.light(), false);
            }
            case CRAB -> {
                face(g, 1, 81, 8, 4, d.dark(), d.glow(), false);
                sideFace(g, 0, 0, 14, 7, 10, d.dark(), d.glow(), false);
            }
            case BEETLE -> {
                face(g, 71, 7, 10, 7, d.dark(), d.glow(), false);
                sideFace(g, 64, 0, 10, 7, 7, d.dark(), d.glow(), false);
            }
            case PLANT -> {
                mouth(g, 1, 81, 10, 8, d.dark(), d.light());
                sideMouth(g, 64, 0, 12, 7, 10, d.dark(), d.light());
            }
            case BOSS -> {
                face(g, 74, 10, 12, 8, d.dark(), d.glow(), false);
                muzzle(g, 6, 86, 10, 5, c(0xA84A24), c(0x1A1717));
                sideFace(g, 64, 0, 12, 8, 10, d.dark(), d.glow(), false);
            }
        }
    }

    private static void face(Graphics2D g, int x, int y, int w, int h,
                             Color dark, Color eye, boolean tears) {
        g.setColor(withAlpha(dark, 235));
        g.fillRect(x, y, w, h);
        int eyeY = y + Math.max(1, h / 3);
        g.setColor(c(0x10151A));
        g.fillRect(x + 1, eyeY, 2, 2);
        g.fillRect(x + w - 3, eyeY, 2, 2);
        g.setColor(eye);
        g.fillRect(x + 1, eyeY, 1, 1);
        g.fillRect(x + w - 3, eyeY, 1, 1);
        g.setColor(c(0x171215));
        g.fillRect(x + w / 2 - 1, y + h - 2, 3, 1);
        if (tears && h >= 5) {
            g.setColor(eye);
            g.fillRect(x + 1, eyeY + 2, 1, 2);
            g.fillRect(x + w - 2, eyeY + 2, 1, 2);
        }
    }

    private static void muzzle(Graphics2D g, int x, int y, int w, int h, Color skin, Color nostril) {
        g.setColor(skin);
        g.fillRect(x, y, w, h);
        g.setColor(nostril);
        g.fillRect(x + 1, y + 1, 1, 1);
        g.fillRect(x + w - 2, y + 1, 1, 1);
    }

    private static void mouth(Graphics2D g, int x, int y, int w, int h, Color inside, Color tooth) {
        g.setColor(inside);
        g.fillRect(x, y, w, h);
        g.setColor(tooth);
        for (int px = x + 1; px < x + w - 1; px += 3) {
            g.fillRect(px, y, 2, 2);
            g.fillRect(px, y + h - 2, 2, 2);
        }
        g.setColor(c(0xB93B55));
        g.fillRect(x + 2, y + h / 2, w - 4, 2);
    }

    /** Paints gameplay-readable identity on both faces seen by the fixed side camera. */
    private static void sideFace(Graphics2D g, int u, int v, int w, int h, int depth,
                                 Color dark, Color eye, boolean tears) {
        sideFaceOne(g, u, v + depth, depth, h, dark, eye, tears);
        sideFaceOne(g, u + depth + w, v + depth, depth, h, dark, eye, tears);
    }

    private static void sideFaceOne(Graphics2D g, int x, int y, int w, int h,
                                    Color dark, Color eye, boolean tears) {
        g.setColor(withAlpha(dark, 205));
        g.fillRect(x, y, w, h);
        int ex = x + Math.max(1, w / 2 - 1);
        int ey = y + Math.max(1, h / 3);
        g.setColor(c(0x10151A));
        g.fillRect(ex, ey, Math.min(2, w - 1), 2);
        g.setColor(eye);
        g.fillRect(ex, ey, 1, 1);
        g.setColor(c(0x171215));
        g.fillRect(x + Math.max(1, w / 2 - 1), y + h - 2, Math.min(3, w - 1), 1);
        if (tears && h > 5) {
            g.setColor(eye);
            g.fillRect(ex, ey + 2, 1, 2);
        }
    }

    private static void sideMouth(Graphics2D g, int u, int v, int w, int h, int depth,
                                  Color inside, Color tooth) {
        mouth(g, u, v + depth, depth, h, inside, tooth);
        mouth(g, u + depth + w, v + depth, depth, h, inside, tooth);
    }

    private static void rune(Graphics2D g, int x, int y, Color glow) {
        g.setColor(glow);
        g.fillRect(x, y, 5, 1);
        g.fillRect(x, y, 1, 5);
        g.fillRect(x, y + 4, 4, 1);
        g.fillRect(x + 3, y + 2, 1, 3);
    }

    private static Color c(int rgb) {
        return new Color(rgb | 0xFF000000, true);
    }

    private static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    private static Color grade(Color raw, Color base) {
        int red = (raw.getRed() * 3 + base.getRed() * 2) / 5;
        int green = (raw.getGreen() * 3 + base.getGreen() * 2) / 5;
        int blue = (raw.getBlue() * 3 + base.getBlue() * 2) / 5;
        float[] hsb = Color.RGBtoHSB(red, green, blue, null);
        hsb[1] = Math.min(1.0F, hsb[1] * 1.08F);
        hsb[2] = Math.min(1.0F, 0.18F + hsb[2] * 1.05F);
        return new Color(Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]));
    }

    private EnemyTextureGen() { }
}
