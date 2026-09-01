package com.studio.planeshift.client;

import com.studio.planeshift.PlaneShift;
import com.studio.planeshift.client.camera.CameraDirector;
import com.studio.planeshift.client.hud.CourseHud;
import com.studio.planeshift.client.input.PlaneConstrainedInput;
import com.studio.planeshift.client.music.CourseMusicManager;
import com.studio.planeshift.client.render.CourseEnemyRenderer;
import com.studio.planeshift.client.render.CourseSkyboxRenderer;
import com.studio.planeshift.client.render.AnimatedCourseEnemyModel;
import com.studio.planeshift.client.render.BespokeEnemyModel;
import com.studio.planeshift.client.render.BespokeProjectileModel;
import com.studio.planeshift.client.render.BespokeProjectileRenderer;
import com.studio.planeshift.client.render.EnemyRigProfile;
import com.studio.planeshift.client.render.FirebarRenderer;
import com.studio.planeshift.client.render.MovingPlatformRenderer;
import com.studio.planeshift.client.render.PlaceholderRigModel;
import com.studio.planeshift.client.render.ProjectileVisualProfile;
import com.studio.planeshift.client.render.ToadModel;
import com.studio.planeshift.client.render.ToadRenderer;
import com.studio.planeshift.client.gui.PlaneShiftTitleScreen;
import com.studio.planeshift.client.screen.CourseMapScreen;
import com.studio.planeshift.client.screen.ToadShopScreen;
import com.studio.planeshift.common.network.OpenCourseMapPayload;
import com.studio.planeshift.common.network.CourseResultsPayload;
import com.studio.planeshift.client.screen.CourseResultsScreen;
import com.studio.planeshift.common.network.OpenTitleScreenPayload;
import com.studio.planeshift.common.network.OpenToadShopPayload;
import com.studio.planeshift.common.network.ScorePopupPayload;
import com.studio.planeshift.common.registry.ModEntities;
import com.studio.planeshift.common.registry.ModParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EndRodParticle;
import net.minecraft.client.particle.ExplodeParticle;
import net.minecraft.client.particle.FireworkParticles;
import net.minecraft.client.particle.PortalParticle;
import net.minecraft.client.particle.SuspendedTownParticle;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterCustomEnvironmentEffectRendererEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

/**
 * Client-only mod-bus registration.
 *
 * <p>This class is auto-registered on the mod event bus for the physical client.
 * It owns keybinds, GUI layers, renderers, particle providers and client payload
 * handlers — never gameplay authority.
 */
@EventBusSubscriber(modid = PlaneShift.MOD_ID, value = Dist.CLIENT)
public final class ClientModEvents {

    private ClientModEvents() {
    }

