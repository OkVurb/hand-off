package com.studio.planeshift.server;

import com.studio.planeshift.PlaneShift;
import com.studio.planeshift.common.course.CourseDefinition;
import com.studio.planeshift.common.course.CourseTheme;
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
        buildFinish(level, start, plan, palette);
        spawnRewards(level, start, plan);
        spawnCast(level, start, plan, course.theme());

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
        platform(level, start, 34, 2, 7, palette.platform());
        platform(level, start, 48, 4, 6, palette.platform());
        platform(level, start, plan.midpoint() - 8, 2, 8, palette.platform());
        platform(level, start, plan.length() - 42, 3, 7, palette.platform());
    }

    private static void buildMechanicSet(ServerLevel level, BlockPos start, CourseLayoutPlan plan) {
        int mid = plan.midpoint();
        set(level, start.offset(mid, 0, 0), ModBlocks.CHECKPOINT_BEACON.get().defaultBlockState());

        set(level, start.offset(mid + 12, 0, 0), ModBlocks.ON_OFF_SWITCH.get().defaultBlockState());
        for (int x = mid + 16; x <= mid + 20; x++) {
            set(level, start.offset(x, 1, 0), ModBlocks.ON_OFF_BLOCK.get().defaultBlockState());
        }

        set(level, start.offset(mid + 28, 0, 0), ModBlocks.P_SWITCH.get().defaultBlockState());
        for (int x = mid + 31; x <= mid + 36; x++) {
            set(level, start.offset(x, 1, 0), ModBlocks.BRICK_BLOCK.get().defaultBlockState());
        }

        for (int x = plan.length() - 28; x <= plan.length() - 25; x++) {
            set(level, start.offset(x, 0, 0), ModBlocks.SPIKE_BLOCK.get().defaultBlockState());
        }
        set(level, start.offset(plan.length() - 21, 0, 0), ModBlocks.SPRING_PAD.get().defaultBlockState());
        set(level, start.offset(plan.length() - 17, 0, 0), ModBlocks.PRIZE_CACHE.get().defaultBlockState());
    }

    private static void buildFinish(ServerLevel level, BlockPos start, CourseLayoutPlan plan,
                                    Palette palette) {
        int finish = plan.length();
        for (int y = 0; y <= 6; y++) {
            set(level, start.offset(finish, y, 0), ModBlocks.FLAG_POLE.get().defaultBlockState());
        }
        for (int step = 0; step < 5; step++) {
            for (int x = finish - 10 + step; x <= finish - 6 + step; x++) {
                set(level, start.offset(x, step, 0), palette.accent());
            }
        }
        set(level, start.offset(finish + 4, 0, 0), ModBlocks.WARP_PIPE.get().defaultBlockState());
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
        for (int x = 9; x < plan.length() - 12; x += 8) {
            double arc = Math.sin(x * 0.22D) * 1.25D;
            ItemEntity coin = new ItemEntity(level,
                    start.getX() + x + 0.5D, start.getY() + 1.2D + Math.max(0.0D, arc),
                    start.getZ() + 0.5D, new ItemStack(ModItems.COIN.get()));
            coin.setPickUpDelay(0);
            coin.setDeltaMovement(0.0D, 0.0D, 0.0D);
            coin.addTag(GENERATED_TAG);
            level.addFreshEntity(coin);
        }
    }

    private static void spawnCast(ServerLevel level, BlockPos start, CourseLayoutPlan plan,
                                  CourseTheme theme) {
        List<EntityType<? extends Mob>> cast = castFor(theme);
        int[] offsets = {30, 54, plan.midpoint() + 20, plan.length() - 35};
        for (int i = 0; i < offsets.length; i++) {
            spawnMob(level, cast.get(i % cast.size()), start, offsets[i], 0);
        }
        spawnMob(level, ModEntities.TOAD.get(), start, plan.length() - 13, 0);
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
