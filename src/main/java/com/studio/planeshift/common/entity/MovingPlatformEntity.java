package com.studio.planeshift.common.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidType;

/**
 * A solid moving platform that travels back and forth along its X or Z axis.
 * Entities standing on top are carried with the platform.
 */
public class MovingPlatformEntity extends Mob {

    private static final EntityDataAccessor<Integer> DATA_AXIS =
            SynchedEntityData.defineId(MovingPlatformEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_RANGE =
            SynchedEntityData.defineId(MovingPlatformEntity.class, EntityDataSerializers.FLOAT);

    /** NaN means "not yet initialized from NBT or the world"; see {@link #onAddedToLevel()}. */
    private double startX = Double.NaN;
    private double startZ = Double.NaN;
    private int tickOffset;

    public MovingPlatformEntity(EntityType<? extends MovingPlatformEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.setNoAi(true);
        this.noPhysics = true;
        this.tickOffset = this.random.nextInt(200);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_AXIS, 0);
        builder.define(DATA_RANGE, 4.0F);
    }

    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        if (Double.isNaN(this.startX) || Double.isNaN(this.startZ)) {
            this.startX = getX();
            this.startZ = getZ();
        }
    }

    @Override
    public void tick() {
        if (this.level().isClientSide()) {
            super.tick();
            return;
        }
        if (Double.isNaN(this.startX) || Double.isNaN(this.startZ)) {
            this.startX = getX();
            this.startZ = getZ();
        }

        float range = getRange();
        double speed = 0.04D;
        double wave = Math.sin((tickCount + tickOffset) * speed) * range;

        double nx = startX + (getAxis() == 0 ? wave : 0.0D);
        double nz = startZ + (getAxis() == 1 ? wave : 0.0D);
        double dx = nx - getX();
        double dz = nz - getZ();

        AABB carryBox = getBoundingBox().inflate(0.2D, 0.1D, 0.2D);
        for (Entity rider : this.level().getEntities(this, carryBox)) {
            if (rider.getBoundingBox().minY >= getBoundingBox().maxY - 0.15D) {
                Vec3 pos = rider.position();
                rider.setPos(pos.x + dx, pos.y, pos.z + dz);
                rider.setOnGround(true);
                rider.fallDistance = 0.0F;
            }
        }

        setPos(nx, getY(), nz);
        super.tick();
    }

    @Override
    public boolean canBeCollidedWith(Entity entity) {
        return this.isAlive();
    }

    @Override
    public boolean isPushedByFluid(FluidType type) {
        return false;
    }

    @Override
    public boolean canDrownInFluidType(FluidType type) {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
    }

    @Override
    public void push(Entity entity) {
    }

    public int getAxis() {
        return this.entityData.get(DATA_AXIS);
    }

    public void setAxis(int axis) {
        this.entityData.set(DATA_AXIS, Mth.clamp(axis, 0, 1));
    }

    public float getRange() {
        return this.entityData.get(DATA_RANGE);
    }

    public void setRange(float range) {
        this.entityData.set(DATA_RANGE, Math.max(0.5F, range));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Axis", getAxis());
        output.putFloat("Range", getRange());
        output.putDouble("StartX", startX);
        output.putDouble("StartZ", startZ);
        output.putInt("TickOffset", tickOffset);
        output.putBoolean("NoPhysics", noPhysics);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setAxis(input.getIntOr("Axis", 0));
        setRange(input.getFloatOr("Range", 4.0F));
        this.startX = input.getDoubleOr("StartX", Double.NaN);
        this.startZ = input.getDoubleOr("StartZ", Double.NaN);
        this.tickOffset = input.getIntOr("TickOffset", this.random.nextInt(200));
        this.noPhysics = input.getBooleanOr("NoPhysics", true);
    }
}
