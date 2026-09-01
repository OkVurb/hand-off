package com.studio.planeshift.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.studio.planeshift.PlaneShift;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/**
 * Client key bindings (Design Bible, "Input and accessibility").
 *
 * <p>Only two extra controls are required in the vertical slice: use the active Form
 * and swap the reserve Form. Movement, jump and camera stay on vanilla keys so the
 * player never has to learn a new locomotion grammar.
 */
public final class PlaneShiftKeybinds {

    public static final KeyMapping.Category CATEGORY =
            new KeyMapping.Category(PlaneShift.id("planeshift"));

    public static final KeyMapping FORM_ACTION = new KeyMapping(
            "key.planeshift.form_action",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            CATEGORY);

    public static final KeyMapping SWAP_RESERVE = new KeyMapping(
            "key.planeshift.swap_reserve",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            CATEGORY);

    /**
     * Opens the playtest tester menu. F6 because it is outside the F1-F5 block vanilla already
     * uses and outside the number row the player is holding during a course.
     */
    public static final KeyMapping TESTER_MENU = new KeyMapping(
            "key.planeshift.tester_menu",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F6,
            CATEGORY);

    private PlaneShiftKeybinds() {
    }
}
