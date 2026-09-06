package com.studio.planeshift.common.course;

import com.studio.planeshift.common.registry.ModItems;
import java.util.List;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;
import net.minecraft.world.item.Item;

/**
 * The four charms, and where a player can actually get one.
 *
 * <p>Every charm was reachable only from the creative menu. They are referenced nowhere else in the
 * mod except the tab that lists them, so in an ordinary game none of the four Forms they grant --
 * Ember Core, Gale Mantle, Barrier Block, Magnet Lantern -- could ever be held.
 *
 * <p>That mattered more than four missing items. Forms are sorted into four categories, and
 * <em>defense</em> and <em>utility</em> contain exactly one Form each: Barrier Block and Magnet
 * Lantern. Both were creative-only, so two of the game's four kinds of power-up did not exist as
 * far as a player was concerned. Offense and traversal had several reachable each and looked fine,
 * which is why nothing about this read as broken.
 *
 * <p>Charms are deliberately placed where the reward is <em>deterministic</em> -- a secret the
 * player chose to open, or the run-up to a boss -- and never on a question block. That is the rule
 * {@code FormCharmItem} was written for: progression should not sit behind a random roll, and a
 * charm exists precisely so a Form can be handed over on purpose.
 */
public final class CharmPool {

    private static final List<Supplier<? extends Item>> CHARMS = List.of(
            ModItems.EMBER_CHARM, ModItems.GALE_CHARM,
            ModItems.BARRIER_CHARM, ModItems.MAGNET_CHARM);

    private CharmPool() {
    }

    /**
     * One charm, chosen from the course seed.
     *
     * <p>Seeded rather than random so a given course always hides the same charm: a secret worth
     * remembering has to be the same secret on a second visit.
     */
    public static Item pick(RandomGenerator random) {
        return CHARMS.get(random.nextInt(CHARMS.size())).get();
    }

    /** How many charms there are, for tests. */
    public static int size() {
        return CHARMS.size();
    }
}
