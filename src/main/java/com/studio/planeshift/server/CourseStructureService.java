package com.studio.planeshift.server;

import com.studio.planeshift.PlaneShift;
import com.studio.planeshift.common.block.FlagPoleBlock;
import com.studio.planeshift.common.course.CourseDefinition;
import com.studio.planeshift.server.gen.CourseCanvas;
import com.studio.planeshift.server.gen.CourseComposer;
import com.studio.planeshift.server.gen.CourseWriter;
import com.studio.planeshift.common.course.CourseLayout;
import com.studio.planeshift.common.course.CourseTheme;
import com.studio.planeshift.common.entity.FirebarEntity;
import com.studio.planeshift.common.entity.MovingPlatformEntity;
import com.studio.planeshift.common.registry.ModBlocks;
import com.studio.planeshift.common.registry.ModEntities;
import com.studio.planeshift.common.registry.ModItems;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.phys.AABB;

/**
 * Builds the physical course before a player is teleported into it.
 *
 * <p>Data-pack structure templates still win when configured. A course without one receives a
 * deterministic straight-line layout assembled from vanilla terrain and PlaneShift gameplay
 * blocks. Every load resets only that course's own corridor, so retries are consistent and
 * courses 256 blocks apart cannot touch one another.
 */
public final class CourseStructureService {

    private static final String GENERATED_TAG = "planeshift.generated_course";
    private static final int LANE_HALF_WIDTH = 1;
    private static final int CLEAR_HALF_WIDTH = 3;
    private static final int CLEAR_BELOW = 4;
    private static final int CLEAR_ABOVE = 12;
    private static final int UPDATE_FLAGS = Block.UPDATE_CLIENTS
            | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;

    private CourseStructureService() {
    }

    public static void place(ServerLevel level, CourseDefinition course, String courseId) {
        if (course.structure().isPresent() && placeTemplate(level, course, course.structure().get())) {
            return;
        }
        placeGenerated(level, course, courseId);
    }

    private static boolean placeTemplate(ServerLevel level, CourseDefinition course,
                                         net.minecraft.resources.Identifier structureId) {
        StructureTemplateManager manager = level.getStructureManager();
        Optional<StructureTemplate> template = manager.get(structureId);
        if (template.isEmpty()) {
            PlaneShift.LOGGER.warn("Course structure not found: {}; using generated layout", structureId);
            return false;
        }

        StructurePlaceSettings settings = new StructurePlaceSettings().setIgnoreEntities(false);
        BlockPos anchor = course.startPos();
        if (!template.get().placeInWorld(level, anchor, anchor, settings, level.getRandom(), Block.UPDATE_ALL)) {
            PlaneShift.LOGGER.warn("Failed to place course structure {} at {}; using generated layout",
                    structureId, anchor);
            return false;
        }
        PlaneShift.LOGGER.info("Placed course structure {} at {}", structureId, anchor);
        return true;
    }

    /**
     * Builds the course.
     *
     * <p>Delegates to the segment composer in {@code server.gen}. The old implementation wrote
     * blocks into the world as it decided them, which meant a course could only be checked by
     * loading it and walking it — so nobody did, and courses shipped with pits nobody could cross
     * and platforms nobody could reach. Composition now produces a {@link CourseCanvas} first: an
     * ordinary data structure a test can flood-fill and prove traversable before a block is
     * placed. See {@code CourseReachability}.
     */
    private static void placeGenerated(ServerLevel level, CourseDefinition course, String courseId) {
        BlockPos start = course.startPos();
        int difficulty = CourseLayoutPlan.difficultyOf(courseId);
        long seed = CourseLayoutPlan.seedOf(courseId, level.getSeed());

        // A course declaring free_3d is built as a wide ribbon rather than a lane. Same segments,
        // same proof, more room — see GenContext.WIDE_HALF_WIDTH.
        int halfWidth = course.startMode() == com.studio.planeshift.common.mode.PlaneMode.FREE_3D
                ? com.studio.planeshift.server.gen.GenContext.WIDE_HALF_WIDTH
                : com.studio.planeshift.server.gen.GenContext.LANE_HALF_WIDTH;

        CourseComposer.Composition composition =
                CourseComposer.compose(course.theme(), course.length(), difficulty, seed, halfWidth);

        CourseWriter.write(level, start, composition.canvas(), course.length());

        PlaneShift.LOGGER.info("Generated {} course at {}: {} blocks long, difficulty {}, {} segments {}",
                course.theme().getSerializedName(), start, course.length(), difficulty,
                composition.segmentIds().size(), composition.segmentIds());
    }

