package com.studio.planeshift.common.block;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;

/**
 * Palette-aware cuboid search for course blocks.
 *
 * <p>The naive form of this — {@code BlockPos.betweenClosed(min, max)} plus a
 * {@link Level#getBlockState} per position — costs one chunk lookup, one section lookup and one
 * palette read for every block in the box. A P-switch box (49x25x49) is ~60k such lookups on a
 * single activation, which is enough to show up in the server tick.
 *
 * <p>This walks loaded chunk sections instead. A section is 4096 blocks, and
 * {@link LevelChunkSection#maybeHas} answers "could any state in this section match?" from the
 * section palette alone. Sections made only of stone and air — which is most of them — are
 * rejected by that one call, so the per-block loop only runs where a match is actually possible.
 * {@code maybeHas} is conservative (a global-palette section always answers yes), so it can cost a
 * wasted section scan but can never miss a block.
 *
 * <p>Positions are collected and returned rather than passed to a callback: callers mutate the
 * blocks they find, and writing into a section while iterating its palette is exactly the kind of
 * aliasing this class exists to avoid.
 */
public final class BlockAreaScan {

    private BlockAreaScan() {
    }

    /**
     * Finds every block within {@code radiusXZ} horizontally and {@code radiusY} vertically of
     * {@code center} whose state matches {@code filter}.
     *
     * <p>Unloaded chunks are skipped, matching the {@code level.isLoaded} guard this replaced.
     * Chunks are never generated to satisfy the search.
     *
     * @return immutable positions, in ascending chunk then section order
     */
    public static List<BlockPos> findMatching(Level level, BlockPos center, int radiusXZ, int radiusY,
                                              Predicate<BlockState> filter) {
        int minX = center.getX() - radiusXZ;
        int maxX = center.getX() + radiusXZ;
        int minZ = center.getZ() - radiusXZ;
        int maxZ = center.getZ() + radiusXZ;
        int minY = Math.max(center.getY() - radiusY, level.getMinY());
        int maxY = Math.min(center.getY() + radiusY, level.getMaxY());

        List<BlockPos> found = new ArrayList<>();
        if (minY > maxY) {
            return found;
        }

        int minSection = level.getSectionIndex(minY);
        int maxSection = level.getSectionIndex(maxY);

        for (int chunkX = SectionPos.blockToSectionCoord(minX); chunkX <= SectionPos.blockToSectionCoord(maxX); chunkX++) {
            for (int chunkZ = SectionPos.blockToSectionCoord(minZ); chunkZ <= SectionPos.blockToSectionCoord(maxZ); chunkZ++) {
                // requireChunk=false: returns null instead of generating terrain off the course.
                ChunkAccess chunk = level.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
                if (chunk == null) {
                    continue;
                }
                int chunkMinX = Math.max(minX, SectionPos.sectionToBlockCoord(chunkX));
                int chunkMaxX = Math.min(maxX, SectionPos.sectionToBlockCoord(chunkX) + 15);
                int chunkMinZ = Math.max(minZ, SectionPos.sectionToBlockCoord(chunkZ));
                int chunkMaxZ = Math.min(maxZ, SectionPos.sectionToBlockCoord(chunkZ) + 15);

                for (int index = minSection; index <= maxSection; index++) {
                    LevelChunkSection section = chunk.getSection(index);
                    if (section.hasOnlyAir() || !section.maybeHas(filter)) {
                        continue;
                    }
                    int sectionBaseY = SectionPos.sectionToBlockCoord(level.getSectionYFromSectionIndex(index));
                    int sectionMinY = Math.max(minY, sectionBaseY);
                    int sectionMaxY = Math.min(maxY, sectionBaseY + 15);

                    for (int y = sectionMinY; y <= sectionMaxY; y++) {
                        for (int z = chunkMinZ; z <= chunkMaxZ; z++) {
                            for (int x = chunkMinX; x <= chunkMaxX; x++) {
                                BlockState state = section.getBlockState(x & 15, y & 15, z & 15);
                                if (filter.test(state)) {
                                    found.add(new BlockPos(x, y, z));
                                }
                            }
                        }
                    }
                }
            }
        }
        return found;
    }
}
