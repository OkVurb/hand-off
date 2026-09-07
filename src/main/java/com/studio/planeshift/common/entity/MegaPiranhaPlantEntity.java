package com.studio.planeshift.common.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * The big plant: the same rhythm, and no longer a gap you can slip through.
 *
 * <p>Third of the reference's oversized enemies to be built here, after {@link BigCheepEntity} —
 * which shipped as one without being called one — and {@link BigBooEntity}. The pattern is settled
 * by now: a separate registration, the same mesh, a larger {@link EnemyRigProfile}, because scale
 * lives in the hitbox and a synced variant cannot carry it.
 *
 * <p>The timings are untouched, and that is the whole point of a big version. A Piranha Plant is a
 * metronome the player learns and passes on the beat; changing the beat would make this a different
 * enemy rather than a bigger one. What changes is how much of the doorway is closed while it is up.
 */
public class MegaPiranhaPlantEntity extends PiranhaPlantEntity {

    public MegaPiranhaPlantEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PiranhaPlantEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, 26.0D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D);
    }

    /**
     * Rises further, in proportion to the body.
     *
     * <p>Not a difficulty knob. At the inherited height this plant's head would still be inside its
     * own pipe at full extension, so the rise and fall the player times their run against would
     * happen out of sight.
     */
    @Override
    protected double emergeHeight() {
        return 2.4D;
    }
}
