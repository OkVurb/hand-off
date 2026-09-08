package com.studio.planeshift.common.entity;

import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

/**
 * A jet of fire that fires on a clock: up out of the lava, or sideways out of a wall.
 *
 * <p>The last of the plan's lava findings. Both shapes are one entity because they are one idea —
 * a fixed place that is safe most of the time and lethal on a rhythm — and the only thing that
 * differs is which way the column points. Splitting them into two entities would have meant two
 * clocks to keep in step and two chances to disagree about how long lethal lasts.
 *
 * <p>It is a timing hazard rather than a projectile, and that is the point of building it as a
 * column rather than as something thrown. A fireball has to be dodged wherever it happens to be; a
 * jet always occupies the same space, so the player can learn it, walk up to it, and wait — which
 * is a decision, where dodging is a reaction.
 *
 * <h2>The tell has to precede the fire</h2>
 *
 * <p>{@link #WARM_TICKS} is the whole design. The column grows from nothing over a fixed wind-up
 * before it does damage, so the player who is looking gets a warning and the player who is not
 * gets hit by something they could have seen. Without it this is a trap, and the project's own
 * rule is that a hazard the player cannot read is a death they cannot learn from.
 */
public class LavaJetEntity extends Entity {

    /** How far the column reaches at full extension, in blocks. */
    private static final EntityDataAccessor<Float> REACH =
            SynchedEntityData.defineId(LavaJetEntity.class, EntityDataSerializers.FLOAT);

    /** 0 fires up, 1 fires east, 2 fires west. Synced, because the renderer points the mesh. */
    private static final EntityDataAccessor<Integer> FACING =
            SynchedEntityData.defineId(LavaJetEntity.class, EntityDataSerializers.INT);

    /** How far out the column currently is, 0 to 1. Synced so the client draws the wind-up. */
    private static final EntityDataAccessor<Float> EXTENSION =
            SynchedEntityData.defineId(LavaJetEntity.class, EntityDataSerializers.FLOAT);

    private static final float DEFAULT_REACH = 4.0F;

    /** Ticks the column spends growing before it can hurt anything. */
    private static final int WARM_TICKS = 18;

    /** Ticks it stays out, at full reach and lethal. */
    private static final int HOT_TICKS = 26;

    /** Ticks it spends withdrawn. Long enough that waiting is a real option. */
    private static final int COLD_TICKS = 46;

    private static final int CYCLE = WARM_TICKS + HOT_TICKS + COLD_TICKS;

    private static final float CONTACT_DAMAGE = 5.0F;

    /** How thick the column is for damage purposes. Under a block, so a near miss is a near miss. */
    private static final double RADIUS = 0.45D;

    private int clock;

    public LavaJetEntity(EntityType<? extends LavaJetEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.clock = this.random.nextInt(CYCLE);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(REACH, DEFAULT_REACH);
        builder.define(FACING, 0);
        builder.define(EXTENSION, 0.0F);
    }

    public float reach() {
        return entityData.get(REACH);
    }

    public void setReach(float reach) {
        entityData.set(REACH, reach);
    }

    /** Which way the column points. */
    public Direction direction() {
        return switch (entityData.get(FACING)) {
            case 1 -> Direction.EAST;
            case 2 -> Direction.WEST;
            default -> Direction.UP;
        };
    }

    public void setDirection(Direction direction) {
        entityData.set(FACING, switch (direction) {
            case EAST -> 1;
            case WEST -> 2;
            default -> 0;
        });
    }

    /** How far out the column is, 0 to 1. The renderer asks; nothing else should. */
    public float extension() {
        return entityData.get(EXTENSION);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            return;
        }
        clock = (clock + 1) % CYCLE;

        float extension;
        boolean lethal;
        if (clock < WARM_TICKS) {
            extension = clock / (float) WARM_TICKS;
            lethal = false;
        } else if (clock < WARM_TICKS + HOT_TICKS) {
            extension = 1.0F;
            lethal = true;
        } else {
            // Withdrawing. Still drawn, still harmless: the retreat is as readable as the rise,
            // and a column that vanished on the frame it stopped hurting would teach the player to
            // treat "visible" and "lethal" as the same thing, which they are not during the rise.
            int out = clock - WARM_TICKS - HOT_TICKS;
            extension = Math.max(0.0F, 1.0F - out / (float) (COLD_TICKS / 2));
            lethal = false;
        }
        entityData.set(EXTENSION, extension);

        if (lethal) {
            burn();
        }
    }

    /** Hurts whatever is standing in the column. */
    private void burn() {
        double reach = reach();
        Direction facing = direction();
        AABB column = switch (facing) {
            case EAST -> new AABB(getX(), getY() - RADIUS, getZ() - RADIUS,
                    getX() + reach, getY() + RADIUS, getZ() + RADIUS);
            case WEST -> new AABB(getX() - reach, getY() - RADIUS, getZ() - RADIUS,
                    getX(), getY() + RADIUS, getZ() + RADIUS);
            default -> new AABB(getX() - RADIUS, getY(), getZ() - RADIUS,
                    getX() + RADIUS, getY() + reach, getZ() + RADIUS);
        };
        if (!(level() instanceof ServerLevel server)) {
            return;
        }
        for (Entity entity : level().getEntities(this, column, e -> e instanceof ServerPlayer)) {
            entity.hurtServer(server, damageSources().inFire(), CONTACT_DAMAGE);
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        clock = input.getIntOr("Clock", 0);
        entityData.set(REACH, input.getFloatOr("Reach", DEFAULT_REACH));
        entityData.set(FACING, input.getIntOr("Facing", 0));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("Clock", clock);
        output.putFloat("Reach", entityData.get(REACH));
        output.putInt("Facing", entityData.get(FACING));
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    /** Fixed plumbing. Nothing puts it out. */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }
}
