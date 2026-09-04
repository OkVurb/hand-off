package com.studio.planeshift.server;

import com.studio.planeshift.common.entity.CourseEnemyEntity;
import com.studio.planeshift.common.registry.ModBlocks;
import com.studio.planeshift.common.registry.ModSounds;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Makes the world dangerous to enemies, not just to the player.
 *
 * <p>Until now {@code KoopaEntity.tickSlide} was the only place in the entire mod where anything
 * other than the player could defeat an enemy. A Goomba could walk into a Muncher pit and stand in
 * it; a Bullet Bill could fly straight through a line of them and part it like a curtain. Every
 * hazard in {@code SegmentLibrary} existed solely as a thing that hurts <em>you</em>.
 *
 * <p>That is a real loss, because it is the difference between a room of obstacles and a system.
 * Once lava kills enemies too, a Muncher pit stops being a wall and becomes something the player
 * can knock a Koopa into — and the level designer gets that for free everywhere the two are
 * already placed next to each other, which in a generated course is constantly.
 */
public final class EnemyHazardService {

    /**
     * How often an enemy checks what it is standing in.
     *
     * <p>Every four ticks rather than every tick. A hazard the player can see is lethal within a
     * fifth of a second either way, and this runs on every enemy in a 720-block course.
     */
    private static final int CHECK_INTERVAL = 4;

    /**
     * Blocks that kill an enemy standing in or on them.
     *
     * <p>The same set {@code CourseReachability.HAZARD} refuses to route the player across, plus
     * lava, which is absent from that set only because the solver never treats a pit floor as
     * standable anyway. Keeping the two aligned is the whole discipline here: a block that kills
     * enemies but that the solver happily walks the player over teaches exactly the wrong lesson.
     *
     * <p>{@code COURSE_MAGMA_BLOCK} is deliberately <em>not</em> here despite the name. It is the
     * LAVA theme accent - a decorative block registered through {@code courseBlock} with no damage
     * behaviour of any kind - so it does not hurt the player, and a block that kills enemies but
     * not the player is the same misalignment in the other direction.
     */
    private static final Set<Block> LETHAL = Set.of(
            ModBlocks.MUNCHER.get(),
            ModBlocks.SPIKE_BLOCK.get(),
            Blocks.LAVA,
            Blocks.MAGMA_BLOCK);

    private EnemyHazardService() {
    }

    /** Called every tick from {@link CourseEnemyEntity#tick()}; cheap on the ticks it skips. */
    public static void tick(CourseEnemyEntity enemy) {
        if (!(enemy.level() instanceof ServerLevel level)) {
            return;
        }
        if (enemy.tickCount % CHECK_INTERVAL != 0 || !enemy.isAlive()) {
            return;
        }
        // Fresh from a drop and still falling: it has not arrived anywhere yet, and killing it
        // mid-air on the way past a hazard would look arbitrary.
        if (enemy.fallingFromDrop()) {
            return;
        }

        BlockPos feet = enemy.blockPosition();
        if (isLethal(level.getBlockState(feet)) || isLethal(level.getBlockState(feet.below()))) {
            consume(level, enemy);
        }
    }

    private static boolean isLethal(BlockState state) {
        return LETHAL.contains(state.getBlock());
    }

    /** Kills the enemy with the same feedback a defeat gets, so the player reads it as one. */
    private static void consume(ServerLevel level, CourseEnemyEntity enemy) {
        level.sendParticles(ParticleTypes.CLOUD,
                enemy.getX(), enemy.getY(0.5D), enemy.getZ(), 10, 0.25D, 0.2D, 0.25D, 0.02D);
        level.playSound(null, enemy.blockPosition(), ModSounds.ENEMY_DEFEAT.get(),
                SoundSource.HOSTILE, 0.8F, 1.3F);
        enemy.discard();
    }

    /**
     * One enemy running through others — a Bullet Bill parting a Goomba line.
     *
     * <p>No score is attributed. The player did not do this, and paying them for it would make
     * standing still next to a blaster a strategy.
     *
     * @return how many were defeated
     */
    public static int plough(ServerLevel level, CourseEnemyEntity source, float damage) {
        int hits = 0;
        AABB box = source.getBoundingBox().inflate(0.2D);
        for (CourseEnemyEntity other : level.getEntitiesOfClass(CourseEnemyEntity.class, box,
                e -> e != source && e.isAlive() && !e.fallingFromDrop())) {
            other.invulnerableTime = 0;
            other.hurtServer(level, source.damageSources().mobAttack(source), damage);
            if (!other.isAlive()) {
                level.playSound(null, other.blockPosition(), ModSounds.ENEMY_DEFEAT.get(),
                        SoundSource.HOSTILE, 0.9F, 1.1F);
                hits++;
            }
        }
        return hits;
    }
}
