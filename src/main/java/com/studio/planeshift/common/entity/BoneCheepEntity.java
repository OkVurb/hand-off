package com.studio.planeshift.common.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * The fish, drowned: identical behaviour, bones instead of flesh.
 *
 * <p>A per-world reskin, which the reference leans on heavily -- the same enemy repainted for the
 * world it appears in, so a flooded tower feels like a drowned version of somewhere rather than a
 * different game. Behaviour is inherited untouched, because a reskin that also changed how the
 * thing moved would not be a reskin.
 *
 * <p>A separate entity type rather than a synced skin value, and that is a compromise rather than
 * the right answer. {@code CourseEnemyRenderer} already picks a texture from a synced variant, but
 * only for Koopalings, and generalising it means changing render state that no test can check.
 * Registering a type costs one entry and is provably safe; the general mechanism is in BACKLOG,
 * where it belongs once there are enough reskins to pay for it.
 */
public class BoneCheepEntity extends CheepCheepEntity {

    public BoneCheepEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    /**
     * Slightly quicker than the living fish.
     *
     * <p>The one deliberate difference. It appears in later, darker worlds, and an enemy the player
     * already knows how to read should ask a little more of them the second time -- otherwise the
     * repaint is only a repaint.
     */
    @Override
    protected double swimSpeed() {
        return 0.072D;
    }
}
