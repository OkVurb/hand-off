package com.studio.planeshift.server.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.LivingEntity;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.world.BossEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.EnumSet;

public class BowserEntity extends Monster {
    
    private final ServerBossEvent bossEvent = new ServerBossEvent(Component.literal("Bowser"), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);
    private boolean isPhaseTwo = false;

    public BowserEntity(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 300.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 10.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new BowserAttackGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        
        // Phase transition logic
        if (!this.level().isClientSide()) {
            this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
            
            if (!isPhaseTwo && this.getHealth() < this.getMaxHealth() * 0.5f) {
                this.isPhaseTwo = true;
                this.playSound(SoundEvents.ENDER_DRAGON_GROWL, 2.0F, 1.0F);
            }
        }
    }

    public boolean isPhaseTwo() {
        return this.isPhaseTwo;
    }
    
    // AI Goal for Bowser's attacks
    static class BowserAttackGoal extends Goal {
        private final BowserEntity bowser;
        private int attackCooldown = 0;

        public BowserAttackGoal(BowserEntity bowser) {
            this.bowser = bowser;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return this.bowser.getTarget() != null && this.bowser.getTarget().isAlive();
        }

        @Override
        public void tick() {
            LivingEntity target = this.bowser.getTarget();
            if (target == null) return;

            this.bowser.getLookControl().setLookAt(target, 30.0F, 30.0F);

            if (attackCooldown > 0) {
                attackCooldown--;
                return;
            }

            // Determine which attack to use
            int attackState = this.bowser.getRandom().nextInt(3);

            if (attackState == 0) {
                shootFireball(target);
                attackCooldown = this.bowser.isPhaseTwo() ? 40 : 60;
            } else if (attackState == 1) {
                performJump();
                attackCooldown = this.bowser.isPhaseTwo() ? 60 : 100;
            } else if (attackState == 2) {
                throwHammer(target);
                attackCooldown = this.bowser.isPhaseTwo() ? 20 : 30;
            }
        }

        private void shootFireball(LivingEntity target) {
            Level level = this.bowser.level();
            if (!level.isClientSide()) {
                double d0 = target.getX() - this.bowser.getX();
                double d1 = target.getY(0.5D) - this.bowser.getY(0.5D);
                double d2 = target.getZ() - this.bowser.getZ();
                
                // Assuming 1.20 mappings, if fireball constructor differs it might need EntityType.LARGE_FIREBALL
                // fireball omitted for now
                this.bowser.playSound(SoundEvents.GHAST_SHOOT, 1.0F, 1.0F);
            }
        }

        private void performJump() {
            if (this.bowser.onGround()) {
                Vec3 movement = this.bowser.getDeltaMovement();
                this.bowser.setDeltaMovement(movement.x, 1.0D, movement.z);
                this.bowser.playSound(SoundEvents.ENDER_DRAGON_FLAP, 1.0F, 1.0F);
            }
        }

        private void throwHammer(LivingEntity target) {
            // Placeholder for hammer throw
            // In a real mod, we'd spawn a custom HammerProjectileEntity
            this.bowser.playSound(SoundEvents.SNOW_GOLEM_SHOOT, 1.0F, 1.0F);
        }
    }
}







