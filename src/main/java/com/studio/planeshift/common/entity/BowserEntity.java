package com.studio.planeshift.common.entity;

import com.studio.planeshift.common.registry.ModSounds;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * The boss at the end of every world.
 *
 * <p>Was described here as a placeholder for as long as nothing spawned him. {@code BossArena}
 * now puts him on a bridge over lava at the end of all five worlds, so this is the real thing and
 * the comment should stop apologising for it.
 *
 * <p>The classic defeat is not in this class and does not need to be. The arena drops the bridge
 * out from under him when the player takes the axe, and the lava check in {@link #tick} does the
 * rest -- so the oldest boss mechanic in the genre falls out of two pieces that were each written
 * for their own reasons.
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
        // Position first, attacks second. BackgroundBossGoal owns only where he stands -- behind
        // the play plane, reaching in -- and leaves BowserGoal's fire and charge untouched, so the
        // staging changed without the fight being rewritten.
        goalSelector.addGoal(1, new BackgroundBossGoal(this));
        goalSelector.addGoal(2, new BowserGoal(this));
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

    /**
     * The roar.
     *
     * <p>ModSounds.BOWSER_ROAR was the one registered sound in the mod that nothing ever played --
     * recorded, packaged, and silent, because until the arena existed there was nothing to roar
     * at. Bowser had no sound hooks of any kind and neither does the base enemy class, so he fought
     * the player in complete silence.
     */
    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() {
        return ModSounds.BOWSER_ROAR.get();
    }

    /**
     * How often the idle roar comes round.
     *
     * <p>Vanilla picks roughly every four seconds, which for a single loud roar in a sealed stone
     * room is not atmosphere, it is a metronome. This is the interval that decides whether the
     * fight sounds tense or ridiculous, so it is set here rather than inherited.
     */
    @Override
    public int getAmbientSoundInterval() {
        return 160;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(
            net.minecraft.world.damagesource.DamageSource source) {
        return ModSounds.BOWSER_ROAR.get();
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() {
        return ModSounds.BOWSER_ROAR.get();
    }

    /**
     * Pitched down, so the same recording reads as three different things.
     *
     * <p>There is one roar and three occasions for it. Playing it identically each time would make
     * the death sound indistinguishable from an idle grumble, which is the moment in the whole
     * game that most needs to land.
     */
    @Override
    public float getVoicePitch() {
        return isDeadOrDying() ? 0.65F : (hurtTime > 0 ? 0.8F : 1.0F);
    }

}
