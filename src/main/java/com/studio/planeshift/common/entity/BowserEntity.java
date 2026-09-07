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

    /**
     * Whether going down puts him back up again, larger.
     *
     * <p>Off by default and switched on by {@code BossArena} for the last world only. Every castle
     * has a Bowser in it; only the last one has this, because a fight that ends twice is a climax
     * the first time and a chore the other four.
     */
    private boolean revives;

    public BowserEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    /**
     * Whether this Bowser stands behind the play plane.
     *
     * <p>False here, and that is a correction rather than a default. He was given
     * {@link BackgroundBossGoal} on the strength of the reference's last boss filling the screen
     * from the backdrop -- but the wiki is specific that the last castle is fought twice, and it is
     * the <em>second</em> Bowser who is enormous. The first is fought in a corridor, which is what
     * the arena builds and what the axe is for.
     *
     * <p>It was also actively broken. The goal eases the boss five blocks behind the plane, the
     * arena only lays floor across the three-block lane, and nothing is built out there -- so the
     * first Bowser walked off the back of his own arena and {@code tick()} killed him for leaving
     * the world. The fight the whole progression points at was ending before the player reached
     * it. This project's characteristic bug, found once more: finished, tested, unreachable.
     *
     * @see SuperBowserEntity
     */
    protected boolean fightsFromTheBackdrop() {
        return false;
    }

    /** Marks this Bowser as the one the Koopalings will put back together. */
    public void setRevives(boolean revives) {
        this.revives = revives;
    }

    /**
     * The phase change, at the only moment it can be missed from.
     *
     * <p>Hooked on death rather than on a health threshold so it fires however he goes down --
     * out-damaged on the bank, or dropped in the lava when the bridge falls. A threshold would have
     * been skippable by the bridge, which is the ending the arena is actually built around.
     */
    @Override
    public void die(net.minecraft.world.damagesource.DamageSource cause) {
        boolean again = revives && !level().isClientSide();
        super.die(cause);
        if (again && level() instanceof net.minecraft.server.level.ServerLevel server) {
            SuperBowserEntity.reviveFrom(this, server);
        }
    }

    @Override
    protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("Revives", revives);
    }

    @Override
    protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput input) {
        super.readAdditionalSaveData(input);
        revives = input.getBooleanOr("Revives", false);
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
        if (fightsFromTheBackdrop()) {
            // Position first, attacks second. BackgroundBossGoal owns only where he stands and
            // leaves BowserGoal's fire and charge untouched, so the staging changes without the
            // fight being rewritten.
            goalSelector.addGoal(1, new BackgroundBossGoal(this));
        }
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
