package com.studio.planeshift.common.course;

import com.studio.planeshift.common.registry.ModItems;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;

/**
 * What a Toad House can hand out.
 *
 * <p>Shared because there are two places a gift can now come from: the boxes in the room, and the
 * direct grant {@code MapNodeService} falls back to when the room fails to load. Those two lists
 * drifting apart would mean the reward depended on whether the datapack was working, which is
 * exactly the class of bug that made the Toad House pay out only when it was misconfigured.
 *
 * <p>The useful Forms, deliberately not the joke ones. A Toad House is a reward for reaching it;
 * handing out a Poison Mushroom would make it a trap, and handing out a plain coin would make it
 * a disappointment.
 */
public final class ToadHouseGifts {

    private static final List<Supplier<? extends Item>> GIFTS = List.of(
            ModItems.SUPER_MUSHROOM, ModItems.FIRE_FLOWER, ModItems.ICE_FLOWER,
            ModItems.LEAF, ModItems.PROPELLER_MUSHROOM, ModItems.CAT_SUIT,
            ModItems.TANOOKI, ModItems.THREE_UP);

    private ToadHouseGifts() {
    }

    /** One gift, chosen uniformly. */
    public static Item roll(RandomSource random) {
        return GIFTS.get(random.nextInt(GIFTS.size())).get();
    }

    /** How many distinct gifts there are, for tests. */
    public static int size() {
        return GIFTS.size();
    }
}
