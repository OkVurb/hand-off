package com.studio.planeshift.common.registry;

import com.studio.planeshift.PlaneShift;
import com.studio.planeshift.common.block.BrickBlock;
import com.studio.planeshift.common.block.BulletBillCannonBlock;
import com.studio.planeshift.common.block.CheckpointBeaconBlock;
import com.studio.planeshift.common.block.CoinBlock;
import com.studio.planeshift.common.block.ConnectedBlock;
import com.studio.planeshift.common.block.CoinRingBlock;
import com.studio.planeshift.common.block.ConveyorBlock;
import com.studio.planeshift.common.block.CourseIceBlock;
import com.studio.planeshift.common.block.FlagPoleBlock;
import com.studio.planeshift.common.block.AxeBlock;
import com.studio.planeshift.common.block.CourseVineBlock;
import com.studio.planeshift.common.block.DonutBlock;
import com.studio.planeshift.common.block.SecretVineBlock;
import com.studio.planeshift.common.block.HiddenQuestionBlock;
import com.studio.planeshift.common.block.KeyholeBlock;
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
import com.studio.planeshift.common.block.SemisolidBlock;
import com.studio.planeshift.common.block.ToadBoxBlock;
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
    public static final DeferredBlock<ConnectedBlock> COURSE_GRASS_BLOCK = connectedBlock(
            "course_grass_block", MapColor.GRASS, SoundType.GRASS);
    /**
     * The fill under every grass level, and the most-repeated surface in the game.
     *
     * <p>A {@link ConnectedBlock} for that reason. It was a plain block, so a wall of it drew the
     * same sixteen pixels over and over with a hard seam at every boundary -- the exact grid the
     * connected machinery exists to remove, on the one block that shows it most. Grass and sand had
     * been converted and the block underneath them had not.
     */
    public static final DeferredBlock<ConnectedBlock> COURSE_DIRT_BLOCK = connectedBlock(
            "course_dirt_block", MapColor.DIRT, SoundType.GRAVEL);
    public static final DeferredBlock<Block> COURSE_CLOUD_BLOCK = courseBlock(
            "course_cloud_block", MapColor.SNOW, SoundType.WOOL);
    public static final DeferredBlock<ConnectedBlock> COURSE_SAND_BLOCK = connectedBlock(
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
    /**
     * Distant copies of the scenery the decorator places at the far depth.
     *
     * <p>Separate blocks rather than a property, because a texture is chosen by block and this is
     * purely a texture difference. They exist because depth was carried entirely by geometry: the
     * decorator picked z=2 or z=3 and then placed the identical block at either, so a prop
     * "behind" another prop was pixel-identical to it. These are the same silhouettes washed out
     * by air -- see {@code distant()} in BlockTextureGen -- which is the cue the eye actually
     * reads distance from.
     */
    /**
     * Native terrain materials, replacing the vanilla blocks five of the six themes were built
     * out of.
     *
     * <p>A theme's fill sits under every surface block, three deep, so it is on screen constantly.
     * A vanilla block there brings Minecraft's palette and drawing conventions with it and takes
     * none of this mod's: not the lighting ramp, not the connected edges, not the aerial
     * perspective. Two materials in one frame drawn by two different hands, and the fill is the
     * one you see most.
     */
    public static final DeferredBlock<ConnectedBlock> COURSE_SANDSTONE = connectedBlock(
            "course_sandstone", MapColor.SAND, SoundType.STONE);
    public static final DeferredBlock<Block> COURSE_DESERT_BRICK = courseBlock(
            "course_desert_brick", MapColor.TERRACOTTA_ORANGE, SoundType.STONE);
    public static final DeferredBlock<ConnectedBlock> COURSE_BASALT = connectedBlock(
            "course_basalt", MapColor.COLOR_BLACK, SoundType.STONE);
    public static final DeferredBlock<ConnectedBlock> COURSE_DEEPSTONE = connectedBlock(
            "course_deepstone", MapColor.DEEPSLATE, SoundType.DEEPSLATE);
    public static final DeferredBlock<Block> COURSE_GHOST_BEAM = courseBlock(
            "course_ghost_beam", MapColor.WOOD, SoundType.WOOD);

    /**
     * The third depth band, for the big background silhouettes.
     *
     * <p>Hazed harder than the FAR set. Reference backgrounds run three layers, not two, and they
     * are told apart by how washed out they are rather than by how far back they sit -- so a third
     * band is only a third band if it is visibly paler than the second.
     */
    public static final DeferredBlock<Block> COURSE_HEDGE_DISTANT = courseBlock(
            "course_hedge_distant", MapColor.PLANT, SoundType.GRASS);
    public static final DeferredBlock<Block> COURSE_WOOD_DISTANT = courseBlock(
            "course_wood_distant", MapColor.WOOD, SoundType.WOOD);

    /**
     * The backdrop set hazed toward hot air rather than cold sky.
     *
     * <p>Aerial perspective is not a single colour. A volcano tints its whole scene orange,
     * foreground included; washing its far layer toward the same blue-grey used everywhere else
     * would say "distant and cold" in the one room where that is exactly wrong.
     */
    /**
     * Seafloor growth: the accent block for submerged courses.
     *
     * <p>The one piece of genuinely new art water needs. Everything else it reuses from land,
     * which is what makes the whole theme cheap -- coral is the thing that says "under water"
     * without a single new terrain block.
     */
    /**
     * A striped climbing pole.
     *
     * <p>The second climbable in the mod, after the vine, and it exists because the reference has
     * two and uses them for different things: a vine hangs on a wall and is climbed where it is,
     * while a pole stands free in a room and is the reason to cross to it.
     *
     * <p>The spiral stripe is not decoration. A climbing player is rendered with the same pose
     * whether they are moving or not, so nothing on the character says the climb is happening --
     * the stripe passing the eye is what carries the motion, and a plain pole reads as standing
     * still against a post.
     *
     * <p>A {@link CourseVineBlock}, because that class is already "ordinary climbable scenery that
     * may exist in quantity", which is exactly what this is. A new class would have been the same
     * code under a different name.
     */
    public static final DeferredBlock<CourseVineBlock> COURSE_CLIMB_POLE =
            BLOCKS.registerBlock("course_climb_pole", CourseVineBlock::new,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_RED)
                            .strength(0.6F)
                            .noOcclusion()
                            .sound(SoundType.WOOD));

    /** The ghost-house lamp: same fitting, cold green flame. See BlockTextureGen. */
    public static final DeferredBlock<Block> COURSE_LAMP_GHOST =
            BLOCKS.registerBlock("course_lamp_ghost", Block::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GREEN)
                    .strength(0.6F)
                    .sound(SoundType.LANTERN)
                    .lightLevel(state -> 14));

    /**
     * An arrow board, planted where a route is ambiguous.
     *
     * <p>Navigation as set dressing rather than as UI. The reference puts these at forks and above
     * hidden entrances, and the value is that they are part of the level -- a HUD arrow tells the
     * player where to go, a sign in the world tells them somebody built this place with a way
     * through it.
     */
    /**
     * Grinder track.
     *
     * <p>The saw's route, built rather than drawn. An earlier version showed the path in particles;
     * this is the same information as a thing that is actually there, which is how the genre does
     * it and how everything else in this mod already worked.
     *
     * <p>Not solid: the player passes through it, so a rail across a corridor never becomes a wall.
     */
    public static final DeferredBlock<Block> COURSE_RAIL =
            BLOCKS.registerBlock("course_rail", Block::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(0.8F)
                    .noOcclusion()
                    .noCollision()
                    .sound(SoundType.CHAIN));

    /**
     * A leaded window for castle interiors.
     *
     * <p>The art-direction entry about visible light sources was half done: sconces and lanterns
     * existed and were placed, and the note said stained glass in castle back walls remained. This
     * is that. It does a job the lamps cannot -- light through a window means there is an outside,
     * which is most of what stops a castle interior reading as a cave with square walls.
     */
    public static final DeferredBlock<Block> COURSE_GLASS = courseBlock(
            "course_glass", MapColor.COLOR_BLUE, SoundType.GLASS);

    /**
     * Ground cover that stands on the playfield itself.
     *
     * <p>Every decorator prop in this mod sits behind the lane, which is why the walkable surface
     * reads as a shelf the level is displayed on rather than as ground. The reference puts flowers,
     * tufts and fences on the surface the player runs along, and the difference is most of what
     * makes its levels look inhabited rather than assembled.
     *
     * <p>No collision, and drawn only in the lower part of its own texture. A decoration on the
     * floor must never be mistakable for something to stand on or trip over -- the moment the
     * player has to think about whether a tuft is solid, it has cost more than it gave.
     */
    public static final DeferredBlock<Block> COURSE_TUFT = courseBlock(
            "course_tuft", MapColor.PLANT, SoundType.GRASS, true);

    /** The same, in snow's palette. */
    public static final DeferredBlock<Block> COURSE_TUFT_SNOW = courseBlock(
            "course_tuft_snow", MapColor.SNOW, SoundType.GRASS, true);

    /** And in the desert's: dry, pale, and sparser than the green one. */
    public static final DeferredBlock<Block> COURSE_TUFT_DESERT = courseBlock(
            "course_tuft_desert", MapColor.SAND, SoundType.GRASS, true);

    public static final DeferredBlock<Block> COURSE_CORAL = courseBlock(
            "course_coral", MapColor.COLOR_PINK, SoundType.CORAL_BLOCK);

    public static final DeferredBlock<Block> COURSE_HEDGE_DISTANT_WARM = courseBlock(
            "course_hedge_distant_warm", MapColor.TERRACOTTA_ORANGE, SoundType.GRASS);
    public static final DeferredBlock<Block> COURSE_WOOD_DISTANT_WARM = courseBlock(
            "course_wood_distant_warm", MapColor.TERRACOTTA_ORANGE, SoundType.WOOD);

    /**
     * The banding drawn inside a far hill.
     *
     * <p>Its own block rather than a reuse of {@code COURSE_WOOD_DISTANT}, and the reason is a test
     * that could not otherwise exist: a stripe sharing a material with tree trunks cannot be told
     * from the forest, and the first two versions of {@code HillPatternTest} passed with the
     * banding switched off because they were counting trees. A pattern needs to be distinguishable
     * from the things it is drawn among, by the player and by the build.
     */
    /**
     * The bonus room's own materials: flat plates with bolts, and a loud frame.
     *
     * <p>§5.10. Every other block in this mod is a material — stone, sand, timber, ice — and these
     * are deliberately manufactured, because that contrast is how the reference says "you have
     * stepped outside the game" without a word of text. The grain and mortar every other texture
     * here spends its pixels on is exactly what these must not have.
     *
     * <p>The border is the one place a block in this mod is allowed to be a pure primary. It is a
     * frame rather than scenery: its whole job is to say where the bonus room stops.
     */
    public static final DeferredBlock<Block> COURSE_BONUS_PLATE_PINK = courseBlock(
            "course_bonus_plate_pink", MapColor.COLOR_PINK, SoundType.METAL);

    /** The other plate. Two colours, so a wall of them is a pattern rather than a slab. */
    public static final DeferredBlock<Block> COURSE_BONUS_PLATE_BLUE = courseBlock(
            "course_bonus_plate_blue", MapColor.COLOR_LIGHT_BLUE, SoundType.METAL);

    /** The frame around a bonus playfield. */
    public static final DeferredBlock<Block> COURSE_BONUS_BORDER = courseBlock(
            "course_bonus_border", MapColor.COLOR_RED, SoundType.METAL);

    /**
     * A thin ledge with a sunk centre panel.
     *
     * <p>§4.8's parts kit. The lip is the point: it is the brightest row on the block, so the
     * surface the player lands on is the lightest thing about it from any distance. A ledge drawn
     * as one flat colour gives the eye nothing to judge a jump against except its outline, which is
     * the first thing lost when the camera is far enough back to show a whole room.
     */
    public static final DeferredBlock<Block> COURSE_LEDGE = courseBlock(
            "course_ledge", MapColor.STONE, SoundType.STONE);

    /**
     * Forest floor: cut logs seen end-on, mossed across the top.
     *
     * <p>§5.9, and the entry calls it the cheapest kind of identity there is — one block's face art
     * doing what a whole tileset would otherwise have to. Two log ends per tile rather than one,
     * because a single centred ring tiles into a grid of bullseyes and reads as spots.
     */
    public static final DeferredBlock<Block> COURSE_LOG_END = courseBlock(
            "course_log_end", MapColor.WOOD, SoundType.WOOD);

    /**
     * A metal grate: floor you can see through.
     *
     * <p>From §4.8's parts kit, and the one item on that list that changes what the player knows
     * rather than what the level looks like. Every other floor in this mod is opaque, so a drop is
     * a decision made blind; standing on a grate, the room below is visible before the player
     * commits to it.
     *
     * <p>Solid and opaque-free rather than semisolid. It is a floor, not a platform to jump up
     * through -- a grate you could pass upward through would be a hole with a picture of a floor
     * over it, which is the opposite of what it is for.
     */
    public static final DeferredBlock<Block> COURSE_GRATE = BLOCKS.registerSimpleBlock(
            "course_grate", properties -> properties
                    .mapColor(MapColor.METAL)
                    .strength(1.5F, 6.0F)
                    .noOcclusion()
                    .sound(SoundType.CHAIN));

    public static final DeferredBlock<Block> COURSE_HEDGE_DISTANT_BAND = courseBlock(
            "course_hedge_distant_band", MapColor.PLANT, SoundType.GRASS);

    public static final DeferredBlock<Block> COURSE_HEDGE_FAR = courseBlock(
            "course_hedge_far", MapColor.PLANT, SoundType.GRASS);
    public static final DeferredBlock<Block> COURSE_PILLAR_FAR = courseBlock(
            "course_pillar_far", MapColor.STONE, SoundType.STONE);
    public static final DeferredBlock<Block> COURSE_CLOUD_BLOCK_FAR = courseBlock(
            "course_cloud_block_far", MapColor.SNOW, SoundType.WOOL);

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
    /**
     * Castle stone, and the first block to draw its own edges from its neighbours.
     *
     * <p>A ConnectedBlock rather than a plain one. Courses are built out of walls of this -- the
     * boss arena and the tower are almost nothing else -- and a wall is exactly where the baked
     * per-block highlight read as stripes. See {@link ConnectedBlock}.
     */
    public static final DeferredBlock<ConnectedBlock> COURSE_CASTLE_BLOCK =
            BLOCKS.registerBlock("course_castle_block", ConnectedBlock::new, p -> p
                    .mapColor(MapColor.DEEPSLATE)
                    .strength(1.5F, 6.0F)
                    .sound(SoundType.DEEPSLATE_BRICKS));
    /**
     * The LAVA theme's decorative accent. <b>Not a hazard.</b>
     *
     * <p>Renamed from {@code COURSE_MAGMA_BLOCK}, because that name was actively dangerous. It is
     * registered through {@link #courseBlock} like any other wall: no damage behaviour, no tick, no
     * special collision. But three separate readers - two review agents and one of the authors -
     * independently proposed adding it to a "kills whatever stands on it" set purely on the
     * strength of the word <em>magma</em>, and one of those very nearly shipped. It is also what
     * lethal would have killed enemies standing on the goal, since the flagpole steps are laid
     * from it. (That reference used to name {@code CourseStructureService.buildFinish}, which was
     * dead code and has been removed; the reasoning holds regardless of which builder places it.)
     *
     * <p>The registry id is deliberately still {@code course_magma_block}: renaming that would
     * churn the blockstate, model, item model, lang entry and texture for no gain, and would break
     * blocks already placed in saved worlds. The trap was the Java identifier, and that is what
     * moved.
     */
    public static final DeferredBlock<Block> COURSE_EMBER_BLOCK = courseBlock(
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

    /**
     * An arrow board planted in the terrain, pointing the way on.
     *
     * <p>Navigation as set dressing rather than as UI. The reference plants these where a route is
     * ambiguous, and it is worth copying for a reason beyond fidelity: this game generates its
     * courses, so it cannot rely on a level designer having made the intended path obvious. A sign
     * is the one piece of guidance that is part of the world instead of drawn over it.
     *
     * <p>No collision, no interaction, no stored text. It is scenery that means something.
     */
    public static final DeferredBlock<Block> SIGNPOST =
            BLOCKS.registerBlock("signpost", Block::new, p -> p
                    .mapColor(MapColor.WOOD)
                    .strength(0.5F)
                    .noCollision()
                    .noOcclusion()
                    .sound(SoundType.WOOD));

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

    /**
     * The platform every 2D Mario level is built from: solid on top, open from below.
     *
     * <p>noOcclusion because the player is regularly inside one on the way up, and a block that
     * occludes while you are standing in it culls the faces of everything behind it.
     */
    /**
     * The ghost house's other exit. noCollission because the player completes the course by
     * walking into it, and a solid door would stop them doing the one thing it is for.
     */
    public static final DeferredBlock<KeyholeBlock> KEYHOLE =
            BLOCKS.registerBlock("keyhole", KeyholeBlock::new, p -> p
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(2.0F)
                    .noOcclusion()
                    .lightLevel(state -> state.getValue(KeyholeBlock.UNLOCKED) ? 12 : 4)
                    .sound(SoundType.METAL));

    /**
     * The Toad House prize box. Hit from below like a question block, but opening one closes the
     * others, so a Toad House is a choice rather than three rewards.
     */
    public static final DeferredBlock<ToadBoxBlock> TOAD_BOX =
            BLOCKS.registerBlock("toad_box", ToadBoxBlock::new, p -> p
                    .mapColor(MapColor.SNOW)
                    .strength(-1.0F, 3_600_000.0F)
                    .sound(SoundType.WOOD));

    public static final DeferredBlock<SemisolidBlock> SEMISOLID_PLATFORM =
            BLOCKS.registerBlock("semisolid_platform", SemisolidBlock::new, p -> p
                    .mapColor(MapColor.WOOD)
                    .strength(1.2F)
                    .noOcclusion()
                    .isViewBlocking((state, level, pos) -> false)
                    .sound(SoundType.WOOD));

    public static final DeferredBlock<TrampolineBlock> TRAMPOLINE =
            BLOCKS.registerBlock("trampoline", TrampolineBlock::new, p -> p
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(0.8F)
                    .noOcclusion()
                    .sound(SoundType.WOOL));

    /**
     * A course block that draws its own edges from its neighbours.
     *
     * <p>Same properties as {@link #courseBlock}; the only difference is the class, and therefore
     * the four boolean properties and the sixteen models behind them. Worth its own helper because
     * the list of blocks that want this is now longer than the list that does not: anything built
     * into walls or terrain wants it, and only props and one-off set pieces do not.
     */
    private static DeferredBlock<ConnectedBlock> connectedBlock(String name, MapColor mapColor,
                                                                 SoundType sound) {
        return BLOCKS.registerBlock(name, ConnectedBlock::new, BlockBehaviour.Properties.of()
                .mapColor(mapColor)
                .strength(1.5F, 6.0F)
                .sound(sound));
    }

    private static DeferredBlock<Block> courseBlock(String name, MapColor mapColor,
                                                     SoundType sound) {
        return courseBlock(name, mapColor, sound, false);
    }

    /**
     * As above, optionally as scenery the player passes straight through.
     *
     * <p>The flag exists for ground cover. A decoration standing on the walkable surface has to be
     * non-solid or it is furniture, and it has to say so in one place -- {@code CourseReachability}
     * keeps its own list of what the player can walk through, and the rail already taught this
     * project what happens when the two disagree: the block becomes unusable anywhere it matters.
     */
    private static DeferredBlock<Block> courseBlock(String name, MapColor mapColor,
                                                     SoundType sound, boolean scenery) {
        return BLOCKS.registerSimpleBlock(name, properties -> {
            var props = properties.mapColor(mapColor).sound(sound);
            return scenery
                    ? props.strength(0.2F).noCollision().noOcclusion()
                    : props.strength(1.5F, 6.0F);
        });
    }

    private ModBlocks() {
    }
}

