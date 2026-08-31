import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.imageio.ImageIO;

/** Converts the 4x4 Mew concept atlas into real 64x32 cuboid-model UV skins. */
public final class MewRigSkinImporter {

    private static final int SKIN_WIDTH = 64;
    private static final int SKIN_HEIGHT = 32;

    private MewRigSkinImporter() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: java tools/MewRigSkinImporter.java <atlas.png> <entity-texture-dir>");
        }
        BufferedImage atlas = ImageIO.read(new File(args[0]));
        File output = new File(args[1]);
        if (!output.isDirectory() && !output.mkdirs()) {
            throw new IllegalStateException("Could not create " + output);
        }

        Map<String, Integer> cells = new LinkedHashMap<>();
        cells.put("goomba", 1);
        cells.put("koopa", 2);
        cells.put("thwomp", 3);
        cells.put("bullet_bill", 4);
        cells.put("boo", 5);
        cells.put("lakitu", 6);
        cells.put("hammer_bro", 7);
        cells.put("spiny", 8);
        cells.put("buzzy_beetle", 9);
        cells.put("piranha_plant", 10);
        cells.put("toad", 11);
        cells.put("bowser", 12);
        cells.put("bowser_fire", 13);
        cells.put("ember_bolt", 14);
        cells.put("fireball", 14);
        cells.put("iceball", 15);

        int cellWidth = atlas.getWidth() / 4;
        int cellHeight = atlas.getHeight() / 4;
        int variant = 0;
        for (Map.Entry<String, Integer> entry : cells.entrySet()) {
            int index = entry.getValue();
            BufferedImage cell = atlas.getSubimage((index % 4) * cellWidth,
                    (index / 4) * cellHeight, cellWidth, cellHeight);
            BufferedImage skin = createSkin(cell, variant++);
            ImageIO.write(skin, "png", new File(output, entry.getKey() + ".png"));
        }
    }

    private static BufferedImage createSkin(BufferedImage source, int variant) {
        BufferedImage skin = new BufferedImage(SKIN_WIDTH, SKIN_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        int[] bounds = subjectBounds(source);
        Color base = averageSubjectColor(source, bounds);
        Color accent = rotate(base, 35 + variant * 11);
        Color shadow = shade(base, 0.48F);

        paintCube(skin, source, bounds, 32, 0, 8, 8, 8, accent, shadow);
        paintCube(skin, source, bounds, 0, 0, 10, 9, 6, base, shadow);
        paintCube(skin, source, bounds, 0, 16, 3, 8, 3, accent, shadow);
        paintCube(skin, source, bounds, 12, 16, 4, 6, 4, shadow, shade(shadow, 0.72F));
        paintCube(skin, source, bounds, 28, 16, 1, 8, 6, accent, shadow);

        // A hidden signature pixel keeps two skins sourced from one concept distinct without
        // changing any face that the model samples.
        skin.setRGB(63, 31, new Color((variant * 47) & 255, (variant * 83) & 255,
                (variant * 131) & 255, 255).getRGB());
        return skin;
    }

    private static void paintCube(BufferedImage target, BufferedImage source, int[] bounds,
                                  int u, int v, int width, int height, int depth,
                                  Color base, Color shadow) {
        Graphics2D graphics = target.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        graphics.setColor(shade(base, 1.14F));
        graphics.fillRect(u + depth, v, width, depth);
        graphics.setColor(shadow);
        graphics.fillRect(u + depth + width, v, width, depth);
        graphics.fillRect(u, v + depth, depth, height);
        graphics.fillRect(u + depth + width, v + depth, depth, height);
        graphics.setColor(shade(shadow, 0.78F));
        graphics.fillRect(u + depth + width + depth, v + depth, width, height);

        int sx = bounds[0];
        int sy = bounds[1];
        int sw = bounds[2] - bounds[0] + 1;
        int sh = bounds[3] - bounds[1] + 1;
        graphics.drawImage(source, u + depth, v + depth, u + depth + width, v + depth + height,
                sx, sy, sx + sw, sy + sh, null);
        graphics.dispose();
        outline(target, u + depth, v + depth, width, height);
    }

    private static void outline(BufferedImage image, int x, int y, int width, int height) {
        int dark = 0xFF11162A;
        for (int px = x; px < x + width; px++) {
            image.setRGB(px, y, dark);
            image.setRGB(px, y + height - 1, dark);
        }
        for (int py = y; py < y + height; py++) {
            image.setRGB(x, py, dark);
            image.setRGB(x + width - 1, py, dark);
        }
    }

    private static int[] subjectBounds(BufferedImage image) {
        Color background = new Color(image.getRGB(2, 2), true);
        int minX = image.getWidth();
        int minY = image.getHeight();
        int maxX = 0;
        int maxY = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                Color color = new Color(image.getRGB(x, y), true);
                int distance = Math.abs(color.getRed() - background.getRed())
                        + Math.abs(color.getGreen() - background.getGreen())
                        + Math.abs(color.getBlue() - background.getBlue());
                if (distance > 52) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        if (minX > maxX || minY > maxY) {
            return new int[]{0, 0, image.getWidth() - 1, image.getHeight() - 1};
        }
        return new int[]{minX, minY, maxX, maxY};
    }

    private static Color averageSubjectColor(BufferedImage image, int[] bounds) {
        long red = 0;
        long green = 0;
        long blue = 0;
        long count = 0;
        for (int y = bounds[1]; y <= bounds[3]; y += 3) {
            for (int x = bounds[0]; x <= bounds[2]; x += 3) {
                Color color = new Color(image.getRGB(x, y), true);
                if (color.getRed() + color.getGreen() + color.getBlue() > 70) {
                    red += color.getRed();
                    green += color.getGreen();
                    blue += color.getBlue();
                    count++;
                }
            }
        }
        return count == 0 ? new Color(170, 110, 60)
                : new Color((int) (red / count), (int) (green / count), (int) (blue / count));
    }

    private static Color shade(Color color, float multiplier) {
        return new Color(clamp(color.getRed() * multiplier), clamp(color.getGreen() * multiplier),
                clamp(color.getBlue() * multiplier));
    }

    private static Color rotate(Color color, int degrees) {
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        return Color.getHSBColor((hsb[0] + degrees / 360.0F) % 1.0F,
                Math.min(1.0F, hsb[1] * 1.15F), Math.min(1.0F, hsb[2] * 1.08F));
    }

    private static int clamp(float value) {
        return Math.max(0, Math.min(255, Math.round(value)));
    }
}