    private static void clearCorridor(ServerLevel level, BlockPos start, int length) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = -4; x <= length + 6; x++) {
            for (int z = -CLEAR_HALF_WIDTH; z <= CLEAR_HALF_WIDTH; z++) {
                for (int y = -CLEAR_BELOW; y <= CLEAR_ABOVE; y++) {
                    cursor.set(start.getX() + x, start.getY() + y, start.getZ() + z);
                    level.setBlock(cursor, Blocks.AIR.defaultBlockState(), UPDATE_FLAGS);
                }
            }
        }
    }

    private static void clearGeneratedEntities(ServerLevel level, BlockPos start, int length) {
        AABB bounds = new AABB(
                start.getX() - 5.0D, start.getY() - CLEAR_BELOW, start.getZ() - CLEAR_HALF_WIDTH - 1.0D,
                start.getX() + length + 8.0D, start.getY() + CLEAR_ABOVE + 2.0D,
                start.getZ() + CLEAR_HALF_WIDTH + 2.0D);
        for (Entity entity : level.getEntities((Entity) null, bounds,
                entity -> entity.getTags().contains(GENERATED_TAG))) {
            entity.discard();
        }
    }

    private static void placeGroundSlice(ServerLevel level, BlockPos start, int floorY,
                                         int offset, Palette palette) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int depth = -LANE_HALF_WIDTH; depth <= LANE_HALF_WIDTH; depth++) {
            cursor.set(start.getX() + offset, floorY, start.getZ() + depth);
            level.setBlock(cursor, palette.surface(), UPDATE_FLAGS);
            for (int below = 1; below <= 3; below++) {
                cursor.setY(floorY - below);
                level.setBlock(cursor, palette.fill(), UPDATE_FLAGS);
            }
        }
    }

    private static void placePitAccent(ServerLevel level, BlockPos start, int floorY,
                                       int offset, CourseTheme theme) {
        if (theme != CourseTheme.LAVA) {
            return;
        }
        for (int depth = -LANE_HALF_WIDTH; depth <= LANE_HALF_WIDTH; depth++) {
            level.setBlock(new BlockPos(start.getX() + offset, floorY - 3, start.getZ() + depth),
                    Blocks.LAVA.defaultBlockState(), UPDATE_FLAGS);
        }
    }

    private static void buildStartLandmark(ServerLevel level, BlockPos start, Palette palette) {
        for (int y = 0; y <= 4; y++) {
            set(level, start.offset(-3, y, 0), palette.accent());
        }
        for (int x = -3; x <= 1; x++) {
            set(level, start.offset(x, 4, 0), palette.accent());
        }
        set(level, start.offset(4, 0, 0), ModBlocks.SPRING_PAD.get().defaultBlockState());
    }

    private static void buildRewardRun(ServerLevel level, BlockPos start) {
        for (int x = 16; x <= 22; x++) {
            BlockState state = x == 19
                    ? ModBlocks.QUESTION_BLOCK.get().defaultBlockState()
                    : ModBlocks.BRICK_BLOCK.get().defaultBlockState();
            set(level, start.offset(x, 3, 0), state);
        }
        set(level, start.offset(27, 3, 0), ModBlocks.COIN_BLOCK.get().defaultBlockState());
        set(level, start.offset(31, 3, 0), ModBlocks.HIDDEN_QUESTION_BLOCK.get().defaultBlockState());
    }

    /**
     * Ledges spread along the course, one per set piece.
     *
     * <p>The count comes from {@code setPieceCount}, which is derived from the course length, so a
     * 224-block course is genuinely denser rather than the same six platforms stretched out. The
     * heights cycle rather than being random: the player should be able to read a rhythm.
     */
    private static void buildPlatformSet(ServerLevel level, BlockPos start, CourseLayoutPlan plan,
                                         Palette palette) {
        int count = plan.setPieceCount();
        int usable = plan.length() - 30;
        int stride = Math.max(8, usable / count);

        for (int i = 0; i < count; i++) {
            int offset = 18 + stride * i;
            if (offset + 8 >= plan.length() - 8) {
                break;
            }
            int height = 3 + (i % 3);
            int width = 4 + (i % 3);
            platform(level, start, offset, height, width, palette.platform());

            // Every other ledge carries a prize row, so the platforms are worth climbing rather
            // than only worth clearing.
            if (i % 2 == 0) {
                set(level, start.offset(offset + 1, height + 4, 0),
                        ModBlocks.QUESTION_BLOCK.get().defaultBlockState());
                set(level, start.offset(offset + 2, height + 4, 0),
                        ModBlocks.BRICK_BLOCK.get().defaultBlockState());
            }
        }

        // One hidden block deliberately placed over a pit: reachable only by jumping out over
        // nothing, which is the whole joke.
        int[] pit = plan.gapAfter(plan.length() / 3);
        if (pit != null) {
            set(level, start.offset(pit[0] + 1, 4, 0),
                    ModBlocks.HIDDEN_QUESTION_BLOCK.get().defaultBlockState());
        }
    }

    private static void buildMechanicSet(ServerLevel level, BlockPos start, CourseLayoutPlan plan) {
        int mid = plan.midpoint();
        set(level, start.offset(mid, 1, 0), ModBlocks.CHECKPOINT_BEACON.get().defaultBlockState());

        set(level, start.offset(mid + 16, 1, 0), ModBlocks.ON_OFF_SWITCH.get().defaultBlockState());
        for (int x = mid + 20; x <= mid + 24; x++) {
            set(level, start.offset(x, 2, 0), ModBlocks.ON_OFF_BLOCK.get().defaultBlockState());
        }

        set(level, start.offset(mid + 32, 1, 0), ModBlocks.P_SWITCH.get().defaultBlockState());
        for (int x = mid + 35; x <= mid + 40; x++) {
            set(level, start.offset(x, 2, 0), ModBlocks.BRICK_BLOCK.get().defaultBlockState());
        }

        for (int x = plan.length() - 28; x <= plan.length() - 25; x++) {
            set(level, start.offset(x, 1, 0), ModBlocks.SPIKE_BLOCK.get().defaultBlockState());
        }
        set(level, start.offset(plan.length() - 21, 1, 0), ModBlocks.SPRING_PAD.get().defaultBlockState());
        set(level, start.offset(plan.length() - 17, 1, 0), ModBlocks.PRIZE_CACHE.get().defaultBlockState());
    }

        private static void buildStaircaseObstacle(ServerLevel level, BlockPos start, CourseLayoutPlan plan, Palette palette) {
        // Place a classic pyramid/staircase obstacle near the start
        int base = 25;
        if (plan.hasGroundAt(base) && plan.hasGroundAt(base + 8)) {
            for (int h = 0; h < 4; h++) {
                for (int x = base + h; x <= base + 8 - h; x++) {
                    set(level, start.offset(x, 1 + h, 0), palette.surface());
                }
            }
        }
    }

    private static void buildFinish(ServerLevel level, BlockPos start, CourseLayoutPlan plan,
                                    Palette palette) {
        int finish = plan.length();
        set(level, start.offset(finish, 1, 0), ModBlocks.FLAG_POLE.get().defaultBlockState()
                .setValue(FlagPoleBlock.PART, FlagPoleBlock.Part.BASE));
        for (int y = 2; y <= 6; y++) {
            set(level, start.offset(finish, y, 0), ModBlocks.FLAG_POLE.get().defaultBlockState()
                    .setValue(FlagPoleBlock.PART, FlagPoleBlock.Part.POLE));
        }
        set(level, start.offset(finish, 7, 0), ModBlocks.FLAG_POLE.get().defaultBlockState()
                .setValue(FlagPoleBlock.PART, FlagPoleBlock.Part.TOP));
        for (int step = 0; step < 5; step++) {
            for (int x = finish - 10 + step; x <= finish - 6 + step; x++) {
                set(level, start.offset(x, step + 1, 0), palette.accent());
            }
        }
        set(level, start.offset(finish + 4, 1, 0), ModBlocks.WARP_PIPE.get().defaultBlockState());
    }

    /**
     * A donut-block bridge over one of the pits, so a gap the player could jump becomes a gap
     * they must cross without stopping.
     *
     * <p>Placed over an existing planned gap rather than a new one: the layout already guarantees
     * the pit is survivable, so the bridge adds pressure without changing the course's shape.
     */
    private static void buildVerticalClimb(ServerLevel level, BlockPos start, CourseLayoutPlan plan) {
        int base = plan.midpoint() + 20;
        set(level, start.offset(base, 1, 0), ModBlocks.NOTE_BLOCK.get().defaultBlockState());
        spawnPlatform(level, start, base + 2, 6, MovingPlatformEntity.AXIS_Y, 4.0F);
        platform(level, start, base + 5, 10, 3, ModBlocks.COURSE_CLOUD_BLOCK.get().defaultBlockState());
        set(level, start.offset(base + 6, 11, 0), ModBlocks.NOTE_BLOCK.get().defaultBlockState());
        platform(level, start, base + 9, 16, 5, ModBlocks.COURSE_CLOUD_BLOCK.get().defaultBlockState());
    }

    private static void buildConveyorGauntlet(ServerLevel level, BlockPos start, CourseLayoutPlan plan) {
        int base = plan.midpoint() + 32; 
        BlockState forward = ModBlocks.CONVEYOR_BELT.get().defaultBlockState()
            .setValue(com.studio.planeshift.common.block.ConveyorBlock.FACING, net.minecraft.core.Direction.EAST);
        BlockState backward = ModBlocks.CONVEYOR_BELT.get().defaultBlockState()
            .setValue(com.studio.planeshift.common.block.ConveyorBlock.FACING, net.minecraft.core.Direction.WEST);
            
        for (int x = 0; x < 12; x++) {
            BlockState state = ((x / 3) % 2 == 0) ? backward : forward;
            set(level, start.offset(base + x, 1, 0), state);
        }
    }

    private static void buildDonutBridge(ServerLevel level, BlockPos start, CourseLayoutPlan plan) {
        BlockState donut = ModBlocks.DONUT_BLOCK.get().defaultBlockState();

        // The teaching version: a flat run across an early pit, so the block's behaviour is
        // learned somewhere a mistake costs one jump.
        if (plan.has(CourseLayout.Feature.DONUT_BRIDGE)) {
            int[] gap = plan.gapAfter(plan.midpoint() / 2);
            if (gap != null && gap[0] < plan.midpoint()) {
                for (int x = gap[0]; x <= gap[1]; x++) {
                    set(level, start.offset(x, 1, 0), donut);
                }
            }
        }

        // The exam: staggered heights over a later pit, gated behind the checkpoint so a failure
        // costs the run from the beacon rather than the whole course.
        if (plan.has(CourseLayout.Feature.DONUT_GAUNTLET)) {
            int[] gap = plan.gapAfter(plan.midpoint() + 1);
            if (gap != null) {
                for (int x = gap[0]; x <= gap[1]; x++) {
                    int y = x % 3 == 0 ? 3 : (x % 2 == 0 ? 2 : 1);
                    set(level, start.offset(x, y, 0), donut);
                }
            }
        }
    }

    /** A run of note blocks that bounce the player up to an otherwise unreachable ledge. */
    private static void buildNoteBlockRun(ServerLevel level, BlockPos start, CourseLayoutPlan plan) {
        int base = plan.midpoint() - 26;
        for (int i = 0; i < 3; i++) {
            set(level, start.offset(base + i * 3, 1, 0),
                    ModBlocks.NOTE_BLOCK.get().defaultBlockState());
        }
        // The reward for using them: a ledge only reachable from a note-block bounce.
        platform(level, start, base + 4, 6, 4, ModBlocks.COURSE_CLOUD_BLOCK.get().defaultBlockState());
    }

    /**
     * The hidden vine block. Tucked mid-course under open sky so the vine it grows has somewhere
     * to go, and so a player who finds it is rewarded with the coin heaven above.
     */
    private static void buildSecretVine(ServerLevel level, BlockPos start, CourseLayoutPlan plan) {
        set(level, start.offset(plan.midpoint() - 34, 5, 0),
                ModBlocks.SECRET_VINE.get().defaultBlockState());
    }

    /**
     * Coin Heaven: a cloud platform high above the course, lined with coins.
     *
     * <p>Sits directly above the secret vine so the climb leads somewhere, and high enough that
     * it cannot be reached by ordinary jumping — the vine has to be found first.
     */
    private static void buildCoinHeaven(ServerLevel level, BlockPos start, CourseLayoutPlan plan,
                                        Palette palette) {
        int anchor = plan.midpoint() - 34;
        int heavenY = 22;
        for (int x = anchor - 2; x <= anchor + 14; x++) {
            for (int depth = -LANE_HALF_WIDTH; depth <= LANE_HALF_WIDTH; depth++) {
                set(level, start.offset(x, heavenY, depth),
                        ModBlocks.COURSE_CLOUD_BLOCK.get().defaultBlockState());
            }
        }
        for (int x = anchor; x <= anchor + 12; x += 2) {
            spawnCoin(level, start.getX() + x, start.getY() + heavenY + 1.5D, start.getZ());
        }
    }

    /**
     * The castle finale: a bridge over a lava pit, a firebar to time, and the axe that drops the
     * bridge. Replaces the plain flag ending for the lava theme.
     */
    private static void buildCastleFinale(ServerLevel level, BlockPos start, CourseLayoutPlan plan,
                                          Palette palette) {
        int bridgeStart = plan.length() - 22;
        int bridgeEnd = plan.length() - 10;

        // Hollow the floor into a lava pit under the bridge.
        for (int x = bridgeStart; x <= bridgeEnd; x++) {
            for (int depth = -LANE_HALF_WIDTH; depth <= LANE_HALF_WIDTH; depth++) {
                set(level, start.offset(x, 0, depth), Blocks.LAVA.defaultBlockState());
                set(level, start.offset(x, -1, depth), Blocks.LAVA.defaultBlockState());
            }
            set(level, start.offset(x, 1, 0), ModBlocks.COURSE_CASTLE_BLOCK.get().defaultBlockState());
        }

        // The axe sits past the far end; taking it collapses the bridge back toward the pit.
        set(level, start.offset(bridgeEnd + 2, 2, 0), ModBlocks.AXE_BLOCK.get().defaultBlockState());

        // Castle walls, so the arena reads as an interior rather than an open pit.
        for (int y = 2; y <= 8; y++) {
            set(level, start.offset(bridgeStart - 2, y, 0), palette.accent());
            set(level, start.offset(bridgeEnd + 5, y, 0), palette.accent());
        }

        spawnFirebar(level, start, (bridgeStart + bridgeEnd) / 2, 6, 4, 1.0F);
        spawnFirebar(level, start, bridgeEnd - 2, 6, 3, -1.0F);
    }

    private static void buildGhostHouseLoop(ServerLevel level, BlockPos start, CourseLayoutPlan plan) {
        int loopX = plan.midpoint() + 15;
        // Invisible wall of trigger blocks
        for (int y = 1; y <= 5; y++) {
            for (int z = -LANE_HALF_WIDTH; z <= LANE_HALF_WIDTH; z++) {
                set(level, start.offset(loopX, y, z), ModBlocks.LOOP_TRIGGER.get().defaultBlockState());
            }
        }
        
        // A hidden path to climb over the loop trigger
        set(level, start.offset(loopX - 5, 2, 0), ModBlocks.HIDDEN_QUESTION_BLOCK.get().defaultBlockState());
        set(level, start.offset(loopX - 3, 4, 0), ModBlocks.HIDDEN_QUESTION_BLOCK.get().defaultBlockState());
        for (int x = loopX - 1; x <= loopX + 1; x++) {
            set(level, start.offset(x, 6, 0), ModBlocks.COURSE_CASTLE_BLOCK.get().defaultBlockState());
        }
    }

    /** Anchors a rotating firebar at a fixed point in the lane. */
    private static void spawnFirebar(ServerLevel level, BlockPos start, int offset, int height,
                                     int length, float spin) {
        FirebarEntity bar = ModEntities.FIREBAR.get().create(level, EntitySpawnReason.STRUCTURE);
        if (bar == null) {
            return;
        }
        bar.snapTo(start.getX() + offset + 0.5D, start.getY() + height,
                start.getZ() + 0.5D, 0.0F, 0.0F);
        bar.setBarLength(length);
        bar.setSpinDirection(spin);
        bar.addTag(GENERATED_TAG);
        level.addFreshEntity(bar);
    }

    /**
     * Moving platforms over two of the pits: one running along the lane, one rising and falling.
     *
     * <p>Two axes rather than one because they ask different things of the player — a horizontal
     * track is a timing problem, a vertical one is a patience problem.
     */
    private static void spawnMovingPlatforms(ServerLevel level, BlockPos start,
                                             CourseLayoutPlan plan) {
        // Indexed off the checkpoint rather than off fixed gap numbers: the pit count now varies
        // with course length, so gaps[4] is not guaranteed to exist.
        int[] late = plan.gapAfter(plan.length() - plan.length() / 3);
        if (late != null) {
            spawnPlatform(level, start, late[0], 3, MovingPlatformEntity.AXIS_X, 5.0F);
        }
        int[] middle = plan.gapAfter(plan.midpoint() - plan.length() / 6);
        if (middle != null && middle != late) {
            spawnPlatform(level, start, middle[0] + 2, 3, MovingPlatformEntity.AXIS_Y, 4.0F);
        }
    }

    private static void spawnPlatform(ServerLevel level, BlockPos start, int offset, int height,
                                      int axis, float range) {
        MovingPlatformEntity platform =
                ModEntities.MOVING_PLATFORM.get().create(level, EntitySpawnReason.STRUCTURE);
        if (platform == null) {
            return;
        }
        platform.snapTo(start.getX() + offset + 0.5D, start.getY() + height,
                start.getZ() + 0.5D, 0.0F, 0.0F);
        platform.setAxis(axis);
        platform.setRange(range);
        platform.addTag(GENERATED_TAG);
        level.addFreshEntity(platform);
    }

    private static void platform(ServerLevel level, BlockPos start, int offset, int height,
                                 int width, BlockState state) {
        for (int x = 0; x < width; x++) {
            for (int depth = -LANE_HALF_WIDTH; depth <= LANE_HALF_WIDTH; depth++) {
                set(level, start.offset(offset + x, height, depth), state);
            }
        }
    }

    private static void spawnRewards(ServerLevel level, BlockPos start, CourseLayoutPlan plan) {
        // Coin arches along the ground, skipped over pits so the coins are not floating in a hole
        // the player is trying to jump.
        for (int archStart = 10; archStart < plan.length() - 15; archStart += 25) {
            for (int dx = 0; dx < 5; dx++) {
                if (!plan.hasGroundAt(archStart + dx)) {
                    continue;
                }
                double archY = Math.sin(dx * Math.PI / 4.0) * 3.0 + 1.5;
                spawnCoin(level, start.getX() + archStart + dx, start.getY() + archY, start.getZ());
            }
        }

        // A coin line over each platform. Uses the same stride as buildPlatformSet so the coins
        // land on the ledges rather than beside them.
        int count = plan.setPieceCount();
        int stride = Math.max(8, (plan.length() - 30) / count);
        for (int i = 0; i < count; i++) {
            int offset = 18 + stride * i;
            if (offset + 8 >= plan.length() - 8) {
                break;
            }
            int height = 3 + (i % 3);
            for (int dx = 0; dx < 3; dx++) {
                spawnCoin(level, start.getX() + offset + dx, start.getY() + height + 1.5, start.getZ());
            }
            
            // Spawn 3 Star Coins at specific platforms
            if (i == 0 || i == count / 2 || i == count - 1) {
                spawnStarCoin(level, start.getX() + offset + 1, start.getY() + height + 3.0, start.getZ());
            }
        }
    }

    private static void spawnCoin(ServerLevel level, double x, double y, double z) {
        ItemEntity coin = new ItemEntity(level, x + 0.5, y, z + 0.5, new ItemStack(ModItems.COIN.get()));
        coin.setPickUpDelay(0);
        coin.setDeltaMovement(0.0, 0.0, 0.0);
        coin.addTag(GENERATED_TAG);
        level.addFreshEntity(coin);
    }

    private static void spawnStarCoin(ServerLevel level, double x, double y, double z) {
        ItemEntity starCoin = new ItemEntity(level, x + 0.5, y, z + 0.5, new ItemStack(ModItems.STAR_COIN.get()));
        starCoin.setPickUpDelay(0);
        starCoin.setDeltaMovement(0.0, 0.0, 0.0);
        starCoin.addTag(GENERATED_TAG);
        level.addFreshEntity(starCoin);
    }

    private static void spawnCast(ServerLevel level, BlockPos start, CourseLayoutPlan plan,
                                  CourseTheme theme) {
        List<EntityType<? extends Mob>> cast = castFor(theme);
        // Enemy count tracks the set-piece count for the same reason the platforms do: a longer
        // course should be a longer fight, not a longer walk.
        int count = plan.setPieceCount();
        int usable = plan.length() - 40;
        int stride = Math.max(10, usable / count);

        for (int i = 0; i < count; i++) {
            int offset = 15 + stride * i;
            if (offset >= plan.length() - 20) {
                break;
            }
            if (!plan.hasGroundAt(offset)) {
                // Spawning into a pit hands the player a free kill and costs the course an enemy.
                continue;
            }
            spawnMob(level, cast.get(i % cast.size()), start, offset, 1);
        }
        spawnMob(level, ModEntities.TOAD.get(), start, plan.length() - 13, 1);
    }

    private static List<EntityType<? extends Mob>> castFor(CourseTheme theme) {
        return switch (theme) {
            case GRASS -> List.of(ModEntities.GOOMBA.get(), ModEntities.KOOPA.get());
            case DESERT -> List.of(ModEntities.SPINY.get(), ModEntities.LAKITU.get());
            case SNOW -> List.of(ModEntities.GOOMBA.get(), ModEntities.BUZZY_BEETLE.get());
            case LAVA -> List.of(ModEntities.HAMMER_BRO.get(), ModEntities.THWOMP.get());
            case UNDERGROUND -> List.of(ModEntities.BOO.get(), ModEntities.BUZZY_BEETLE.get());
            case GHOST_HOUSE -> List.of(ModEntities.BOO.get());
        };
    }

    private static void spawnMob(ServerLevel level, EntityType<? extends Mob> type,
                                 BlockPos start, int offset, int height) {
        Mob mob = type.create(level, EntitySpawnReason.STRUCTURE);
        if (mob == null) {
            return;
        }
        mob.snapTo(start.getX() + offset + 0.5D, start.getY() + height,
                start.getZ() + 0.5D, 90.0F, 0.0F);
        mob.setPersistenceRequired();
        mob.addTag(GENERATED_TAG);
        level.addFreshEntity(mob);
    }

    private static void set(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, UPDATE_FLAGS);
    }

    private record Palette(BlockState surface, BlockState fill, BlockState accent,
                           BlockState platform) {
        static Palette forTheme(CourseTheme theme) {
            return switch (theme) {
                case GRASS -> new Palette(ModBlocks.COURSE_GRASS_BLOCK.get().defaultBlockState(),
                        Blocks.DIRT.defaultBlockState(), ModBlocks.BRICK_BLOCK.get().defaultBlockState(),
                        ModBlocks.COURSE_CLOUD_BLOCK.get().defaultBlockState());
                case DESERT -> new Palette(ModBlocks.COURSE_SAND_BLOCK.get().defaultBlockState(),
                        Blocks.SANDSTONE.defaultBlockState(), Blocks.ORANGE_TERRACOTTA.defaultBlockState(),
                        ModBlocks.COURSE_SAND_BLOCK.get().defaultBlockState());
                case SNOW -> new Palette(ModBlocks.COURSE_SNOW_BLOCK.get().defaultBlockState(),
                        Blocks.PACKED_ICE.defaultBlockState(), Blocks.LIGHT_BLUE_CONCRETE.defaultBlockState(),
                        ModBlocks.COURSE_SNOW_BLOCK.get().defaultBlockState());
                case LAVA -> new Palette(ModBlocks.COURSE_CASTLE_BLOCK.get().defaultBlockState(),
                        Blocks.BLACKSTONE.defaultBlockState(), ModBlocks.COURSE_EMBER_BLOCK.get().defaultBlockState(),
                        ModBlocks.COURSE_CASTLE_BLOCK.get().defaultBlockState());
                case UNDERGROUND -> new Palette(ModBlocks.COURSE_CASTLE_BLOCK.get().defaultBlockState(),
                        Blocks.DEEPSLATE.defaultBlockState(), Blocks.PURPLE_TERRACOTTA.defaultBlockState(),
                        ModBlocks.COURSE_CASTLE_BLOCK.get().defaultBlockState());
                case GHOST_HOUSE -> new Palette(ModBlocks.COURSE_CASTLE_BLOCK.get().defaultBlockState(),
                        Blocks.DARK_OAK_PLANKS.defaultBlockState(), Blocks.DARK_OAK_LOG.defaultBlockState(),
                        ModBlocks.COURSE_CASTLE_BLOCK.get().defaultBlockState());
            };
        }
    }
}



