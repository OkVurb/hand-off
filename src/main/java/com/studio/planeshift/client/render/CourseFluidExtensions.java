package com.studio.planeshift.client.render;

import com.studio.planeshift.PlaneShift;
import com.studio.planeshift.common.registry.ModFluids;
import net.minecraft.client.Camera;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import org.joml.Vector4f;

/**
 * How the course fluids look, and what being inside one does to the screen.
 *
 * <p>This is the half of a fluid a block cannot do. A solid hazard block can hurt you and can be
 * painted to look like lava, and there it stops: the screen does not tint, fog does not close in,
 * and the surface renders as an opaque face rather than as something you are looking through.
 * Every one of those lives here.
 *
 * <p>The fog is the part worth tuning carefully. It is what sells being submerged -- vision closes
 * to a short distance and everything takes the liquid's colour -- but it is also the single
 * easiest way to make a level unplayable. A platformer is a game about seeing the jump before you
 * commit to it, so underwater fog is set far enough back that the next platform is still readable,
 * and lava fog is tight, because being inside lava is not a place you are meant to be surveying
 * from.
 */
public final class CourseFluidExtensions {

    /**
     * How far you can see underwater, in blocks.
     *
     * <p>Effectively unlimited, which is the genre's answer rather than a compromise. Underwater
     * levels in the reference are not murky at all: the water is a colour over a fully visible
     * screen, and every platform, fish and coin in the room is legible from the moment it comes on
     * screen. Fog is how a first-person game says "you are submerged"; a side-on platformer says it
     * with the tint and then gets out of the way.
     *
     * <p>An earlier pass set this to 24 blocks and called it generous. That was still reasoning
     * from vanilla -- pushing a Minecraft default outward instead of asking what the game being
     * imitated actually does. Set past any distance the camera can see, so the value is really
     * "none" and says so.
     */
    private static final float WATER_FOG_FAR = 128.0F;
    private static final float WATER_FOG_NEAR = 64.0F;

    /** Lava is nearly blind, because being in it is a mistake and not a vantage point. */
    private static final float LAVA_FOG_FAR = 3.0F;
    private static final float LAVA_FOG_NEAR = 0.2F;

    private CourseFluidExtensions() {
    }

    public static void register(RegisterClientExtensionsEvent event) {
        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override
            public Identifier getStillTexture() {
                return PlaneShift.id("block/course_water_still");
            }

            @Override
            public Identifier getFlowingTexture() {
                return PlaneShift.id("block/course_water_flow");
            }

            /**
             * Tints the surface and, with the texture's own alpha, is what makes it see-through.
             * Kept light: the texture already carries the colour, and multiplying a blue tint over
             * a blue texture turns a pool into a solid navy slab.
             */
            @Override
            public int getTintColor() {
                return 0xC0_6FB4E8;
            }

            @Override
            public Vector4f modifyFogColor(Camera camera, float partialTick,
                                           net.minecraft.client.multiplayer.ClientLevel level,
                                           int renderDistance, float darkenWorldAmount,
                                           Vector4f fluidFogColor) {
                return new Vector4f(0.16F, 0.42F, 0.62F, 1.0F);
            }

            /** No fog worth the name; the tint above does all the work. See the constants. */
            @Override
            public void modifyFogRender(Camera camera,
                                        net.minecraft.client.renderer.fog.environment.FogEnvironment environment,
                                        float renderDistance, float partialTick,
                                        net.minecraft.client.renderer.fog.FogData data) {
                data.environmentalStart = WATER_FOG_NEAR;
                data.environmentalEnd = WATER_FOG_FAR;
            }
        }, ModFluids.WATER_TYPE.get());

        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override
            public Identifier getStillTexture() {
                return PlaneShift.id("block/course_lava_still");
            }

            @Override
            public Identifier getFlowingTexture() {
                return PlaneShift.id("block/course_lava_flow");
            }

            @Override
            public Vector4f modifyFogColor(Camera camera, float partialTick,
                                           net.minecraft.client.multiplayer.ClientLevel level,
                                           int renderDistance, float darkenWorldAmount,
                                           Vector4f fluidFogColor) {
                return new Vector4f(0.72F, 0.22F, 0.04F, 1.0F);
            }

            /** Nearly blind. Being inside lava is a mistake, not a vantage point. */
            @Override
            public void modifyFogRender(Camera camera,
                                        net.minecraft.client.renderer.fog.environment.FogEnvironment environment,
                                        float renderDistance, float partialTick,
                                        net.minecraft.client.renderer.fog.FogData data) {
                data.environmentalStart = LAVA_FOG_NEAR;
                data.environmentalEnd = LAVA_FOG_FAR;
            }
        }, ModFluids.LAVA_TYPE.get());
    }

    /** Convenience for anything that needs to know a state is one of ours. */
    public static boolean isCourseFluid(BlockAndTintGetter level, BlockState state) {
        return state.getFluidState().getType() == ModFluids.WATER.get()
                || state.getFluidState().getType() == ModFluids.LAVA.get();
    }
}
