package com.studio.planeshift.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;

/**
 * A big, soft, saturated menu button.
 *
 * <p>The title screen used vanilla {@code Button}s, which are the correct choice for a settings
 * page and the wrong one for the front door of a platformer: a 20px grey slab with 8px text reads
 * as a form control, and the first thing the player sees decides what kind of game they think they
 * have opened. The modern Mario menus are all the same handful of moves — large targets, thick
 * rounded corners, one saturated colour per action, a hard highlight along the top edge and a
 * shadow under the bottom — and none of them need a texture.
 *
 * <p>Drawn entirely from fills. That is a deliberate constraint rather than a shortcut: a nine-slice
 * sprite would have to be authored, atlased and kept in step with every size change, whereas a
 * button assembled from rectangles resizes for free and cannot end up mismatched with its own art.
 */
public class MenuButton extends AbstractButton {

    /** Corner inset, in pixels. Three rows of progressively wider fill make a rounded corner. */
    private static final int[] CORNER = {3, 2, 1};

    private final Runnable action;
    private final int colour;

    /** How far the face lifts under the cursor. Small, but it is what makes the target feel live. */
    private float lift;

    public MenuButton(int x, int y, int width, int height, Component label, int colour,
                      Runnable action) {
        super(x, y, width, height, label);
        this.colour = colour;
        this.action = action;
    }

    /**
     * 1.21.11 passes the input that triggered the press. Ignored here: every action on this screen
     * is the same whether it came from a click, a keyboard activation or a controller.
     */
    @Override
    public void onPress(InputWithModifiers input) {
        action.run();
    }

    /**
     * Draws the button face.
     *
     * <p>{@code renderContents} rather than {@code renderWidget}: on {@link AbstractButton} the
     * latter is final in 1.21.11, because vanilla wants to own the sprite-and-focus wrapper around
     * every button. This is the hook that is actually meant to be overridden.
     */
    @Override
    protected void renderContents(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean hovered = isHovered();
        lift += ((hovered ? 2.0F : 0.0F) - lift) * 0.35F;
        int top = getY() - Math.round(lift);

        int face = hovered ? PlaneShiftGui.lighten(colour, 0.14F) : colour;
        int shade = PlaneShiftGui.darken(colour, 0.42F);

        // Drop shadow, then the sunk base, then the face. Three layers is all a chunky button is.
        rounded(graphics, getX() + 2, top + 4, width, height, 0x40000000);
        rounded(graphics, getX(), top + 3, width, height, shade);
        rounded(graphics, getX(), top, width, height, face);

        // A hard highlight along the top edge. This one line does most of the work of making the
        // face read as convex rather than as a flat coloured rectangle.
        graphics.fill(getX() + 5, top + 2, getX() + width - 5, top + 4,
                PlaneShiftGui.lighten(colour, 0.45F));

        int textY = top + (height - 8) / 2;
        graphics.drawCenteredString(net.minecraft.client.Minecraft.getInstance().font,
                getMessage(), getX() + width / 2 + 1, textY + 1, 0xC0000000);
        graphics.drawCenteredString(net.minecraft.client.Minecraft.getInstance().font,
                getMessage(), getX() + width / 2, textY, 0xFFFFFFFF);

        if (!active) {
            rounded(graphics, getX(), top, width, height, 0x80101018);
        }
    }

    /** A filled rectangle with three-pixel rounded corners. */
    private static void rounded(GuiGraphics graphics, int x, int y, int w, int h, int colour) {
        graphics.fill(x + CORNER.length, y, x + w - CORNER.length, y + h, colour);
        for (int i = 0; i < CORNER.length; i++) {
            int inset = CORNER[i];
            graphics.fill(x + i, y + inset, x + i + 1, y + h - inset, colour);
            graphics.fill(x + w - i - 1, y + inset, x + w - i, y + h - inset, colour);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
