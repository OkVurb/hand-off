package com.studio.planeshift.common.entity;

import com.studio.planeshift.common.registry.ModSounds;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A mid-world tower boss.
 *
 * <p>One entity type for all eight siblings; {@link Koopaling} carries who this one is. See that
 * enum for why it is one type rather than eight — briefly, {@code EnemyRigProfile} ties art scale
 * to hitbox size, and eight registrations would be eight chances to break that.
 *
 * <h2>Stomped, not pounded</h2>
 *
 * <p>Deliberately the opposite of {@link BowserEntity}, which excludes STOMP and requires a ground
 * pound. A tower boss is the game checking that the player has learned to jump on things, so the
 * answer has to be the move they have used since the first Goomba. Three of them, because one is a
 * regular enemy and five is a chore.
 *
 * <h2>Every attack is a different question</h2>
 *
 * <p>None of the eight is "the same shot, recoloured". Each changes what the player has to do:
 * where to stand, when to jump, whether standing still is safe at all. Eight bosses that all fire
 * a slightly different projectile is one boss fought eight times, and the whole reason to have
 * siblings is that they are not interchangeable.
 */
public class KoopalingEntity extends CourseEnemyEntity {

    private static final EntityDataAccessor<Integer> VARIANT =
            SynchedEntityData.defineId(KoopalingEntity.class, EntityDataSerializers.INT);

    /** Health per stomp. Three stomps at 10 apiece against 30 max health. */
    private static final float STOMP_DAMAGE = 10.0F;

    /** Base ticks between attacks. Long enough to read, short enough to pressure. */
    private static final int ATTACK_INTERVAL = 55;

    /** How close the player must be before the boss starts fighting rather than posing. */
    private static final double ENGAGE_RANGE = 22.0D;

    /** Reach of a floor slam, in blocks. */
    private static final double SLAM_RADIUS = 6.0D;

    /** Upward kick a slam gives a grounded player: a stumble, not a launch. */
    private static final double SLAM_LIFT = 0.42D;

    private int attackCooldown = ATTACK_INTERVAL;
    private int hopCooldown;

    public KoopalingEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.26D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.FOLLOW_RANGE, 40.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(VARIANT, 0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        // The clown car. A Koopaling in the reference fights from a hovering vehicle, which is not
        // decoration -- a boss standing on the same floor as the player turns the fight into a
        // shoving match along a line, and the answer to it is to walk forward. In the air it owns
        // a space the player cannot reach, and the fight becomes about the moments it comes down.
        goalSelector.addGoal(1, new ClownCarGoal(this));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public Koopaling variant() {
        return Koopaling.byOrdinal(entityData.get(VARIANT));
    }

    public void setVariant(Koopaling koopaling) {
        entityData.set(VARIANT, koopaling.ordinal());
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Variant", entityData.get(VARIANT));
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        entityData.set(VARIANT, input.getIntOr("Variant", 0));
    }

    /** Three stomps. The player's oldest move is the answer to a tower boss. */
    @Override
    protected float stompDamage() {
        return STOMP_DAMAGE;
    }

    /** A boss must not be lockable by a move the player can repeat at will. */
    @Override
    public boolean canBeStaggered() {
        return false;
    }

    @Override
    public java.util.Set<DefeatVector> answers() {
        return java.util.EnumSet.of(DefeatVector.STOMP, DefeatVector.GROUND_POUND,
                DefeatVector.SHELL, DefeatVector.FIRE, DefeatVector.STAR);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() || !isAlive()) {
            return;
        }
        Player target = level().getNearestPlayer(this, ENGAGE_RANGE);
        if (target == null) {
            return;
        }
        faceToward(target);
        moveFor(variant().attack(), target);

        if (--attackCooldown > 0) {
            return;
        }
        attackCooldown = intervalFor(variant().attack());
        perform(variant().attack(), target);
    }

    /** Attack cadence. Only the erratic one is unpredictable, and that is its entire character. */
    private int intervalFor(Koopaling.Attack attack) {
        return switch (attack) {
            // Never twice at the same rhythm, so the gap cannot be counted. Bounded well away from
            // zero: "unpredictable" must not collapse into "constant".
            case ERRATIC_SHOT -> 22 + random.nextInt(46);
            case SPREAD_SHOT -> ATTACK_INTERVAL + 15;
            case CHARGE -> ATTACK_INTERVAL + 25;
            default -> ATTACK_INTERVAL;
        };
    }

