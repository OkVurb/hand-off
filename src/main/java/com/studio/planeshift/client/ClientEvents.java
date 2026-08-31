package com.studio.planeshift.client;

import com.studio.planeshift.PlaneShift;
import com.studio.planeshift.client.camera.CameraDirector;
import com.studio.planeshift.client.input.PlaneConstrainedInput;
import com.studio.planeshift.client.music.CourseMusicManager;
import com.studio.planeshift.common.network.FormActionPayload;
import com.studio.planeshift.common.network.ReserveSwapPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CalculateDetachedCameraDistanceEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
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
        }
    }
}