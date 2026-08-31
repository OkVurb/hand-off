package com.studio.planeshift.common.item;

import net.minecraft.world.item.Item;

/**
 * Poison Mushroom: a trap that reads like a power-up.
 *
 * <p>Deliberately shaped like a Super Mushroom so it can be mistaken for one at speed. Picking it
 * up costs a pip through the ordinary damage path, so the Form buffer absorbs it first and the
 * invulnerability window applies exactly as it would for any other hit.
 *
 * <p>Its whole design job is to teach the player to read colour before grabbing, which only works
 * if it drifts and pops like the real thing — see {@code PowerupDriftService}.
 */
public class PoisonMushroomItem extends Item {

    public PoisonMushroomItem(Properties properties) {
        super(properties);
    }
}
