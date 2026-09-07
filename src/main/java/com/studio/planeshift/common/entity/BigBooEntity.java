package com.studio.planeshift.common.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * The big ghost: same shyness, half the room.
 *
 * <p>Checked before building — Big Boos are in the reference, alongside Mega Cheep-Cheeps and Mega
 * Piranha Plants. The mod already had the small one and already had the pattern: {@link
 * BigCheepEntity} is a separate registration for exactly this reason, because {@link
 * EnemyRigProfile}'s scale is tied to the registered hitbox and size is the one thing a synced
 * variant cannot carry.
 *
 * <p>Nothing about the behaviour changes, and that is the design rather than the shortcut. A Boo is
 * already unkillable and already answered by looking at it; making the big one faster or tougher
 * would be answering a question nobody asked. What changes is that it fills the corridor, so a
 * player who turns their back is not gambling with a gap they might slip through.
 */
public class BigBooEntity extends BooEntity {

    public BigBooEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return BooEntity.createAttributes()
                // More health than it needs, since nothing ordinary can hurt it either way. It
                // matters only against a star, and a big Boo that popped as fast as a small one
                // would make the star feel like less than it is.
                .add(Attributes.MAX_HEALTH, 14.0D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D);
    }
}
