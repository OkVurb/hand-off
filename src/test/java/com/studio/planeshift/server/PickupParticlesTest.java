package com.studio.planeshift.server;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.studio.planeshift.common.item.AcornItem;
import com.studio.planeshift.common.item.BoomerangItem;
import com.studio.planeshift.common.item.CloudFlowerItem;
import com.studio.planeshift.common.item.ExtraPipItem;
import com.studio.planeshift.common.item.FireFlowerItem;
import com.studio.planeshift.common.item.HammerItem;
import com.studio.planeshift.common.item.IceFlowerItem;
import com.studio.planeshift.common.item.LeafItem;
import com.studio.planeshift.common.item.MegaMushroomItem;
import com.studio.planeshift.common.item.MiniMushroomItem;
import com.studio.planeshift.common.item.PoisonMushroomItem;
import com.studio.planeshift.common.item.PropellerMushroomItem;
import com.studio.planeshift.common.item.StarPowerItem;
import com.studio.planeshift.common.item.SuperMushroomItem;
import com.studio.planeshift.common.item.TanookiSuitItem;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.item.Item;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The mapping is a pure class-keyed table, so the property that matters can be checked without a
 * world: pickups the player has to tell apart mid-run must not throw the same burst.
 *
 * <p>These go through {@code forClass} rather than holding items, because NeoForge refuses to
 * construct an {@link Item} outside registration — a test can name the classes but can never hold
 * an instance.
 */
class PickupParticlesTest {

    /** Every power-up, minus the currency items which deliberately share the coin sparkle. */
    private static Map<String, Class<? extends Item>> powerUps() {
        Map<String, Class<? extends Item>> items = new LinkedHashMap<>();
        items.put("super", SuperMushroomItem.class);
        items.put("mega", MegaMushroomItem.class);
        items.put("mini", MiniMushroomItem.class);
        items.put("fire", FireFlowerItem.class);
        items.put("ice", IceFlowerItem.class);
        items.put("propeller", PropellerMushroomItem.class);
        items.put("cloud", CloudFlowerItem.class);
        items.put("leaf", LeafItem.class);
        items.put("tanooki", TanookiSuitItem.class);
        items.put("hammer", HammerItem.class);
        items.put("boomerang", BoomerangItem.class);
        items.put("acorn", AcornItem.class);
        items.put("pip", ExtraPipItem.class);
        items.put("star", StarPowerItem.class);
        items.put("poison", PoisonMushroomItem.class);
        return items;
    }

    @Test
    @DisplayName("every power-up throws a real burst rather than nothing")
    void everyPowerUpThrowsSomething() {
        powerUps().forEach((name, type) -> {
            PickupParticles.Burst burst = PickupParticles.forClass(type);
            assertNotNull(burst, name);
            assertTrue(burst.count() > 0, name + " throws no particles, which reads as a dropped input");
            assertTrue(burst.spread() > 0.0D, name + " throws every particle at one point");
        });
    }

    /**
     * The regression this class exists for. Before {@code PickupParticles} every power-up emitted
     * the same glow, so in a side-on course — where the item is behind the player within two
     * frames — the burst confirmed only that <em>something</em> was collected.
     */
    @Test
    @DisplayName("power-ups are not all mapped to one shared burst")
    void burstsAreNotAllIdentical() {
        long distinct = powerUps().values().stream()
                .map(PickupParticles::forClass)
                .map(PickupParticles.Burst::type)
                .distinct()
                .count();

        assertTrue(distinct >= 8,
                "only " + distinct + " distinct particle types across " + powerUps().size()
                        + " power-ups; the burst stops being an identifier");
    }

    /**
     * Mega and Mini share a silhouette and have opposite effects, so grabbing the wrong one is a
     * real mistake the player must see at once. Their bursts differ in shape, not only in type:
     * Mega throws wide, Mini stays tight.
     */
    @Test
    @DisplayName("the two size mushrooms are distinguishable by shape, not just colour")
    void megaAndMiniDifferInShape() {
        PickupParticles.Burst mega = PickupParticles.forClass(MegaMushroomItem.class);
        PickupParticles.Burst mini = PickupParticles.forClass(MiniMushroomItem.class);

        assertNotEquals(mega.type(), mini.type(), "same particle for opposite power-ups");
        assertTrue(mega.spread() > mini.spread() * 2.0D,
                "Mega must burst visibly wider than Mini, not merely differently coloured");
    }

    /** Fire and ice are the pair most easily confused; they must not look alike. */
    @Test
    @DisplayName("fire and ice throw different particles")
    void fireAndIceDiffer() {
        assertNotEquals(PickupParticles.forClass(FireFlowerItem.class).type(),
                PickupParticles.forClass(IceFlowerItem.class).type());
    }

    /** The trap must never look like a reward. */
    @Test
    @DisplayName("the poison mushroom does not share a burst with any beneficial power-up")
    void poisonLooksLikeNothingGood() {
        ParticleOptions poison = PickupParticles.forClass(PoisonMushroomItem.class).type();

        powerUps().forEach((name, type) -> {
            if (name.equals("poison")) {
                return;
            }
            assertNotEquals(poison, PickupParticles.forClass(type).type(),
                    "poison shares its burst with " + name + ", so the trap reads as a reward");
        });
    }

    @Test
    @DisplayName("an unmapped item still gets a burst rather than nothing")
    void unknownItemFallsBack() {
        PickupParticles.Burst burst = PickupParticles.forClass(Item.class);
        assertNotNull(burst);
        assertTrue(burst.count() > 0);
    }
}
