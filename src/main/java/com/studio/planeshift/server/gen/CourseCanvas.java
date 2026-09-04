package com.studio.planeshift.server.gen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Everything a course is made of, held in memory before any of it touches the world.
 *
 * <p>This is the single most important structural decision in the generator, so it is worth
 * stating why. The old generator wrote blocks straight into a {@code ServerLevel} as it decided
 * them. That meant the only way to find out whether a course was playable was to load it and walk
 * it — so nobody did, and courses shipped with pits nobody could cross and platforms nobody could
 * reach. Building into a buffer instead makes the finished course an ordinary data structure:
 * something a test can flood-fill, measure, and prove reachable before a single block is placed.
 *
 * <p>Coordinates are course-local: x runs along the course from 0 at the spawn, y is height with 0
 * at the spawn floor, z is depth across the lane with 0 at the centre. The service that flushes
 * the canvas is the only thing that knows where the course actually sits in the world, which also
 * means segments can be composed and re-ordered without any of them knowing their absolute
 * position.
 */
public final class CourseCanvas {

    /** A block the generator wants placed, keyed by packed local coordinates. */
    private final Map<Long, BlockState> blocks = new LinkedHashMap<>();

    /** Mobs to spawn, in insertion order so a course generates identically every time. */
    private final List<EntitySpawn> entities = new ArrayList<>();

    /** Item pickups — coins, star coins, power-ups placed loose in the world. */
    private final List<ItemDrop> items = new ArrayList<>();

    /**
     * Work deferred until the canvas has been written into a real level.
     *
     * <p>The canvas is a pure write buffer precisely so a course can be validated before anything
     * touches the world; anything needing a live {@code ServerLevel} — running a command, talking
     * to another mod — cannot run during composition. This is the escape hatch, and it is private
     * with an accessor like every other collection here, rather than a public mutable field.
     */
    private final List<java.util.function.BiConsumer<net.minecraft.server.level.ServerLevel, BlockPos>>
            postBuildTasks = new ArrayList<>();

    public void addPostBuildTask(
            java.util.function.BiConsumer<net.minecraft.server.level.ServerLevel, BlockPos> task) {
        postBuildTasks.add(task);
    }

    public List<java.util.function.BiConsumer<net.minecraft.server.level.ServerLevel, BlockPos>> postBuildTasks() {
        return postBuildTasks;
    }

    /** Named points other systems need: the checkpoint, the flag, sub-room links. */
    private final Map<String, BlockPos> markers = new HashMap<>();

    private int maxX = 0;
    private int minY = 0;
    private int maxY = 0;

    /**
     * A mob placement.
     *
     * @param type    what to spawn
     * @param x       local x, in blocks from the spawn
     * @param y       local y, relative to the spawn floor
     * @param z       local z across the lane
     * @param facing  yaw in degrees
     * @param tag     an optional tag applied to the entity, for cleanup on course reload
     */
    public record EntitySpawn(EntityType<?> type, double x, double y, double z, float facing,
                              String tag) {
    }

    /** An item pickup placed in the world. */
    public record ItemDrop(Item item, double x, double y, double z) {
    }

    /**
     * Ground that exists only because something moves through it — a lift, a moving platform.
     *
     * <p>Declared by the segment that creates the platform, because the reachability solver reads
     * blocks and a platform is an entity. Without this a segment whose only crossing is a moving
     * platform looks like an impassable pit, and the validator rejects a course that plays fine.
     *
     * <p>Declaring it is deliberately the segment's job rather than something inferred from the
     * spawn list. A segment knows the platform's axis, range and timing; the canvas does not, and
     * guessing would produce a proof that quietly stops matching the level.
     */
    public record MovingSurface(int fromX, int toX, int y) {
    }

    private final List<MovingSurface> movingSurfaces = new ArrayList<>();

    /** Declares that a platform sweeps along {@code y} between two x positions, inclusive. */
    public void movingSurface(int fromX, int toX, int y) {
        movingSurfaces.add(new MovingSurface(Math.min(fromX, toX), Math.max(fromX, toX), y));
    }

    public List<MovingSurface> movingSurfaces() {
        return movingSurfaces;
    }

    /**
     * Packs local coordinates into one long.
     *
     * <p>Ranges are deliberately generous on x (courses can be 224 long and segments overrun) and
     * tight on z (the lane is three blocks wide). Offsets keep negative coordinates positive so
     * the packing stays monotonic and debuggable.
     */
    static long key(int x, int y, int z) {
        return ((long) (x + 512) << 24) | ((long) (y + 256) << 8) | (z + 128);
    }

    static int unpackX(long k) {
        return (int) (k >> 24) - 512;
    }

    static int unpackY(long k) {
        return (int) ((k >> 8) & 0xFFFF) - 256;
    }

    static int unpackZ(long k) {
        return (int) (k & 0xFF) - 128;
    }

    /** Places a block. Later writes win, so a segment may deliberately overwrite terrain. */
    public void set(int x, int y, int z, BlockState state) {
        blocks.put(key(x, y, z), state);
        if (x > maxX) {
            maxX = x;
        }
        if (y > maxY) {
            maxY = y;
        }
        if (y < minY) {
            minY = y;
        }
    }

    /** Places a block only where nothing has been placed yet — for decoration behind set pieces. */
    public void setIfEmpty(int x, int y, int z, BlockState state) {
        if (!blocks.containsKey(key(x, y, z))) {
            set(x, y, z, state);
        }
    }

    /** Fills the full lane width at one column and height. */
    public void setLane(int x, int y, BlockState state, int halfWidth) {
        for (int z = -halfWidth; z <= halfWidth; z++) {
            set(x, y, z, state);
        }
    }

    /** Fills an inclusive box in local space. */
    public void fill(int x0, int y0, int z0, int x1, int y1, int z1, BlockState state) {
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) {
            for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++) {
                for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) {
                    set(x, y, z, state);
                }
            }
        }
    }

    public BlockState get(int x, int y, int z) {
        return blocks.get(key(x, y, z));
    }

    public boolean isEmpty(int x, int y, int z) {
        return !blocks.containsKey(key(x, y, z));
    }

    public void spawn(EntityType<?> type, double x, double y, double z, float facing, String tag) {
        entities.add(new EntitySpawn(type, x, y, z, facing, tag));
    }

    public void item(Item item, double x, double y, double z) {
        items.add(new ItemDrop(item, x, y, z));
    }

    public void marker(String name, int x, int y, int z) {
        markers.put(name, new BlockPos(x, y, z));
    }

    public Map<Long, BlockState> blocks() {
        return blocks;
    }

    public List<EntitySpawn> entities() {
        return entities;
    }

    public List<ItemDrop> items() {
        return items;
    }

    public Map<String, BlockPos> markers() {
        return markers;
    }

    public int maxX() {
        return maxX;
    }

    public int minY() {
        return minY;
    }

    public int maxY() {
        return maxY;
    }

    public int blockCount() {
        return blocks.size();
    }
}
