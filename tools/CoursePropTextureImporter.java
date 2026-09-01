import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.imageio.ImageIO;

/** Crops the transparent 4x4 course-prop source atlas into final 16x16 block textures. */
public final class CoursePropTextureImporter {

    private record Target(int cell, Color opaqueBase) { }
    private static final Map<String, Target> TARGETS = new LinkedHashMap<>();

    static {
        TARGETS.put("axe_block", new Target(0, null));
        TARGETS.put("axe_block_taken", new Target(1, null));
        TARGETS.put("checkpoint_beacon", new Target(2, null));
        TARGETS.put("checkpoint_beacon_lit", new Target(3, null));
        TARGETS.put("coin_ring_block", new Target(4, null));
        TARGETS.put("coin_ring_block_used", new Target(5, null));
        TARGETS.put("course_vine", new Target(6, null));
        TARGETS.put("donut_block_shaking", new Target(7, new Color(0xB65727)));
        TARGETS.put("note_block", new Target(8, new Color(0x174F57)));
        TARGETS.put("on_off_switch", new Target(9, null));
        TARGETS.put("on_off_switch_powered", new Target(10, null));
        TARGETS.put("p_switch", new Target(11, null));
        TARGETS.put("p_switch_pressed", new Target(12, null));
        TARGETS.put("spike_block", new Target(13, null));
        TARGETS.put("loop_trigger", new Target(14, null));
        TARGETS.put("secret_vine", new Target(15, null));
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: java tools/CoursePropTextureImporter.java <atlas.png> <block-texture-dir>");
        }
        BufferedImage atlas = ImageIO.read(new File(args[0]));
        File output = new File(args[1]);
        if (atlas == null || !output.isDirectory() && !output.mkdirs()) {
            throw new IllegalArgumentException("Unreadable atlas or output directory");
        }

        for (Map.Entry<String, Target> entry : TARGETS.entrySet()) {
            BufferedImage texture = crop(atlas, entry.getValue());
            File file = new File(output, entry.getKey() + ".png");
            ImageIO.write(texture, "png", file);
            System.out.println("wrote " + file);
        }
    }

    private static BufferedImage crop(BufferedImage atlas, Target target) {
        int col = target.cell() % 4;
        int row = target.cell() / 4;
        int x0 = col * atlas.getWidth() / 4;
        int x1 = (col + 1) * atlas.getWidth() / 4;
        int y0 = row * atlas.getHeight() / 4;
        int y1 = (row + 1) * atlas.getHeight() / 4;

        int minX = x1;
        int minY = y1;
        int maxX = x0;
        int maxY = y0;
        for (int y = y0; y < y1; y++) {
            for (int x = x0; x < x1; x++) {
                if ((atlas.getRGB(x, y) >>> 24) > 24) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        if (maxX < minX) {
            throw new IllegalStateException("Empty source cell " + target.cell());
        }

        int sourceW = maxX - minX + 1;
        int sourceH = maxY - minY + 1;
        double scale = Math.min(14.0 / sourceW, 14.0 / sourceH);
        int width = Math.max(1, (int) Math.round(sourceW * scale));
        int height = Math.max(1, (int) Math.round(sourceH * scale));
        int dx = (16 - width) / 2;
        int dy = 15 - height;

        BufferedImage texture = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = texture.createGraphics();
        if (target.opaqueBase() != null) {
            g.setColor(target.opaqueBase());
            g.fillRect(0, 0, 16, 16);
            // A small bevel stops a repeated cube-face icon looking like a flat UI tile.
            g.setColor(target.opaqueBase().brighter());
            g.fillRect(0, 0, 16, 1);
            g.fillRect(0, 0, 1, 16);
            g.setColor(target.opaqueBase().darker());
            g.fillRect(0, 15, 16, 1);
            g.fillRect(15, 0, 1, 16);
        }
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(atlas, dx, dy, dx + width, dy + height, minX, minY, maxX + 1, maxY + 1, null);
        g.dispose();
        return texture;
    }

    private CoursePropTextureImporter() { }
}
