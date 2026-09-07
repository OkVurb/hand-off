package com.studio.planeshift.common.registry;

import com.studio.planeshift.PlaneShift;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * The course's own lava and water.
 *
 * <h2>Why real fluids and not hazard blocks</h2>
 *
 * <p>A solid block can be made to hurt you, and that is most of what lava does. What it cannot do
 * is any of the things that make being <em>in</em> a liquid feel like anything: the screen does not
 * tint, fog does not close in, you do not swim, and the surface does not render as something you
 * are looking through. All of that lives on {@link FluidType} and its client extensions, and none
 * of it is reachable from a block.
 *
 * <p>Water is the stronger case. The reference footage has water levels as a whole level type with
 * their own movement, and {@code WorldDefinition.hasUnderwaterStage} has sat in this codebase as
 * dead code with zero callers for its entire life. Swimming needs a fluid; there is no version of
 * an underwater course built out of solid blocks.
 *
 * <h2>Why they do not flow</h2>
 *
 * <p>This is the part that would otherwise break courses. A generated course places lava in a pit
 * and expects it to stay there. Vanilla fluid spreads: over the lip, along the floor, across ground
 * {@code CourseReachability} already proved traversable — and it does that <em>after</em> the proof
 * was made, so the guarantee stops describing the course the player is standing in.
 *
 * <p>So both are configured to die immediately: a level decrease of the full range means a source
 * has nothing left to give a neighbour, and a slope-find distance of zero means it does not go
 * looking for anywhere to fall. They render, tint, drown and burn exactly like a liquid, and they
 * stay in the hole the generator dug for them.
 */
public final class ModFluids {

    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, PlaneShift.MOD_ID);
    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(Registries.FLUID, PlaneShift.MOD_ID);
    public static final DeferredRegister.Blocks FLUID_BLOCKS =
            DeferredRegister.createBlocks(PlaneShift.MOD_ID);

    /**
     * A full level of decrease per block.
     *
     * <p>Fluid levels run 1 to 8, so taking eight away leaves a neighbour nothing and the fluid
     * cannot spread. This is the whole non-flowing mechanism and it is one number.
     */
    private static final int NO_SPREAD = 8;

    // ------------------------------------------------------------------ lava

    public static final DeferredHolder<FluidType, FluidType> LAVA_TYPE =
            FLUID_TYPES.register("course_lava", () -> new FluidType(FluidType.Properties.create()
                    .lightLevel(15)
                    .density(3000)
                    .viscosity(6000)
                    .temperature(1300)
                    .canSwim(false)
                    .canDrown(false)
                    .canPushEntity(false)
                    .supportsBoating(false)));

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> LAVA =
            FLUIDS.register("course_lava", () -> new BaseFlowingFluid.Source(lavaProperties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> LAVA_FLOWING =
            FLUIDS.register("course_lava_flowing", () -> new BaseFlowingFluid.Flowing(lavaProperties()));

    // registerBlock(name, factory, properties), not register(name, supplier).
    //
    // The supplier form builds the block without stamping the registry id onto its Properties, and
    // modern NeoForge requires one -- the failure is a NullPointerException reading "Block id not
    // set" thrown during RegisterEvent, which takes the whole mod down at load rather than failing
    // anywhere near this line.
    public static final DeferredHolder<Block, LiquidBlock> LAVA_BLOCK =
            FLUID_BLOCKS.registerBlock("course_lava_block",
                    props -> new LiquidBlock(LAVA.get(), props),
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.FIRE)
                            .replaceable()
                            .strength(100.0F)
                            .pushReaction(PushReaction.DESTROY)
                            .noLootTable()
                            .liquid()
                            .lightLevel(state -> 15));

    private static BaseFlowingFluid.Properties lavaProperties() {
        return new BaseFlowingFluid.Properties(LAVA_TYPE, LAVA, LAVA_FLOWING)
                .block(LAVA_BLOCK)
                .slopeFindDistance(0)
                .levelDecreasePerBlock(NO_SPREAD)
                .tickRate(30);
    }

    // ------------------------------------------------------------------ water

    public static final DeferredHolder<FluidType, FluidType> WATER_TYPE =
            FLUID_TYPES.register("course_water", () -> new FluidType(FluidType.Properties.create()
                    .density(1000)
                    .viscosity(1000)
                    .temperature(300)
                    .canSwim(true)
                    .canDrown(true)
                    .canHydrate(true)
                    .supportsBoating(true)));

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> WATER =
            FLUIDS.register("course_water", () -> new BaseFlowingFluid.Source(waterProperties()));
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> WATER_FLOWING =
            FLUIDS.register("course_water_flowing", () -> new BaseFlowingFluid.Flowing(waterProperties()));

    public static final DeferredHolder<Block, LiquidBlock> WATER_BLOCK =
            FLUID_BLOCKS.registerBlock("course_water_block",
                    props -> new LiquidBlock(WATER.get(), props),
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.WATER)
                            .replaceable()
                            .strength(100.0F)
                            .pushReaction(PushReaction.DESTROY)
                            .noLootTable()
                            .liquid());

    private static BaseFlowingFluid.Properties waterProperties() {
        return new BaseFlowingFluid.Properties(WATER_TYPE, WATER, WATER_FLOWING)
                .block(WATER_BLOCK)
                .slopeFindDistance(0)
                .levelDecreasePerBlock(NO_SPREAD)
                .tickRate(5);
    }

    private ModFluids() {
    }
}
