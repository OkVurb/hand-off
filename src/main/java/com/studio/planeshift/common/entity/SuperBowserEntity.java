package com.studio.planeshift.common.entity;

import com.studio.planeshift.common.registry.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * Bowser after the Koopalings put him back together, and larger for it.
 *
 * <p>Read off the wiki rather than the footage: the final castle is fought twice. The first Bowser
 * is dropped into the lava by the bridge, and then the Koopalings use their wands to revive him as
 * an enormous version of himself, which is fought differently. The plan called this "phases, and
 * the phase change is visible" — the visible part is the whole idea, and it is what makes the last
 * castle end twice instead of once.
 *
 * <h2>A second entity type rather than a flag</h2>
 *
 * <p>{@link EnemyRigProfile} is emphatic that its scale is tied to the registered hitbox, because
 * art drawn larger than what it collides with produces stomps that land on the sprite and pass
 * through. Size is registered, not set, so a bigger Bowser has to be a second registration. That is
 * the same reasoning that keeps the eight Koopalings as <em>one</em> type — there the size is equal
 * so one registration is right, here it is not so two are.
 *
 * <p>Everything else is inherited. He stands in the backdrop and throws the same fire, because the
 * player has just spent a fight learning to read that and the point of the transformation is that
 * the thing they learned now arrives with more reach and more health, not that they start over.
 */
public class SuperBowserEntity extends BowserEntity implements ReachesIn {

    public SuperBowserEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    /**
     * Ticks in a full swipe: wind-up, strike, and the withdrawal.
     *
     * <p>The wind-up is most of it. The wiki's phase two is dodging claws and fire, and a claw the
     * player cannot see coming is not something they dodge -- it is something that happens to them.
     * So the boss leans visibly out of the backdrop first, and the lean is the telegraph: a real
     * movement of a real object, which is the same rule the rest of the mod follows about showing
     * a hazard's reach.
     */
    private static final int WIND_UP = 22;
    private static final int STRIKE = 6;
    private static final int WITHDRAW = 14;
    private static final int SWIPE_CYCLE = 96;

    /** How far into the lane the claw comes, as a fraction of the backdrop gap. */
    private static final double LUNGE = 0.85D;

    /** How wide a stretch of lane the claw covers. */
    private static final double SWIPE_RADIUS = 2.6D;

    private static final float CLAW_DAMAGE = 6.0F;

    private int swipeClock;

    /**
     * {@inheritDoc}
     *
     * <p>True from the first frame of the wind-up to the last of the withdrawal, so the goal keeps
     * its hands off for the whole movement rather than only while the claw is out. Yielding just
     * for the strike would make the boss snap forward and snap back, which reads as a teleport.
     */
    @Override
    public boolean reachingIn() {
        return swipeClock > 0 && swipeClock <= WIND_UP + STRIKE + WITHDRAW;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() || !isAlive()) {
            return;
        }
        swipeClock = (swipeClock + 1) % SWIPE_CYCLE;
        if (!reachingIn()) {
            return;
        }
        net.minecraft.world.entity.player.Player target =
                level().getNearestPlayer(this, 24.0D);
        if (target == null) {
            return;
        }
        // Depth only. The boss does not chase along the lane while swinging -- a claw that
        // followed the player sideways would be unavoidable, and the whole point of a fixed reach
        // is that stepping out of it works.
        double toward = swipeClock <= WIND_UP
                ? swipeClock / (double) WIND_UP
                : swipeClock <= WIND_UP + STRIKE
                        ? 1.0D
                        : 1.0D - (swipeClock - WIND_UP - STRIKE) / (double) WITHDRAW;
        double gap = target.getZ() - getZ();
        setPos(getX(), getY(), getZ() + gap * LUNGE * toward * 0.25D);

        if (swipeClock > WIND_UP && swipeClock <= WIND_UP + STRIKE
                && level() instanceof net.minecraft.server.level.ServerLevel server
                && Math.abs(target.getX() - getX()) < SWIPE_RADIUS) {
            target.hurtServer(server, damageSources().mobAttack(this), CLAW_DAMAGE);
        }
    }

    /**
     * This one does stand in the backdrop, and is the only thing in the game that does.
     *
     * <p>It is the staging the plan's first boss entry asks for and the reason the entry is worth
     * having: a boss standing off the rail changes the question from "can I get past it" to "can I
     * read what it is about to do". Applied to the first Bowser it was both wrong to the reference
     * and fatal to him; applied here it is exactly right, because being too big for the corridor is
     * the whole content of the transformation.
     */
    @Override
    protected boolean fightsFromTheBackdrop() {
        return true;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return BowserEntity.createAttributes()
                // Not double. The first fight already took the player's supplies, and a second bar
                // twice as long past that is not a climax, it is the same fight for longer.
                .add(Attributes.MAX_HEALTH, 90.0D)
                .add(Attributes.ATTACK_DAMAGE, 7.0D)
                // Slower, deliberately. He is much bigger and reaches much further, so the thing
                // that keeps him readable is that he commits to where he is going.
                .add(Attributes.MOVEMENT_SPEED, 0.17D);
    }

    /**
     * Revives {@code fallen} as this, in place.
     *
     * <p>Called from the first Bowser's death rather than from the arena, so it happens wherever
     * and however he goes down — into the lava when the bridge drops, or on the bank if the player
     * simply out-damages him. A phase change that only fires on one of those is a phase change the
     * player can accidentally skip.
     */
    public static void reviveFrom(BowserEntity fallen, ServerLevel level) {
        SuperBowserEntity risen = ModEntities.SUPER_BOWSER.get().create(level,
                net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
        if (risen == null) {
            return;
        }
        // On the bank rather than in the pit he fell into. He is being put back together by
        // somebody else, so he does not have to come back from where he landed.
        risen.snapTo(fallen.getX(), Math.max(fallen.getY(), 1.0D), fallen.getZ(),
                fallen.getYRot(), 0.0F);
        level.addFreshEntity(risen);
        level.levelEvent(1023, risen.blockPosition(), 0);
    }
}
