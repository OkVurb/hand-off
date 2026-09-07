package com.studio.planeshift.common.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A ball of fuzz that will not stop coming and cannot be stomped.
 *
 * <p>It shares a brief with {@link UrchinEntity} — touch it and you are hurt, nothing you have
 * works on it — and differs in the one way that matters: it travels. An urchin closes a gap the
 * player is trying to swim through; a Fuzzy walks the corridor toward them, which turns the same
 * "you cannot fight this" into a reason to keep moving rather than a reason to time a jump.
 *
 * <p>It has eyes for exactly that reason, where the urchin deliberately has none. Eyes say the
 * thing is going somewhere. The player needs to read a Fuzzy as an animal that will arrive and an
 * urchin as terrain that is in the way, and at this camera distance a face is the whole difference.
 */
public class FuzzyEntity extends CourseEnemyEntity {

    /** How far it wanders from where it was placed before turning back. */
    private static final double RANGE = 7.0D;

    /** Blocks per tick. Slower than a walk, so outrunning it is always possible and never free. */
    private static final double SPEED = 0.075D;

    private double originX = Double.NaN;
    private double direction = 1.0D;

    public FuzzyEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 10.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D);
    }

    @Override
    protected void registerGoals() {
        // None. It runs a line, and that is all it has ever done.
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() || !isAlive()) {
            return;
        }
        if (Double.isNaN(originX)) {
            originX = getX();
        }
        if (Math.abs(getX() - originX) > RANGE) {
            direction = getX() > originX ? -1.0D : 1.0D;
        }
        // A small bob, so a line of them does not read as beads on a wire.
        double bob = Math.sin(tickCount / 18.0D) * 0.018D;
        setDeltaMovement(SPEED * direction, bob, 0.0D);
        move(net.minecraft.world.entity.MoverType.SELF, getDeltaMovement());
    }

    /** Stomping it is the mistake it exists to punish. */
    @Override
    public java.util.Set<DefeatVector> answers() {
        return java.util.EnumSet.of(DefeatVector.STAR);
    }

    @Override
    public boolean canBeStaggered() {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        if (!Double.isNaN(originX)) {
            output.putDouble("OriginX", originX);
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        originX = input.getDoubleOr("OriginX", Double.NaN);
    }
}
