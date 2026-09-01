package com.studio.planeshift.common.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Buzzy Beetle - hard-shelled and stompable.
 */
public class BuzzyBeetleEntity extends CourseEnemyEntity {

    public BuzzyBeetleEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 10.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.16D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D)
                .add(Attributes.ARMOR, 4.0D);
    }

    /**
     * Immune to fire. The shell is the Buzzy Beetle's whole identity: it is the answer to a
     * player who has been solving every problem with the Fire Flower, and it has to be stomped
     * or avoided instead.
     */
    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        // fireImmune covers vanilla fire types; this also turns away the mod's own fireball,
        // which arrives as an indirect projectile hit rather than a fire damage type.
        if (source.getDirectEntity() instanceof FireballProjectile
                || source.getDirectEntity() instanceof EmberBoltEntity
                || source.is(DamageTypes.IN_FIRE)
                || source.is(DamageTypes.ON_FIRE)) {
            return false;
        }
        return super.hurtServer(level, source, amount);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0D, false));
        goalSelector.addGoal(2, new LanePatrolGoal(this, 1.0D));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }
}

