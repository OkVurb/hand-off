package com.studio.planeshift.common.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Bowser placeholder boss. Big, fire-breathing, not stompable.
 */
public class BowserEntity extends CourseEnemyEntity {

    public BowserEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 60.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.22D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.ARMOR, 4.0D)
                .add(Attributes.FOLLOW_RANGE, 48.0D);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new BowserGoal(this));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }


    @Override
    public void tick() {
        super.tick();

        if (this.isAlive() && !this.level().isClientSide()) {
            if (this.isInLava() || this.getY() < -64.0D) {
                this.hurtServer((net.minecraft.server.level.ServerLevel) this.level(), this.damageSources().fellOutOfWorld(), Float.MAX_VALUE);
            }
        }

        if (this.fallDistance > 1.5F) {
            this.setYRot(this.getYRot() + 25.0F);
            this.yBodyRot = this.getYRot();
            this.yHeadRot = this.getYRot();
        }
    }

    /** A boss must not be lockable by a move the player can repeat at will. */
    @Override
    public boolean canBeStaggered() {
        return false;
    }

    /**
     * A pound hurts Bowser but does not delete him. The default is four times max health, which
     * exists so armour cannot let an ordinary enemy survive one; a boss needs a real health bar.
     */
    @Override
    protected float groundPoundDamage() {
        return 20.0F;
    }


    /**
     * A boss: too big to stomp, and not to be deleted by a thrown fireball either. The ground
     * pound is the required finisher, which is why it is in this set and STOMP is not.
     */
    @Override
    public java.util.Set<DefeatVector> answers() {
        return java.util.EnumSet.of(DefeatVector.GROUND_POUND, DefeatVector.SHELL,
                DefeatVector.FIRE, DefeatVector.STAR);
    }

}
