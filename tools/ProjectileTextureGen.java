import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.imageio.ImageIO;

/** Converts the original 3x2 projectile material atlas into exact 64x64 model UV sheets. */
public final class ProjectileTextureGen {

    private record Design(int cell, Color base, Color dark, Color light, Color accent) { }
    private static final Map<String, Design> DESIGNS = new LinkedHashMap<>();

    static {
        DESIGNS.put("ember_bolt", design(0, 0xD94C15, 0x252A31, 0xFFB12F, 0x45E8EF));
        DESIGNS.put("hammer", design(1, 0xB88429, 0x173F43, 0xF0C55A, 0x57D8D8));
        DESIGNS.put("fireball", design(2, 0xF36B12, 0x6B2219, 0xFFF0A3, 0xFFB027));
        DESIGNS.put("iceball", design(3, 0x53BFD9, 0x173D69, 0xE7FCFF, 0x8FE7F2));
        DESIGNS.put("boomerang", design(4, 0x267567, 0x173C3D, 0xD7A839, 0x35E5E3));
        DESIGNS.put("bowser_fire", design(5, 0xD6331E, 0x2A2027, 0xFFB31C, 0xFFF1A8));
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: java tools/ProjectileTextureGen.java <atlas.png> <entity-texture-dir>");
        }
        BufferedImage atlas = ImageIO.read(new File(args[0]));
        File output = new File(args[1]);
        if (atlas == null || !output.isDirectory() && !output.mkdirs()) {
            throw new IllegalArgumentException("Unreadable atlas or output directory");
        }
        for (Map.Entry<String, Design> entry : DESIGNS.entrySet()) {
            File file = new File(output, entry.getKey() + ".png");
            ImageIO.write(make(atlas, entry.getValue()), "png", file);
            System.out.println("wrote " + file + " (64x64)");
        }
    }

    private static BufferedImage make(BufferedImage atlas, Design design) {
        BufferedImage out = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        int cellW = atlas.getWidth() / 3;
        int cellH = atlas.getHeight() / 2;
        int sx = (design.cell() % 3) * cellW;
        int sy = (design.cell() / 3) * cellH;
        for (int quadrant = 0; quadrant < 4; quadrant++) {
            BufferedImage pixels = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
            Graphics2D pg = pixels.createGraphics();
            pg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            int inset = quadrant * Math.max(1, cellW / 24);
            pg.drawImage(atlas, 0, 0, 8, 8, sx + inset, sy + inset,
                    sx + cellW - inset, sy + cellH - inset, null);
            pg.dispose();
            grade(pixels, design.base());
            int dx = quadrant % 2 * 32;
            int dy = quadrant / 2 * 32;
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.drawImage(pixels, dx, dy, dx + 32, dy + 32, 0, 0, 8, 8, null);
        }
        paintIdentity(g, design);
        g.dispose();
        return out;
    }

    private static void paintIdentity(Graphics2D g, Design d) {
        g.setColor(new Color(d.dark().getRGB(), true));
        g.fillRect(0, 30, 64, 3);
        g.fillRect(30, 0, 3, 64);
        g.setColor(new Color(d.light().getRed(), d.light().getGreen(), d.light().getBlue(), 150));
        g.fillRect(1, 1, 28, 2);
        g.fillRect(33, 33, 28, 2);
        g.setColor(d.accent());
        g.fillRect(13, 13, 6, 6);
        g.fillRect(45, 13, 4, 4);
        g.fillRect(13, 45, 4, 4);
        g.fillRect(45, 45, 6, 6);
    }

    private static void grade(BufferedImage image, Color base) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                Color raw = new Color(image.getRGB(x, y), true);
                int r = (raw.getRed() * 3 + base.getRed() * 2) / 5;
                int g = (raw.getGreen() * 3 + base.getGreen() * 2) / 5;
                int b = (raw.getBlue() * 3 + base.getBlue() * 2) / 5;
                float[] hsb = Color.RGBtoHSB(r, g, b, null);
                hsb[2] = Math.min(1.0F, 0.22F + hsb[2]);
                image.setRGB(x, y, Color.HSBtoRGB(hsb[0], Math.min(1.0F, hsb[1] * 1.08F), hsb[2]));
            }
        }
    }

    private static Design design(int cell, int base, int dark, int light, int accent) {
        return new Design(cell, color(base), color(dark), color(light), color(accent));
    }

    private static Color color(int rgb) {
        return new Color(rgb | 0xFF000000, true);
    }

    private ProjectileTextureGen() { }
}
