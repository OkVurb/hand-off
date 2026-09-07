package com.studio.planeshift.common.entity;

import com.studio.planeshift.common.registry.ModEffects;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The Koopalings' clown car, which does not fight.
 *
 * <p>Checked against the wiki rather than reconstructed from a contact sheet, because the last
 * three plan entries written from frames were wrong about how the thing was built. What the wiki
 * actually describes: in the final castle the Koopalings ride one clown car and try to turn the
 * player to stone by flashing its eyes, and the player evades the flash by passing between the
 * castle's pillars. It is not a boss fight -- the boss of that castle is Bowser, twice -- it is a
 * hazard hanging over the walk in, and the pillars are the answer to it.
 *
 * <p>That reading is what makes this cheap to build honestly. There is no health, no phases and no
 * defeat: the car cannot be beaten, only got past, which is a different verb from everything else
 * in the mod and is the whole reason the set-piece is worth having.
 *
 * <h2>Cover is line of sight, and nothing else</h2>
 *
 * <p>The flash is resolved with one raycast from the car's eyes to the player's. A pillar between
 * them blocks it; nothing else in the arena can, because nothing else in the arena is solid at head
 * height. So the rule the player learns -- get a column between you and it -- is literally the rule
 * the code runs, rather than a proximity radius that approximates it. A radius would have let a
 * player standing in the open next to a pillar be safe, which is exactly the kind of near-miss that
 * teaches the wrong lesson.
 */
public class ClownCarEntity extends Entity {

    /**
     * Whether the eyes are lit, synced so the client can draw the wind-up.
     *
     * <p>The flash needs to be readable <em>before</em> it lands or the only way to learn it is to
     * be turned to stone by it. This is that telegraph, and it is the eyes themselves lighting up
     * rather than an overlay, because in this genre the warning is always part of the object.
     */
    private static final EntityDataAccessor<Boolean> CHARGING =
            SynchedEntityData.defineId(ClownCarEntity.class, EntityDataSerializers.BOOLEAN);

    /** Ticks between flashes. */
    private static final int FLASH_INTERVAL = 90;

    /** How long the eyes glow before the flash fires. */
    private static final int CHARGE_TICKS = 25;

    /** How long the player stays stone. Long enough to hurt, short enough not to be a death. */
    private static final int STONE_TICKS = 70;

    /** How far the car will look for someone to menace. */
    private static final double RANGE = 26.0D;

    /** How fast it drifts after the player. Slower than a walk: it herds rather than chases. */
    private static final double DRIFT = 0.055D;

    /** How high above the floor it hangs. */
    private static final double HOVER_Y = 8.0D;

    private int clock;

    public ClownCarEntity(EntityType<? extends ClownCarEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(CHARGING, false);
    }

    /** Whether the eyes are lit. The renderer asks; nothing else should. */
    public boolean charging() {
        return entityData.get(CHARGING);
    }

    @Override
    public void tick() {
        super.tick();
        // Bob, on both sides, so the client sees it move even between the flashes.
        setPos(getX(), getY() + Math.sin(tickCount * 0.06D) * 0.012D, getZ());
        if (level().isClientSide()) {
            return;
        }

        Player target = level().getNearestPlayer(this, RANGE);
        if (target == null) {
            // No audience, no performance. The clock is reset rather than paused so a player who
            // walks back into range gets the full wind-up instead of an instant flash.
            clock = 0;
            entityData.set(CHARGING, false);
            return;
        }

        drift(target);

        clock++;
        boolean charging = clock >= FLASH_INTERVAL - CHARGE_TICKS;
        entityData.set(CHARGING, charging);
        if (clock >= FLASH_INTERVAL) {
            clock = 0;
            entityData.set(CHARGING, false);
            flash(target);
        }
    }

    /** Follows the player horizontally and holds its height. */
    private void drift(Player target) {
        double dx = target.getX() - getX();
        double toward = Math.signum(dx) * Math.min(DRIFT, Math.abs(dx));
        double floor = target.getY() + HOVER_Y - getY();
        setPos(getX() + toward, getY() + Math.signum(floor) * Math.min(0.05D, Math.abs(floor)),
                getZ());
    }

    /** Turns the player to stone unless something solid is in the way. */
    private void flash(Player target) {
        if (!sees(target)) {
            return;
        }
        target.addEffect(new MobEffectInstance(ModEffects.STONE, STONE_TICKS, 0, false, true, true));
    }

    /** True when nothing blocks the line from the car's eyes to the player's. */
    private boolean sees(Player target) {
        Vec3 from = new Vec3(getX(), getY(), getZ());
        Vec3 to = target.getEyePosition();
        BlockHitResult hit = level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, this));
        return hit.getType() == HitResult.Type.MISS;
    }

    /** Nothing here is worth saving: the car is placed by the arena and belongs to it. */
    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        clock = input.getIntOr("Clock", 0);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("Clock", clock);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    /**
     * Nothing hurts it.
     *
     * <p>Not an oversight and not a difficulty choice. The car has no defeat condition in the
     * reference either -- it is scenery with an attack, and the player's answer is the pillars.
     * Letting a fireball drive it off would replace "get past this" with "shoot this", which is
     * the verb the rest of the game already has.
     */
    @Override
    public boolean hurtServer(net.minecraft.server.level.ServerLevel level,
                              net.minecraft.world.damagesource.DamageSource source, float amount) {
        return false;
    }
}
