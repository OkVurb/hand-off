package com.studio.planeshift.common.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Rock thrown out of the erupting background, landing in the lane.
 *
 * <p>The sheets show volcanoes erupting behind the playfield and dropping debris <em>into</em> it,
 * which is the only case in the whole reference where the background is not just scenery. Every
 * other animated thing back there stays back there.
 *
 * <p>That creates a problem the other hazards do not have. A firebar, a saw and a chain ball are
 * all visible for their whole cycle, so drawing their path is a courtesy; a rock arriving from
 * off-screen is invisible until it is already falling, and no amount of drawing its path helps
 * because the path starts somewhere the player cannot see. Left there it would be the one unfair
 * hazard in the game.
 *
 * <p>So the telegraph moves to the other end: the landing spot is marked on the ground before the
 * rock is released. That is the fifth form of the same rule -- reach as a circle, path as a line,
 * swing as an arc, and here the destination, because the destination is the only part of this
 * hazard the player can act on.
 *
 * <p>Fixed cycle, not reactive, for exactly the reason {@link PodobooEntity} gives: the value of
 * the hazard is that it is a metronome the player reads and then walks through. One that aimed at
 * the player would punish being there, and being there is what the level is asking for.
 */
public class VolcanicBombEntity extends Entity {

    /** Ticks spent marking the ground before the rock is released. */
    private static final int WARN_TICKS = 34;

    /** Ticks the rock spends falling, after which the cycle restarts. */
    private static final int FALL_TICKS = 46;

    private static final int CYCLE = WARN_TICKS + FALL_TICKS;

    /** How far above its anchor the rock starts. High enough to be off-screen at our camera. */
    private static final double DROP_HEIGHT = 14.0D;

    private static final double FALL_SPEED = DROP_HEIGHT / FALL_TICKS;

    private static final double HIT_RADIUS = 0.8D;
    private static final float CONTACT_DAMAGE = 4.0F;

    private double anchorY = Double.NaN;
    private int tickOffset;

    public VolcanicBombEntity(EntityType<? extends VolcanicBombEntity> type, Level level) {
        super(type, level);
        this.tickOffset = this.random.nextInt(CYCLE);
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder b) {
        // Nothing synced: the cycle is a pure function of tickCount, so both sides compute the
        // same position from the same clock without anything crossing the wire.
    }

    /** Where in the cycle this rock is, 0 to {@link #CYCLE}. */
    private int phase() {
        return Math.floorMod(tickCount + tickOffset, CYCLE);
    }

    private boolean falling() {
        return phase() >= WARN_TICKS;
    }

    /** Height above the anchor at the current phase. Zero once it has landed. */
    private double heightAboveAnchor() {
        if (!falling()) {
            return DROP_HEIGHT;
        }
        int fallen = phase() - WARN_TICKS;
        return Math.max(0.0D, DROP_HEIGHT - fallen * FALL_SPEED);
    }

    @Override
    public void tick() {
        super.tick();

        if (Double.isNaN(anchorY)) {
            anchorY = getY();
        }

        double y = anchorY + heightAboveAnchor();
        setPos(getX(), y, getZ());

        if (level().isClientSide()) {
            return;
        }

        if (falling()) {
            hurtTouching();
        }
    }

    private void hurtTouching() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        AABB box = new AABB(getX() - HIT_RADIUS, getY() - HIT_RADIUS, getZ() - HIT_RADIUS,
                getX() + HIT_RADIUS, getY() + HIT_RADIUS, getZ() + HIT_RADIUS);
        for (ServerPlayer player : serverLevel.getEntitiesOfClass(ServerPlayer.class, box,
                p -> p.isAlive() && !p.isCreative() && !p.isSpectator())) {
            player.hurtServer(serverLevel, damageSources().generic(), CONTACT_DAMAGE);
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        anchorY = input.getDoubleOr("AnchorY", Double.NaN);
        tickOffset = input.getIntOr("TickOffset", this.random.nextInt(CYCLE));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putDouble("AnchorY", anchorY);
        output.putInt("TickOffset", tickOffset);
    }

    @Override
    public boolean hurtServer(ServerLevel level,
                              net.minecraft.world.damagesource.DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    /** Exposed for tests: the warning always finishes before the rock starts moving. */
    static int warnTicks() {
        return WARN_TICKS;
    }

    static int cycleTicks() {
        return CYCLE;
    }
}
