package com.studio.planeshift.common.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * The big fish: same behaviour, twice the body.
 *
 * <p>A separate entity type rather than a synced variant of {@link CheepCheepEntity}, and that is
 * the codebase's own rule rather than a preference. {@code Koopaling} keeps eight siblings on one
 * type precisely <em>because</em> they are all the same size -- the note there says the scale is
 * tied to the registered hitbox, so per-sibling sizes would mean per-sibling registrations. Size is
 * the one thing a variant cannot carry, and size is the entire difference here.
 *
 * <p>Slower and tougher, because a big fish that moved like a small one would just be a small fish
 * that is harder to swim past. The speed difference is what makes it read as mass: it commits to a
 * direction long before it reaches you, which means it can be read from further away and dodged
 * with more room -- a bigger threat that is also a fairer one.
 */
public class BigCheepEntity extends CheepCheepEntity {

    public BigCheepEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 14.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.06D)
                // Hits harder than the small one. Two pips rather than one: the point of a big
                // enemy the player cannot kill is that meeting it costs something real.
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.ARMOR, 2.0D);
    }

    @Override
    protected double swimSpeed() {
        return 0.032D;
    }

    /** Wanders further, so its patrol covers a stretch rather than a spot. */
    @Override
    protected double swimRange() {
        return 9.0D;
    }
}
