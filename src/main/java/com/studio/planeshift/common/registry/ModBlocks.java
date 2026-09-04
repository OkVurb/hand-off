package com.studio.planeshift.common.registry;

import com.studio.planeshift.PlaneShift;
import com.studio.planeshift.common.block.BrickBlock;
import com.studio.planeshift.common.block.BulletBillCannonBlock;
import com.studio.planeshift.common.block.CheckpointBeaconBlock;
import com.studio.planeshift.common.block.CoinBlock;
import com.studio.planeshift.common.block.CoinRingBlock;
import com.studio.planeshift.common.block.ConveyorBlock;
import com.studio.planeshift.common.block.CourseIceBlock;
import com.studio.planeshift.common.block.FlagPoleBlock;
import com.studio.planeshift.common.block.AxeBlock;
import com.studio.planeshift.common.block.CourseVineBlock;
import com.studio.planeshift.common.block.DonutBlock;
import com.studio.planeshift.common.block.SecretVineBlock;
import com.studio.planeshift.common.block.HiddenQuestionBlock;
import com.studio.planeshift.common.block.LoopTriggerBlock;
import com.studio.planeshift.common.block.MuncherBlock;
import com.studio.planeshift.common.block.MusicBlock;
import com.studio.planeshift.common.block.OnOffBlock;
import com.studio.planeshift.common.block.OnOffSwitchBlock;
import com.studio.planeshift.common.block.PrizeCacheBlock;
import com.studio.planeshift.common.block.PSwitchBlock;
import com.studio.planeshift.common.block.PlaneshiftNoteBlock;
import com.studio.planeshift.common.block.QuestionBlock;
import com.studio.planeshift.common.block.RotatingBlock;
import com.studio.planeshift.common.block.SecretPassageBlock;
import com.studio.planeshift.common.block.ShiftGateBlock;
import com.studio.planeshift.common.block.SpikeBlock;
import com.studio.planeshift.common.block.SpringPadBlock;
import com.studio.planeshift.common.block.TrampolineBlock;
import com.studio.planeshift.common.block.WarpPipeBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Course object blocks (Design Bible, "Blocks, objects, hazards, and portals").
 * Vertical-slice families: Shift, Checkpoint, Movement, Reward.
 */
