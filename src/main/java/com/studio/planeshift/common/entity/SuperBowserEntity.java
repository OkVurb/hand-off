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
public class SuperBowserEntity extends BowserEntity {

    public SuperBowserEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
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
