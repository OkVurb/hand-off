package com.studio.planeshift.common.item;

import net.minecraft.world.item.Item;

/**
 * Cat Suit pickup.
 *
 * <p>Grants the claw swipe as its Form action, a diving pounce, and a wall cling. The climbing and
 * all-fours run are deliberately not implemented here — see {@code docs/THREE_D_AND_ANIMATION.md}:
 * Player Animation Library and ParCool already do both properly, and a second implementation of
 * someone else's solved problem would be worse and fight theirs.
 *
 * <p>What is left is the part that touches systems this mod owns: the pounce interacts with the
 * enemy stomp contract, and the dive reuses the ground-pound path.
 */
public class CatSuitItem extends Item {

    public CatSuitItem(Properties properties) {
        super(properties);
    }
}
