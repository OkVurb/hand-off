package com.studio.planeshift.server;

import com.studio.planeshift.PlaneShift;
import com.studio.planeshift.common.course.CourseDefinition;
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

    public static void place(ServerLevel level, CourseDefinition course) {
        if (course.structure().isPresent() && placeTemplate(level, course, course.structure().get())) {
            return;
        }
        placeGenerated(level, course);
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

    private static void placeGenerated(ServerLevel level, CourseDefinition course) {
        BlockPos start = course.startPos();
        CourseLayoutPlan plan = CourseLayoutPlan.forTheme(course.theme(), course.length());
        int floorY = start.getY() - 1;

        clearCorridor(level, start, plan.length());
        clearGeneratedEntities(level, start, plan.length());

        Palette palette = Palette.forTheme(course.theme());
        for (int offset = -4; offset <= plan.length() + 6; offset++) {
            if (!plan.hasGroundAt(offset)) {
                placePitAccent(level, start, floorY, offset, course.theme());
                continue;
            }
            placeGroundSlice(level, start, floorY, offset, palette);
        }

        buildStartLandmark(level, start, palette);
        buildRewardRun(level, start);
        buildPlatformSet(level, start, plan, palette);
        buildMechanicSet(level, start, plan);
        buildDonutBridge(level, start, plan);
        buildNoteBlockRun(level, start, plan);
        buildSecretVine(level, start, plan);
        buildCoinHeaven(level, start, plan, palette);
        buildVerticalClimb(level, start, plan);
        buildConveyorGauntlet(level, start, plan);
        buildFinish(level, start, plan, palette);
        spawnRewards(level, start, plan);
        spawnCast(level, start, plan, course.theme());
        spawnMovingPlatforms(level, start, plan);
        if (course.theme() == CourseTheme.LAVA) {
            // Only the lava theme gets a castle finale; a castle in a meadow reads as a mistake.
            buildCastleFinale(level, start, plan, palette);
        }

        PlaneShift.LOGGER.info("Generated {} course at {} ({} blocks long, floor y={}, kill y={})",
                course.theme().getSerializedName(), start, plan.length(), floorY, course.killY());
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

    private static void buildPlatformSet(ServerLevel level, BlockPos start, CourseLayoutPlan plan,
                                         Palette palette) {
        int mid = plan.midpoint();
        // Early platforms
        platform(level, start, 20, 3, 5, palette.platform());
        platform(level, start, 32, 5, 4, palette.platform());
        
        // Mid platforms
        platform(level, start, mid - 12, 4, 6, palette.platform());
        platform(level, start, mid + 10, 4, 5, palette.platform());
        
        // Late platforms
        platform(level, start, plan.length() - 40, 5, 8, palette.platform());
        platform(level, start, plan.length() - 25, 3, 6, palette.platform());
        
        // Add powerups / blocks above these platforms
        for (int x : new int[]{22, 34, mid - 10, mid + 12, plan.length() - 38}) {
            // Find platform height
            int y = (x == 32 || x == 34) ? 5 : (x == plan.length() - 38 ? 5 : ((x == mid - 10 || x == mid + 12) ? 4 : 3));
            BlockState qb = ModBlocks.QUESTION_BLOCK.get().defaultBlockState();
            set(level, start.offset(x, y + 4, 0), qb);
            set(level, start.offset(x + 1, y + 4, 0), ModBlocks.BRICK_BLOCK.get().defaultBlockState());
        }
        
        // A tricky hidden block over a gap
        set(level, start.offset(47, 4, 0), ModBlocks.HIDDEN_QUESTION_BLOCK.get().defaultBlockState());
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

    private static void buildFinish(ServerLevel level, BlockPos start, CourseLayoutPlan plan,
                                    Palette palette) {
        int finish = plan.length();
        for (int y = 1; y <= 7; y++) {
            set(level, start.offset(finish, y, 0), ModBlocks.FLAG_POLE.get().defaultBlockState());
        }
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
        int[][] gaps = plan.gaps();
        if (gaps.length > 1) {
            int[] gap = gaps[1];
            for (int x = gap[0]; x <= gap[1]; x++) {
                set(level, start.offset(x, 1, 0), ModBlocks.DONUT_BLOCK.get().defaultBlockState());
            }
        }
        if (gaps.length > 3) {
            int[] gap2 = gaps[3];
            for (int x = gap2[0]; x <= gap2[1]; x++) {
                set(level, start.offset(x, (x % 2 == 0) ? 2 : 1, 0), ModBlocks.DONUT_BLOCK.get().defaultBlockState());
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
        int[][] gaps = plan.gaps();
        if (gaps.length >= 5) {
            spawnPlatform(level, start, gaps[4][0], 3, MovingPlatformEntity.AXIS_X, 5.0F);
        }
        if (gaps.length >= 3) {
            spawnPlatform(level, start, gaps[2][0] + 2, 3, MovingPlatformEntity.AXIS_Y, 4.0F);
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
        // Generate coin arches
        for (int archStart = 10; archStart < plan.length() - 15; archStart += 25) {
            for (int dx = 0; dx < 5; dx++) {
                double archY = Math.sin(dx * Math.PI / 4.0) * 3.0 + 1.5;
                spawnCoin(level, start.getX() + archStart + dx, start.getY() + archY, start.getZ());
            }
        }
        
        // Generate straight coin lines on some platforms
        for (int pStart : new int[]{32, plan.midpoint() - 12, plan.length() - 40}) {
            int pHeight = (pStart == 32) ? 5 : (pStart == plan.midpoint() - 12 ? 4 : 5);
            for (int dx = 0; dx < 3; dx++) {
                spawnCoin(level, start.getX() + pStart + dx + 1, start.getY() + pHeight + 1.5, start.getZ());
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

    private static void spawnCast(ServerLevel level, BlockPos start, CourseLayoutPlan plan,
                                  CourseTheme theme) {
        List<EntityType<? extends Mob>> cast = castFor(theme);
        int[] offsets = {15, 28, 42, plan.midpoint() + 5, plan.midpoint() + 28, plan.length() - 35};
        for (int i = 0; i < offsets.length; i++) {
            spawnMob(level, cast.get(i % cast.size()), start, offsets[i], 1);
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
                        Blocks.BLACKSTONE.defaultBlockState(), ModBlocks.COURSE_MAGMA_BLOCK.get().defaultBlockState(),
                        ModBlocks.COURSE_CASTLE_BLOCK.get().defaultBlockState());
                case UNDERGROUND -> new Palette(ModBlocks.COURSE_CASTLE_BLOCK.get().defaultBlockState(),
                        Blocks.DEEPSLATE.defaultBlockState(), Blocks.PURPLE_TERRACOTTA.defaultBlockState(),
                        ModBlocks.COURSE_CASTLE_BLOCK.get().defaultBlockState());
            };
        }
    }
}

