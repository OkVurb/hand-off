package com.studio.planeshift.server.gen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.studio.planeshift.common.course.CourseTheme;
import com.studio.planeshift.common.registry.ModItems;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.world.item.Item;
import org.junit.jupiter.api.Test;

/**
 * A player can actually hold every kind of Form.
 *
 * <p>All four charms were referenced nowhere except the creative tab, so the four Forms they grant
 * could never be held in an ordinary game. That was worse than four missing items: Forms are sorted
 * into four categories, and <em>defense</em> and <em>utility</em> contain exactly one Form each --
 * Barrier Block and Magnet Lantern, both charm-only. Two of the game's four kinds of power-up did
 * not exist as far as a player was concerned.
 *
 * <p>Nothing looked wrong, because offense and traversal each had several reachable Forms and the
 * creative menu listed all of them.
 */
class CharmReachabilityTest {

    @Test
    void everyCharmCanBeFoundInSomeCourse() {
        Set<Item> found = new HashSet<>();
        for (CourseTheme theme : CourseTheme.values()) {
            for (long seed = 0; seed < 80; seed++) {
                CourseComposer.Composition c = CourseComposer.compose(theme, 480, 3, seed);
                c.canvas().items().forEach(drop -> found.add(drop.item()));
            }
        }
        BossArena.build().items().forEach(drop -> found.add(drop.item()));

        // Not "some charm appears" -- every one of them, or a Form is still unreachable and the
        // test passes anyway.
        Set<Item> missing = new HashSet<>();
        for (Item charm : new Item[] {ModItems.EMBER_CHARM.get(), ModItems.GALE_CHARM.get(),
                ModItems.BARRIER_CHARM.get(), ModItems.MAGNET_CHARM.get()}) {
            if (!found.contains(charm)) {
                missing.add(charm);
            }
        }
        assertTrue(missing.isEmpty(),
                "these charms cannot be obtained outside creative: " + missing);
    }

    @Test
    void theBossApproachCarriesTheDefensiveCharm() {
        // Barrier Block is the only Form in the defense category. The run-up to the one fight in
        // the game is where it means the most, and it is the one placement that does not depend on
        // the player finding a secret.
        boolean carried = BossArena.build().items().stream()
                .anyMatch(drop -> drop.item() == ModItems.BARRIER_CHARM.get());
        assertTrue(carried, "no defensive charm before the boss");
    }

    @Test
    void aGivenCourseAlwaysHidesTheSameCharm() {
        // Seeded, not random. A secret worth remembering has to be the same secret on a second
        // visit, or finding it once tells the player nothing.
        for (long seed = 0; seed < 20; seed++) {
            assertEquals(charmsIn(seed), charmsIn(seed),
                    "seed " + seed + " does not reproduce");
        }
    }

    private static Set<Item> charmsIn(long seed) {
        Set<Item> items = new HashSet<>();
        CourseComposer.compose(CourseTheme.GRASS, 480, 3, seed)
                .canvas().items().forEach(drop -> items.add(drop.item()));
        return items;
    }
}
