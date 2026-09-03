package com.studio.planeshift.server.gen;

import com.studio.planeshift.PlaneShift;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/**
 * Flushes a composed {@link CourseCanvas} into the world.
 *
 * <p>Kept separate from composition on purpose. Everything upstream of this class is pure data and
 * can be generated, inspected and proven correct in a unit test; this is the only part that needs
 * a running server. When a course plays badly, that split is what lets the question "is the level
 * wrong, or is the placement wrong?" be answered instead of guessed at.
 */
public final class CourseWriter {

    private static final int UPDATE_FLAGS = Block.UPDATE_CLIENTS
            | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;

    /** How far past the course bounds to clear, so a previous layout cannot show through. */
    private static final int CLEAR_MARGIN = 6;
    private static final int CLEAR_BELOW = 8;
    private static final int CLEAR_ABOVE = 26;
    private static final int CLEAR_HALF_WIDTH = 3;

    private CourseWriter() {
    }

    /**
     * Clears the corridor, then writes every block, entity and item.
     *
     * <p>Clearing first is what makes a retry deterministic. Two courses generated from different
     * seeds occupy the same region, and anything left behind from the previous one is a block the
     * player can stand on that the reachability proof never saw.
     */
    public static void write(ServerLevel level, BlockPos origin, CourseCanvas canvas, int length) {
        clear(level, origin, canvas, length);

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        canvas.blocks().forEach((key, state) -> {
            cursor.set(origin.getX() + CourseCanvas.unpackX(key),
                    origin.getY() + CourseCanvas.unpackY(key),
                    origin.getZ() + CourseCanvas.unpackZ(key));
            level.setBlock(cursor, state, UPDATE_FLAGS);
        });

        for (CourseCanvas.EntitySpawn spawn : canvas.entities()) {
            Entity entity = spawn.type().create(level, EntitySpawnReason.STRUCTURE);
            if (entity == null) {
                continue;
            }
            entity.snapTo(origin.getX() + spawn.x(), origin.getY() + spawn.y(),
                    origin.getZ() + spawn.z(), spawn.facing(), 0.0F);
            if (entity instanceof Mob mob) {
                mob.setPersistenceRequired();
            }
            if (spawn.tag() != null) {
                entity.addTag(spawn.tag());
            }
            level.addFreshEntity(entity);
        }

        for (CourseCanvas.ItemDrop drop : canvas.items()) {
            ItemEntity item = new ItemEntity(level,
                    origin.getX() + drop.x(), origin.getY() + drop.y(), origin.getZ() + drop.z(),
                    new ItemStack(drop.item()));
            item.setPickUpDelay(0);
            item.setDeltaMovement(0.0D, 0.0D, 0.0D);
            item.addTag(SegmentLibrary.GENERATED_TAG);
            level.addFreshEntity(item);
        }

        PlaneShift.LOGGER.info("Wrote course at {}: {} blocks, {} entities, {} items",
                origin, canvas.blockCount(), canvas.entities().size(), canvas.items().size());
    }

    private static void clear(ServerLevel level, BlockPos origin, CourseCanvas canvas, int length) {
        int maxX = Math.max(length, canvas.maxX()) + CLEAR_MARGIN;
        int top = Math.max(CLEAR_ABOVE, canvas.maxY() + 4);
        int bottom = Math.min(-CLEAR_BELOW, canvas.minY() - 4);

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = -CLEAR_MARGIN; x <= maxX; x++) {
            for (int z = -CLEAR_HALF_WIDTH; z <= CLEAR_HALF_WIDTH; z++) {
                for (int y = bottom; y <= top; y++) {
                    cursor.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
                    level.setBlock(cursor, Blocks.AIR.defaultBlockState(), UPDATE_FLAGS);
                }
            }
        }

        AABB bounds = new AABB(
                origin.getX() - CLEAR_MARGIN, origin.getY() + bottom,
                origin.getZ() - CLEAR_HALF_WIDTH - 1.0D,
                origin.getX() + maxX, origin.getY() + top,
                origin.getZ() + CLEAR_HALF_WIDTH + 2.0D);
        for (Entity entity : level.getEntities((Entity) null, bounds,
                e -> e.getTags().contains(SegmentLibrary.GENERATED_TAG))) {
            entity.discard();
        }
    }
}
