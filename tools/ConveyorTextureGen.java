import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Generates the directional conveyor top and mechanical side textures. */
public final class ConveyorTextureGen {

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("Usage: java tools/ConveyorTextureGen.java <block-texture-dir>");
        }
        File output = new File(args[0]);
        if (!output.isDirectory() && !output.mkdirs()) {
            throw new IllegalStateException("Could not create " + output);
        }
        ImageIO.write(top(), "png", new File(output, "conveyor_belt_top.png"));
        ImageIO.write(side(), "png", new File(output, "conveyor_belt_side.png"));
        ImageIO.write(particle(), "png", new File(output, "conveyor_belt.png"));
    }

    private static BufferedImage top() {
        BufferedImage image = tile(new Color(0x173D43));
        Graphics2D g = image.createGraphics();
        g.setColor(new Color(0xC79431));
        g.fillRect(1, 1, 2, 14);
        g.fillRect(13, 1, 2, 14);
        g.setColor(new Color(0x235B61));
        for (int y = 2; y < 15; y += 4) {
            g.fillRect(3, y, 10, 2);
        }
        g.setColor(new Color(0x56E0DD));
        chevron(g, 8, 3);
        chevron(g, 8, 9);
        g.dispose();
        return image;
    }

    private static BufferedImage side() {
        BufferedImage image = tile(new Color(0x183C43));
        Graphics2D g = image.createGraphics();
        g.setColor(new Color(0xA86D2A));
        for (int y = 2; y < 14; y += 4) {
            g.fillRect(1, y, 14, 2);
            g.setColor(new Color(0xD39A39));
            g.fillRect(2, y, 12, 1);
            g.setColor(new Color(0xA86D2A));
        }
        g.setColor(new Color(0x54D8D6));
        g.fillRect(2, 14, 12, 1);
        g.dispose();
        return image;
    }

    private static BufferedImage particle() {
        BufferedImage image = side();
        Graphics2D g = image.createGraphics();
        g.setColor(new Color(0x56E0DD));
        g.fillRect(7, 5, 2, 6);
        g.fillRect(5, 7, 6, 2);
        g.setColor(new Color(0xD39A39));
        g.fillRect(7, 7, 2, 2);
        g.dispose();
        return image;
    }

    private static BufferedImage tile(Color base) {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setColor(base);
        g.fillRect(0, 0, 16, 16);
        g.setColor(base.brighter());
        g.fillRect(0, 0, 16, 1);
        g.setColor(base.darker());
        g.fillRect(0, 15, 16, 1);
        g.dispose();
        return image;
    }

    private static void chevron(Graphics2D g, int x, int y) {
        g.fillRect(x - 3, y, 2, 2);
        g.fillRect(x + 2, y, 2, 2);
        g.fillRect(x - 1, y + 2, 4, 2);
    }

    private ConveyorTextureGen() { }
}
