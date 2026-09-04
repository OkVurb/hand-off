package com.studio.planeshift.server;

import com.studio.planeshift.common.entity.CourseEnemyEntity;
import com.studio.planeshift.common.registry.ModParticles;
import com.studio.planeshift.common.registry.ModSounds;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The shockwave a ground pound sends out when it lands.
 *
 * <p>Before this existed the ground pound touched nothing but the block underneath it: it opened
 * question blocks and did not so much as inconvenience an enemy standing next to one. That left
 * armoured enemies with exactly one counter in the entire mod — the spin jump — and left the pound
 * as a block-opening tool rather than a combat verb, which is not what it is anywhere in the genre
 * it is borrowed from.
 *
 * <p>Split from {@link GroundPoundResolver} on purpose: everything here needs a live server, and
 * everything <em>there</em> is arithmetic. The decision about who is hit is the part that goes
 * wrong, so it lives where a unit test can reach it.
 */
public final class GroundPoundImpactService {

    private GroundPoundImpactService() {
    }

    /**
     * Applies the wave and returns how many enemies it affected.
     *
     * <p>One bounded query, sized from the resolver's own radii so the box and the rules cannot
     * drift apart. The vertical extent is deliberately shallow — a pound sweeps the floor it landed
     * on, not the platform above it.
     *
     * @param source the player who landed the pound, or null if something else caused it
     */
    public static int shockwave(ServerLevel level, Vec3 origin, @Nullable ServerPlayer source) {
        AABB box = AABB.ofSize(origin,
                GroundPoundResolver.STAGGER_RADIUS * 2.0D,
                GroundPoundResolver.VERTICAL_REACH * 2.0D,
                GroundPoundResolver.STAGGER_RADIUS * 2.0D);

        int affected = 0;
        for (CourseEnemyEntity enemy : level.getEntitiesOfClass(CourseEnemyEntity.class, box,
                CourseEnemyEntity::isAlive)) {
            // A dropped enemy still falling is exempt for the same reason it cannot be touched at
            // all yet: the exchange is postponed until the player can see it coming.
            boolean immune = !enemy.canBeStaggered() || enemy.fallingFromDrop();
            GroundPoundResolver.Response response = GroundPoundResolver.classify(
                    enemy.getX() - origin.x, enemy.getY() - origin.y, enemy.getZ() - origin.z,
                    enemy.canBeFlipped(), enemy.onGround(), immune);

            switch (response) {
                case FLIP -> {
                    enemy.flipOntoBack(GroundPoundResolver.FLIP_TICKS);
                    affected++;
                }
                case STAGGER -> {
                    enemy.stagger(GroundPoundResolver.STAGGER_TICKS);
                    affected++;
                }
                case NONE -> {
                    // Out of range, airborne or exempt.
                }
            }
        }

        if (affected > 0) {
            // Only announce a wave that did something. A ring of particles every time the player
            // pounds an empty floor teaches them the move is working when it is not.
            level.sendParticles(ModParticles.HIT_BURST.get(),
                    origin.x, origin.y + 0.1D, origin.z, 14, 1.2D, 0.05D, 1.2D, 0.02D);
            level.playSound(null, origin.x, origin.y, origin.z, ModSounds.STOMP.get(),
                    SoundSource.PLAYERS, 1.0F, 0.6F);
        }
        return affected;
    }
}
