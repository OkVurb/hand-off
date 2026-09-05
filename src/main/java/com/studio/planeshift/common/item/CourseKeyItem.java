package com.studio.planeshift.common.item;

import net.minecraft.world.item.Item;

/**
 * A key, carried to a {@code KeyholeBlock}.
 *
 * <p>Deliberately does nothing on its own. Every other item in the mod is a power-up that changes
 * the player the moment it is picked up; this one changes nothing at all and is only worth having
 * because of somewhere else in the level. That is what makes it an objective rather than a reward.
 *
 * <p>Stack size one on purpose: carrying two keys should be impossible, so a course can never be
 * completed twice through the same door, and so finding the key is a moment rather than a counter.
 */
public class CourseKeyItem extends Item {

    public CourseKeyItem(Properties properties) {
        super(properties);
    }
}
