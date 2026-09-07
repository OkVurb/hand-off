package com.studio.planeshift.common.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * The big fish that follows you: the last of the reference's oversized four.
 *
 * <p>Fourth time through the same pattern — a separate registration, the same mesh and sheet, a
 * larger {@link EnemyRigProfile} — and by now that is the point rather than a chore. Size lives in
 * the registered hitbox, so every one of these has to be its own type, and the fact that four of
 * them are otherwise a two-line class is the evidence the rule is carrying its weight.
 *
 * <p>No slower than the small one, unlike {@link BigCheepEntity}. A big patrolling fish is easier
 * to read when it commits early; a big <em>pursuing</em> fish that also gave up speed would simply
 * never arrive, and an enemy that cannot reach you is scenery.
 */
public class MegaDeepCheepEntity extends DeepCheepEntity {

    public MegaDeepCheepEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return DeepCheepEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, 18.0D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D);
    }
}