    /** Per-variant movement, run every tick rather than on the attack clock. */
    private void moveFor(Koopaling.Attack attack, Player target) {
        switch (attack) {
            case BALL_BOUNCE -> {
                // Never on the floor. The head is the target and it moves vertically, so the
                // player has to time a jump against a moving mark rather than a still one.
                if (onGround() && --hopCooldown <= 0) {
                    hopCooldown = 12;
                    setDeltaMovement(getDeltaMovement().x, 0.52D, getDeltaMovement().z);
                    hurtMarked = true;
                }
            }
            case CHARGE -> {
                // Runs the lane instead of casting, so the answer is to move rather than to time.
                double dx = Math.signum(target.getX() - getX());
                setDeltaMovement(dx * 0.24D, getDeltaMovement().y, getDeltaMovement().z);
                hurtMarked = true;
            }
            default -> {
            }
        }
    }

    private void perform(Koopaling.Attack attack, Player target) {
        if (!(level() instanceof ServerLevel server)) {
            return;
        }
        switch (attack) {
            case STRAIGHT_SHOT, ERRATIC_SHOT -> shoot(target, 0.0D);
            case SPREAD_SHOT -> {
                // Several at once, then off the ground: the airborne moment is what stops the
                // player answering a spread by simply standing on the boss's head.
                shoot(target, -0.18D);
                shoot(target, 0.0D);
                shoot(target, 0.18D);
                setDeltaMovement(getDeltaMovement().x, 0.46D, getDeltaMovement().z);
                hurtMarked = true;
            }
            case RICOCHET_RING -> {
                // A boomerang, because it already comes back. The point of this fight is that
                // there is no spot the player can retreat to and wait in.
                BoomerangProjectile ring = new BoomerangProjectile(level(), this);
                ring.shoot(target.getX() - getX(), 0.0D, target.getZ() - getZ(), 0.9F, 0.0F);
                level().addFreshEntity(ring);
            }
            case GROUND_SLAM -> slam(server);
            case CEILING_DROP -> {
                // Up and over the player, so the threat arrives from the one direction a
                // side-on platformer usually treats as safe.
                setDeltaMovement((target.getX() - getX()) * 0.08D, 0.92D, 0.0D);
                hurtMarked = true;
            }
            case BALL_BOUNCE -> shoot(target, 0.0D);
            case CHARGE -> {
                setDeltaMovement(Math.signum(target.getX() - getX()) * 0.55D, 0.24D, 0.0D);
                hurtMarked = true;
            }
        }
    }

    /** One shot along the lane, with an optional vertical bias for spreads. */
    private void shoot(Player target, double rise) {
        BowserFire fire = new BowserFire(level(), this);
        fire.shoot(target.getX() - getX(),
                target.getY() + 0.4D - getY() + rise * 4.0D,
                target.getZ() - getZ(), 0.85F, 0.0F);
        level().addFreshEntity(fire);
        level().playSound(null, blockPosition(), ModSounds.FIREBALL.get(),
                SoundSource.HOSTILE, 0.7F, 1.4F);
    }

    /**
     * Shakes the floor.
     *
     * <p>Only touches players who are actually standing on it. A slam that also hit an airborne
     * player would make jumping the answer to every attack in the fight, and this one exists
     * precisely to punish being on the ground.
     */
    private void slam(ServerLevel server) {
        setDeltaMovement(getDeltaMovement().x, -0.9D, getDeltaMovement().z);
        hurtMarked = true;
        server.playSound(null, blockPosition(), ModSounds.STOMP.get(),
                SoundSource.HOSTILE, 1.0F, 0.6F);

        AABB reach = getBoundingBox().inflate(SLAM_RADIUS, 2.0D, SLAM_RADIUS);
        for (Player player : server.getEntitiesOfClass(Player.class, reach)) {
            if (!player.onGround()) {
                continue;
            }
            Vec3 away = new Vec3(Math.signum(player.getX() - getX()), 0.0D, 0.0D);
            player.setDeltaMovement(away.scale(0.24D).add(0.0D, SLAM_LIFT, 0.0D));
            player.hurtMarked = true;
        }
        server.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD,
                getX(), getY(), getZ(), 18, SLAM_RADIUS * 0.4D, 0.1D, 0.3D, 0.02D);
    }

    private void faceToward(Player target) {
        // Side-on courses: only yaw matters, and only to face left or right along the rail.
        setYRot(target.getX() < getX() ? 90.0F : -90.0F);
        yBodyRot = getYRot();
        yHeadRot = getYRot();
    }
}