public final class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(PlaneShift.MOD_ID);

    /** Bright, durable terrain tiles used by the generated 2.5D courses. */
    public static final DeferredBlock<Block> COURSE_GRASS_BLOCK = courseBlock(
            "course_grass_block", MapColor.GRASS, SoundType.GRASS);
    public static final DeferredBlock<Block> COURSE_DIRT_BLOCK = courseBlock(
            "course_dirt_block", MapColor.DIRT, SoundType.GRAVEL);
    public static final DeferredBlock<Block> COURSE_CLOUD_BLOCK = courseBlock(
            "course_cloud_block", MapColor.SNOW, SoundType.WOOL);
    public static final DeferredBlock<Block> COURSE_SAND_BLOCK = courseBlock(
            "course_sand_block", MapColor.SAND, SoundType.SAND);
    // ------------------------------------------------------------------ decoration
    //
    // Blocks with no behaviour at all. A course made only of blocks that do something reads as a
    // machine rather than as a place: every surface is a promise, so the player stops trusting
    // that anything is just scenery. These exist to be ignored, which is what makes the blocks
    // that matter stand out.

    /** Patterned floor tile. */
    public static final DeferredBlock<Block> COURSE_TILE = courseBlock(
            "course_tile", MapColor.STONE, SoundType.STONE);
    /** Fluted column, for framing an arena or a doorway. */
    public static final DeferredBlock<Block> COURSE_PILLAR = courseBlock(
            "course_pillar", MapColor.STONE, SoundType.STONE);
    /** Open lattice, useful as a railing or a window. */
    public static final DeferredBlock<Block> COURSE_LATTICE = courseBlock(
            "course_lattice", MapColor.WOOD, SoundType.WOOD);
    /** Wooden crate. Reads as "someone lives here" more than any amount of terrain does. */
    public static final DeferredBlock<Block> COURSE_CRATE = courseBlock(
            "course_crate", MapColor.WOOD, SoundType.WOOD);
    /** Hanging cloth banner. */
    public static final DeferredBlock<Block> COURSE_BANNER = courseBlock(
            "course_banner", MapColor.COLOR_RED, SoundType.WOOL);
    /** Clipped hedge. */
    public static final DeferredBlock<Block> COURSE_HEDGE = courseBlock(
            "course_hedge", MapColor.PLANT, SoundType.GRASS);
    /** Carved trim, for the top of a wall. */
    public static final DeferredBlock<Block> COURSE_TRIM = courseBlock(
            "course_trim", MapColor.STONE, SoundType.STONE);

    /** Lamp. The only decorative block that emits light, so it doubles as a signpost. */
    public static final DeferredBlock<Block> COURSE_LAMP =
            BLOCKS.registerSimpleBlock("course_lamp", properties -> properties
                    .mapColor(MapColor.COLOR_YELLOW)
                    .strength(1.0F, 3.0F)
                    .sound(SoundType.LANTERN)
                    .lightLevel(state -> 14));

    public static final DeferredBlock<Block> COURSE_SNOW_BLOCK =
            BLOCKS.registerSimpleBlock("course_snow_block", properties -> properties
                    .mapColor(MapColor.ICE)
                    .strength(1.5F, 6.0F)
                    .sound(SoundType.SNOW)
                    // 0.98 is vanilla ice. Snow is not ice: giving both the same figure meant every
                    // snow course was a single uninterruptible slide, and left COURSE_ICE_BLOCK with
                    // nothing to say. 0.85 is slippery enough to feel like footing you have to
                    // respect, loose enough to still stop on a ledge.
                    .friction(0.85F));
    public static final DeferredBlock<Block> COURSE_CASTLE_BLOCK = courseBlock(
            "course_castle_block", MapColor.DEEPSLATE, SoundType.DEEPSLATE_BRICKS);
    public static final DeferredBlock<Block> COURSE_MAGMA_BLOCK = courseBlock(
            "course_magma_block", MapColor.FIRE, SoundType.NETHER_BRICKS);
    public static final DeferredBlock<Block> COURSE_WOOD_BLOCK = courseBlock(
            "course_wood_block", MapColor.WOOD, SoundType.WOOD);
    public static final DeferredBlock<Block> COURSE_HARD_BLOCK = courseBlock(
            "course_hard_block", MapColor.STONE, SoundType.STONE);

    public static final DeferredBlock<ShiftGateBlock> SHIFT_GATE =
            BLOCKS.registerBlock("shift_gate", ShiftGateBlock::new, p -> p
                    .mapColor(MapColor.COLOR_CYAN)
                    .strength(-1.0F, 3_600_000.0F)
                    .noCollision()
                    .noLootTable()
                    .lightLevel(state -> 11)
                    .noOcclusion()
                    .sound(SoundType.AMETHYST));

    public static final DeferredBlock<CheckpointBeaconBlock> CHECKPOINT_BEACON =
            BLOCKS.registerBlock("checkpoint_beacon", CheckpointBeaconBlock::new, p -> p
                    .mapColor(MapColor.GOLD)
                    .strength(1.5F)
                    .lightLevel(state -> state.getValue(CheckpointBeaconBlock.LIT) ? 13 : 4)
                    .noCollision()
                    .noOcclusion()
                    .sound(SoundType.COPPER));

    public static final DeferredBlock<SpringPadBlock> SPRING_PAD =
            BLOCKS.registerBlock("spring_pad", SpringPadBlock::new, p -> p
                    .mapColor(MapColor.COLOR_LIGHT_GREEN)
                    .strength(0.8F)
                    .noOcclusion()
                    .sound(SoundType.SLIME_BLOCK));

    public static final DeferredBlock<PrizeCacheBlock> PRIZE_CACHE =
            BLOCKS.registerBlock("prize_cache", PrizeCacheBlock::new, p -> p
                    .mapColor(MapColor.TERRACOTTA_YELLOW)
                    .strength(1.0F)
                    .sound(SoundType.CHISELED_BOOKSHELF));

    /** Mario-style pickups and rewards. */
    public static final DeferredBlock<CoinBlock> COIN_BLOCK =
            BLOCKS.registerBlock("coin_block", CoinBlock::new, p -> p
                    .mapColor(MapColor.COLOR_YELLOW)
                    .strength(0.8F)
                    .sound(SoundType.WOOD));

    public static final DeferredBlock<CoinRingBlock> COIN_RING_BLOCK =
            BLOCKS.registerBlock("coin_ring_block", CoinRingBlock::new, p -> p
                    .mapColor(MapColor.GOLD)
                    .strength(0.8F)
                    .noOcclusion()
                    .sound(SoundType.WOOD));

    public static final DeferredBlock<QuestionBlock> QUESTION_BLOCK =
            BLOCKS.registerBlock("question_block", QuestionBlock::new, p -> p
                    .mapColor(MapColor.COLOR_YELLOW)
                    .strength(0.8F)
                    .sound(SoundType.WOOD));

    public static final DeferredBlock<BrickBlock> BRICK_BLOCK =
            BLOCKS.registerBlock("brick_block", BrickBlock::new, p -> p
                    .mapColor(MapColor.COLOR_RED)
                    .strength(1.0F)
                    .noLootTable()
                    .sound(SoundType.STONE));

    public static final DeferredBlock<RotatingBlock> ROTATING_BLOCK =
            BLOCKS.registerBlock("rotating_block", RotatingBlock::new, p -> p
                    .mapColor(MapColor.COLOR_YELLOW)
                    .strength(1.2F)
                    .sound(SoundType.WOOD));

    /** Course mechanics and hazards. */
    public static final DeferredBlock<ConveyorBlock> CONVEYOR_BELT =
            BLOCKS.registerBlock("conveyor_belt", ConveyorBlock::new, p -> p
                    .mapColor(MapColor.TERRACOTTA_GRAY)
                    .strength(1.0F)
                    .noOcclusion()
                    .sound(SoundType.STONE));

    public static final DeferredBlock<PlaneshiftNoteBlock> NOTE_BLOCK =
            BLOCKS.registerBlock("note_block", PlaneshiftNoteBlock::new, p -> p
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(0.8F)
                    .sound(SoundType.WOOD));

    public static final DeferredBlock<MusicBlock> MUSIC_BLOCK =
            BLOCKS.registerBlock("music_block", MusicBlock::new, p -> p
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(0.8F)
                    .sound(SoundType.WOOD));

    public static final DeferredBlock<OnOffBlock> ON_OFF_BLOCK =
            BLOCKS.registerBlock("on_off_block", OnOffBlock::new, p -> p
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(1.0F)
                    .lightLevel(state -> state.getValue(OnOffBlock.ON) ? 13 : 0)
                    .sound(SoundType.METAL));

    public static final DeferredBlock<OnOffSwitchBlock> ON_OFF_SWITCH =
            BLOCKS.registerBlock("on_off_switch", OnOffSwitchBlock::new, p -> p
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(1.0F)
                    .noOcclusion()
                    .sound(SoundType.METAL));

    public static final DeferredBlock<PSwitchBlock> P_SWITCH =
            BLOCKS.registerBlock("p_switch", PSwitchBlock::new, p -> p
                    .mapColor(MapColor.COLOR_BLUE)
                    .strength(1.0F)
                    .noOcclusion()
                    .sound(SoundType.STONE));

    public static final DeferredBlock<SpikeBlock> SPIKE_BLOCK =
            BLOCKS.registerBlock("spike_block", SpikeBlock::new, p -> p
                    .mapColor(MapColor.COLOR_RED)
                    .strength(1.0F)
                    .noOcclusion()
                    .sound(SoundType.STONE));

    /** Hidden question block - invisible until hit from below. */
    /** Falls away shortly after the player stands on it, then returns. */
    public static final DeferredBlock<DonutBlock> DONUT_BLOCK =
            BLOCKS.registerBlock("donut_block", DonutBlock::new, p -> p
                    .mapColor(MapColor.COLOR_BROWN)
                    .strength(1.0F)
                    .sound(SoundType.WOOD));

    /** Taking it collapses the castle bridge tile by tile. */
    public static final DeferredBlock<AxeBlock> AXE_BLOCK =
            BLOCKS.registerBlock("axe_block", AxeBlock::new, p -> p
                    .mapColor(MapColor.METAL)
                    .strength(2.0F)
                    .noOcclusion()
                    .noCollision()
                    .sound(SoundType.METAL));

    /** Hidden until hit from below, then grows a climbable vine upward. */
    public static final DeferredBlock<SecretVineBlock> SECRET_VINE =
            BLOCKS.registerBlock("secret_vine", SecretVineBlock::new, p -> p
                    .mapColor(MapColor.PLANT)
                    .strength(0.8F)
                    .noOcclusion()
                    .noCollision()
                    .sound(SoundType.GRASS));

    /** The climbable stalk grown by SECRET_VINE. */
    public static final DeferredBlock<CourseVineBlock> COURSE_VINE =
            BLOCKS.registerBlock("course_vine", CourseVineBlock::new, p -> p
                    .mapColor(MapColor.PLANT)
                    .strength(0.2F)
                    .noOcclusion()
                    .noCollision()
                    .sound(SoundType.VINE));

    public static final DeferredBlock<HiddenQuestionBlock> HIDDEN_QUESTION_BLOCK =
            BLOCKS.registerBlock("hidden_question_block", HiddenQuestionBlock::new, p -> p
                    .mapColor(MapColor.COLOR_YELLOW)
                    .strength(0.8F)
                    .noOcclusion()
                    .sound(SoundType.WOOD));

    /** Course end marker. */
    public static final DeferredBlock<FlagPoleBlock> FLAG_POLE =
            BLOCKS.registerBlock("flag_pole", FlagPoleBlock::new, p -> p
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(0.4F)
                    .noCollision()
                    .noOcclusion()
                    .sound(SoundType.METAL));

    /** Mario-style transport. */
    public static final DeferredBlock<WarpPipeBlock> WARP_PIPE =
            BLOCKS.registerBlock("warp_pipe", WarpPipeBlock::new, p -> p
                    .mapColor(MapColor.COLOR_GREEN)
                    .strength(1.5F)
                    .noOcclusion()
                    .sound(SoundType.METAL));

    /** Secret area block — looks solid, has no collision. */
    public static final DeferredBlock<SecretPassageBlock> SECRET_PASSAGE =
            BLOCKS.registerBlock("secret_passage", SecretPassageBlock::new, p -> p
                    .mapColor(MapColor.COLOR_RED)
                    .strength(0.2F)
                    .noCollision()
                    .noOcclusion()
                    .noLootTable()
                    .sound(SoundType.STONE));

    public static final DeferredBlock<LoopTriggerBlock> LOOP_TRIGGER =
            BLOCKS.registerBlock("loop_trigger", LoopTriggerBlock::new, p -> p
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(-1.0F, 3_600_000.0F)
                    .noCollision()
                    .noOcclusion()
                    .noLootTable()
                    .sound(SoundType.STONE));

    public static final DeferredBlock<BulletBillCannonBlock> BULLET_BILL_CANNON =
            BLOCKS.registerBlock("bullet_bill_cannon", BulletBillCannonBlock::new, p -> p
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(3.5F, 12.0F)
                    .sound(SoundType.ANVIL));

    public static final DeferredBlock<CourseIceBlock> COURSE_ICE_BLOCK =
            BLOCKS.registerBlock("course_ice_block", CourseIceBlock::new, p -> p
                    .mapColor(MapColor.ICE)
                    .strength(0.5F)
                    .friction(0.98F)
                    .noOcclusion()
                    .sound(SoundType.GLASS));

    public static final DeferredBlock<MuncherBlock> MUNCHER =
            BLOCKS.registerBlock("muncher", MuncherBlock::new, p -> p
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(1.5F, 10.0F)
                    .noOcclusion()
                    .sound(SoundType.GRASS));

    public static final DeferredBlock<TrampolineBlock> TRAMPOLINE =
            BLOCKS.registerBlock("trampoline", TrampolineBlock::new, p -> p
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(0.8F)
                    .noOcclusion()
                    .sound(SoundType.WOOL));

    private static DeferredBlock<Block> courseBlock(String name, MapColor mapColor,
                                                     SoundType sound) {
        return BLOCKS.registerSimpleBlock(name, properties -> properties
                .mapColor(mapColor)
                .strength(1.5F, 6.0F)
                .sound(sound));
    }

    private ModBlocks() {
    }
}