    @SubscribeEvent
    public static void onRegisterEnvironmentRenderers(
            RegisterCustomEnvironmentEffectRendererEvent event) {
        event.registerSkyboxRenderer(PlaneShift.id("course"), new CourseSkyboxRenderer());
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.registerCategory(PlaneShiftKeybinds.CATEGORY);
        event.register(PlaneShiftKeybinds.FORM_ACTION);
        event.register(PlaneShiftKeybinds.SWAP_RESERVE);
    }

    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, PlaneShift.id("course_hud"), CourseHud::render);
    }

    @SubscribeEvent
    public static void onRegisterLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(PlaceholderRigModel.LAYER_LOCATION, PlaceholderRigModel::createBodyLayer);
        event.registerLayerDefinition(AnimatedCourseEnemyModel.LAYER_LOCATION,
                AnimatedCourseEnemyModel::createBodyLayer);
        event.registerLayerDefinition(ToadModel.LAYER_LOCATION, ToadModel::createBodyLayer);
        for (EnemyRigProfile profile : EnemyRigProfile.values()) {
            if (profile != EnemyRigProfile.VILLAGER) {
                event.registerLayerDefinition(BespokeEnemyModel.layer(profile),
                        () -> BespokeEnemyModel.createLayer(profile));
            }
        }
        for (ProjectileVisualProfile profile : ProjectileVisualProfile.values()) {
            event.registerLayerDefinition(BespokeProjectileModel.layer(profile),
                    () -> BespokeProjectileModel.createLayer(profile));
        }
    }

    /**
     * Particle types are registered on the common side, but without a provider the
     * client silently drops every spawn request, so each type needs a sprite-set
     * provider backed by its {@code assets/planeshift/particles/*.json}.
     */
    @SubscribeEvent
    public static void onRegisterParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.COIN_SPARKLE.get(), FireworkParticles.SparkProvider::new);
        event.registerSpriteSet(ModParticles.PICKUP_GLOW.get(), EndRodParticle.Provider::new);
        event.registerSpriteSet(ModParticles.HIT_BURST.get(), ExplodeParticle.Provider::new);
        event.registerSpriteSet(ModParticles.RESPAWN_WARP.get(), PortalParticle.Provider::new);
        event.registerSpriteSet(ModParticles.THEME_DUST.get(), SuspendedTownParticle.Provider::new);
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        Identifier goomba = PlaneShift.id("textures/entity/goomba.png");
        Identifier koopa = PlaneShift.id("textures/entity/koopa.png");
        Identifier thwomp = PlaneShift.id("textures/entity/thwomp.png");
        Identifier bulletBill = PlaneShift.id("textures/entity/bullet_bill.png");
        Identifier boo = PlaneShift.id("textures/entity/boo.png");
        Identifier lakitu = PlaneShift.id("textures/entity/lakitu.png");
        Identifier hammerBro = PlaneShift.id("textures/entity/hammer_bro.png");
        Identifier spiny = PlaneShift.id("textures/entity/spiny.png");
        Identifier buzzyBeetle = PlaneShift.id("textures/entity/buzzy_beetle.png");
        Identifier piranhaPlant = PlaneShift.id("textures/entity/piranha_plant.png");
        Identifier bowser = PlaneShift.id("textures/entity/bowser.png");

        event.registerEntityRenderer(ModEntities.GOOMBA.get(),
                CourseEnemyRenderer.provider(goomba, 0.25F, EnemyRigProfile.SPROUTLING));
        event.registerEntityRenderer(ModEntities.KOOPA.get(),
                CourseEnemyRenderer.provider(koopa, 0.22F, EnemyRigProfile.GECKO));
        event.registerEntityRenderer(ModEntities.THWOMP.get(),
                CourseEnemyRenderer.provider(thwomp, 0.55F, EnemyRigProfile.CRUSHER));
        event.registerEntityRenderer(ModEntities.BULLET_BILL.get(),
                CourseEnemyRenderer.provider(bulletBill, 0.15F, EnemyRigProfile.FLYER));
        event.registerEntityRenderer(ModEntities.BOO.get(),
                CourseEnemyRenderer.provider(boo, 0.22F, EnemyRigProfile.WISP));
        event.registerEntityRenderer(ModEntities.LAKITU.get(),
                CourseEnemyRenderer.provider(lakitu, 0.22F, EnemyRigProfile.RIDER));
        event.registerEntityRenderer(ModEntities.HAMMER_BRO.get(),
                CourseEnemyRenderer.provider(hammerBro, 0.22F, EnemyRigProfile.WARRIOR));
        event.registerEntityRenderer(ModEntities.SPINY.get(),
                CourseEnemyRenderer.provider(spiny, 0.22F, EnemyRigProfile.CRAWLER));
        event.registerEntityRenderer(ModEntities.BUZZY_BEETLE.get(),
                CourseEnemyRenderer.provider(buzzyBeetle, 0.25F, EnemyRigProfile.BEETLE));
        event.registerEntityRenderer(ModEntities.PIRANHA_PLANT.get(),
                CourseEnemyRenderer.provider(piranhaPlant, 0.55F, EnemyRigProfile.PLANT));
        event.registerEntityRenderer(ModEntities.TOAD.get(), ToadRenderer::new);
        event.registerEntityRenderer(ModEntities.BOWSER.get(),
                CourseEnemyRenderer.provider(bowser, 1.0F, EnemyRigProfile.BOSS));
        event.registerEntityRenderer(ModEntities.EMBER_BOLT.get(),
                BespokeProjectileRenderer.provider(PlaneShift.id("textures/entity/ember_bolt.png"),
                        ProjectileVisualProfile.EMBER_BOLT));
        event.registerEntityRenderer(ModEntities.HAMMER.get(),
                BespokeProjectileRenderer.provider(PlaneShift.id("textures/entity/hammer.png"),
                        ProjectileVisualProfile.HAMMER));
        event.registerEntityRenderer(ModEntities.FIREBALL.get(),
                BespokeProjectileRenderer.provider(PlaneShift.id("textures/entity/fireball.png"),
                        ProjectileVisualProfile.FIREBALL));
        event.registerEntityRenderer(ModEntities.ICEBALL.get(),
                BespokeProjectileRenderer.provider(PlaneShift.id("textures/entity/iceball.png"),
                        ProjectileVisualProfile.ICEBALL));
        event.registerEntityRenderer(ModEntities.BOOMERANG.get(),
                BespokeProjectileRenderer.provider(PlaneShift.id("textures/entity/boomerang.png"),
                        ProjectileVisualProfile.BOOMERANG));
        event.registerEntityRenderer(ModEntities.BOWSER_FIRE.get(),
                BespokeProjectileRenderer.provider(PlaneShift.id("textures/entity/bowser_fire.png"),
                        ProjectileVisualProfile.BOWSER_FIRE));
        event.registerEntityRenderer(ModEntities.MOVING_PLATFORM.get(), MovingPlatformRenderer::new);
        event.registerEntityRenderer(ModEntities.FIREBAR.get(), FirebarRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterClientPayloadHandlers(RegisterClientPayloadHandlersEvent event) {
        event.register(CourseResultsPayload.TYPE,
                (payload, context) -> context.enqueueWork(() ->
                        Minecraft.getInstance().setScreen(new CourseResultsScreen(payload))));

        event.register(OpenTitleScreenPayload.TYPE,
                (payload, context) -> context.enqueueWork(() ->
                        Minecraft.getInstance().setScreen(new PlaneShiftTitleScreen())));
        event.register(OpenCourseMapPayload.TYPE,
                (payload, context) -> context.enqueueWork(() ->
                        Minecraft.getInstance().setScreen(new CourseMapScreen())));
        event.register(ScorePopupPayload.TYPE,
                (payload, context) -> context.enqueueWork(() ->
                        com.studio.planeshift.client.hud.ScorePopups.add(payload.amount())));
        event.register(OpenToadShopPayload.TYPE,
                (payload, context) -> context.enqueueWork(() ->
                        Minecraft.getInstance().setScreen(new ToadShopScreen())));
    }

    /** Helper: open the Toad shop screen safely from a payload handler. */
    public static void openToadShop() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null) {
            minecraft.setScreen(new ToadShopScreen());
        }
    }
}
