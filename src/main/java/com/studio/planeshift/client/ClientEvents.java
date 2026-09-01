package com.studio.planeshift.client;

import com.studio.planeshift.PlaneShift;
import com.studio.planeshift.client.camera.CameraDirector;
import com.studio.planeshift.client.input.PlaneConstrainedInput;
import com.studio.planeshift.client.input.PlaneMovementAssists;
import com.studio.planeshift.client.music.CourseMusicManager;
import com.studio.planeshift.common.course.CourseCrouch;
import com.studio.planeshift.client.screen.CoursePauseScreen;
import com.studio.planeshift.common.network.FormActionPayload;
import com.studio.planeshift.common.network.ReserveSwapPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CalculateDetachedCameraDistanceEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.event.entity.EntityEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * Client-side game-bus wiring (Design Bible, "Technical architecture").
 *
 * <p>Everything in this class only loads on the physical client. It owns camera,
 * input projection, and music — never gameplay authority. Mod-bus registration
 * lives in {@link ClientModEvents}.
 */
@EventBusSubscriber(modid = PlaneShift.MOD_ID, value = Dist.CLIENT)
public final class ClientEvents {

    private ClientEvents() {
    }

    /**
     * Swaps the vanilla pause screen for the course one while a course is running.
     *
     * <p>Done by replacing the screen as it opens rather than by binding a key, so it covers every
     * route to pausing — Escape, losing window focus, a controller Start button — without having
     * to know about any of them.
     *
     * <p>Only the plain pause screen is intercepted. {@code PauseScreen} is also constructed for
     * the "Saving world" pause during a level save, which has no menu and must not be replaced;
     * {@code isPauseScreen()} being false on that variant is how the two are told apart.
     */
    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (!(event.getNewScreen() instanceof PauseScreen pause) || !pause.isPauseScreen()) {
            return;
        }
        if (!ClientCourseState.get().inCourse()) {
            return;
        }
        event.setNewScreen(new CoursePauseScreen());
    }

    @SubscribeEvent
    public static void onClientTickPost(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;

        if (player != null && minecraft.screen == null) {
            if (!(player.input instanceof PlaneConstrainedInput)) {
                player.input = new PlaneConstrainedInput(minecraft.options);
            }

            while (PlaneShiftKeybinds.FORM_ACTION.consumeClick()) {
                Vec3 aim = player.getLookAngle();
                ClientPacketDistributor.sendToServer(new FormActionPayload(aim));
            }

            while (PlaneShiftKeybinds.SWAP_RESERVE.consumeClick()) {
                ClientPacketDistributor.sendToServer(ReserveSwapPayload.INSTANCE);
            }

            // After entities tick, so mouse-driven head turning cannot undo the 2.5D silhouette.
            PlaneMovementAssists.tickAvatarFacing(player);
        }

        CameraDirector.tickCameraType();
        CourseMusicManager.tick();
    }

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        CameraDirector.onComputeCameraAngles(event);
    }

    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        CameraDirector.onComputeFov(event);
    }

    @SubscribeEvent
    public static void onCameraDistance(CalculateDetachedCameraDistanceEvent event) {
        CameraDirector.onCameraDistance(event);
    }

    /**
     * Client mirror of the course crouch hitbox.
     *
     * <p>The client predicts its own movement, so it has to compute the same height the server
     * does or the player stutters against gaps the server thinks they fit through. The rule
     * itself lives in {@link CourseCrouch}; only the source of the course flag differs.
     */
    @SubscribeEvent
    public static void onEntitySize(EntityEvent.Size event) {
        if (!(event.getEntity() instanceof LocalPlayer)) {
            return;
        }
        var crouched = CourseCrouch.crouchedDimensions(event.getPose(), event.getNewSize(),
                ClientCourseState.get().inCourse());
        if (crouched != null) {
            event.setNewSize(crouched);
        }
    }

    /**
     * Fired inside {@code LocalPlayer#aiStep} right after the input is projected and before the
     * movement for the tick is applied — the correct point for the coyote/float assists.
     */
    @SubscribeEvent
    public static void onMovementInputUpdate(MovementInputUpdateEvent event) {
        if (event.getEntity() instanceof LocalPlayer player) {
            PlaneMovementAssists.tick(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        installInput(event.getPlayer());
    }

    @SubscribeEvent
    public static void onPlayerRespawn(ClientPlayerNetworkEvent.Clone event) {
        installInput(event.getPlayer());
    }

    private static void installInput(LocalPlayer player) {
        if (player != null) {
            Minecraft minecraft = Minecraft.getInstance();
            player.input = new PlaneConstrainedInput(minecraft.options);
            // The assists hold their budgets statically now, so a respawn or dimension change
            // must not carry a half-spent coyote or float window across.
            PlaneMovementAssists.reset();
        }
    }
}
