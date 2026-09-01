package com.studio.planeshift.server;

import com.studio.planeshift.common.item.AcornItem;
import com.studio.planeshift.common.item.BoomerangItem;
import com.studio.planeshift.common.item.CloudFlowerItem;
import com.studio.planeshift.common.item.CoinItem;
import com.studio.planeshift.common.item.ExtraPipItem;
import com.studio.planeshift.common.item.FireFlowerItem;
import com.studio.planeshift.common.item.FiveUpItem;
import com.studio.planeshift.common.item.HammerItem;
import com.studio.planeshift.common.item.IceFlowerItem;
import com.studio.planeshift.common.item.LeafItem;
import com.studio.planeshift.common.item.MegaMushroomItem;
import com.studio.planeshift.common.item.MiniMushroomItem;
import com.studio.planeshift.common.item.PoisonMushroomItem;
import com.studio.planeshift.common.item.PropellerMushroomItem;
import com.studio.planeshift.common.item.StarCoinItem;
import com.studio.planeshift.common.item.StarPowerItem;
import com.studio.planeshift.common.item.SuperMushroomItem;
import com.studio.planeshift.common.item.TanookiSuitItem;
import com.studio.planeshift.common.item.ThreeUpItem;
import com.studio.planeshift.common.registry.ModParticles;
import java.util.Map;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;

/**
 * Which particles a pickup throws.
 *
 * <p>Every power-up used to emit the same generic glow, with coins as the only exception. That
 * wastes the clearest feedback channel the game has: in a side-on platformer the player often
 * cannot see what they grabbed — the item is behind them within two frames — so the burst is the
 * confirmation, and if every burst looks alike it confirms only that *something* happened.
 *
 * <p>Each entry is chosen to read as the power it grants rather than as decoration: fire throws
 * flame, ice throws snow, the propeller throws an updraft of cloud, a 1-Up throws hearts. The
 * shape carries meaning too — Mega bursts wide and fast, Mini stays tight and small, so the two
 * mushrooms are distinguishable at a glance even though they are the same silhouette.
 */
public final class PickupParticles {

    /**
     * One burst.
     *
     * @param type   what to spawn
     * @param count  how many, before the second layer
     * @param spread gaussian offset from the player's centre, in blocks
     * @param speed  per-particle speed passed to the client
     */
    public record Burst(ParticleOptions type, int count, double spread, double speed) {
    }

    /** The generic burst, used by anything without a specific identity of its own. */
    private static final Burst DEFAULT = new Burst(ModParticles.PICKUP_GLOW.get(), 6, 0.2D, 0.05D);

    /**
     * Class-keyed rather than an {@code instanceof} chain.
     *
     * <p>Two reasons. It reads as the table it is, so a new power-up is one line rather than a
     * branch inserted in the right place. And it is callable from a unit test: NeoForge blocks
     * constructing an {@link Item} outside registration, so a test can name the classes but can
     * never hold an instance — a chain switching on instances is therefore untestable by
     * construction.
     */
    private static final Map<Class<? extends Item>, Burst> BURSTS = Map.ofEntries(
            // Currency: the existing coin sparkle, which players already read as "score".
            Map.entry(CoinItem.class, new Burst(ModParticles.COIN_SPARKLE.get(), 8, 0.22D, 0.06D)),
            Map.entry(StarCoinItem.class, new Burst(ModParticles.COIN_SPARKLE.get(), 8, 0.22D, 0.06D)),

            // Size changes. Same silhouette, opposite shapes: Mega throws wide and fast, Mini
            // stays tight, so which mushroom was taken is legible without reading the HUD.
            Map.entry(MegaMushroomItem.class, new Burst(ParticleTypes.EXPLOSION, 8, 0.6D, 0.0D)),
            Map.entry(MiniMushroomItem.class, new Burst(ParticleTypes.END_ROD, 8, 0.12D, 0.02D)),
            Map.entry(SuperMushroomItem.class, new Burst(ParticleTypes.HAPPY_VILLAGER, 10, 0.3D, 0.05D)),

            // Elemental Forms throw the element they grant.
            Map.entry(FireFlowerItem.class, new Burst(ParticleTypes.FLAME, 14, 0.3D, 0.06D)),
            Map.entry(IceFlowerItem.class, new Burst(ParticleTypes.SNOWFLAKE, 14, 0.3D, 0.04D)),

            // Flight and float: everything upward and airy.
            Map.entry(PropellerMushroomItem.class, new Burst(ParticleTypes.CLOUD, 12, 0.35D, 0.12D)),
            Map.entry(CloudFlowerItem.class, new Burst(ParticleTypes.CLOUD, 14, 0.4D, 0.03D)),
            Map.entry(LeafItem.class, new Burst(ParticleTypes.SPORE_BLOSSOM_AIR, 14, 0.35D, 0.02D)),
            Map.entry(TanookiSuitItem.class, new Burst(ParticleTypes.POOF, 12, 0.3D, 0.05D)),

            // Thrown-weapon Forms: a hard, metallic read rather than a soft glow.
            Map.entry(HammerItem.class, new Burst(ParticleTypes.CRIT, 12, 0.28D, 0.10D)),
            Map.entry(BoomerangItem.class, new Burst(ParticleTypes.ENCHANT, 16, 0.35D, 0.08D)),
            Map.entry(AcornItem.class, new Burst(ParticleTypes.WAX_ON, 10, 0.25D, 0.06D)),

            // Lives and health. Hearts for a pip, totem sparkle for an extra life - both already
            // mean "you gained survivability" in vanilla, so they need no learning.
            Map.entry(ExtraPipItem.class, new Burst(ParticleTypes.HEART, 6, 0.22D, 0.04D)),
            Map.entry(ThreeUpItem.class, new Burst(ParticleTypes.TOTEM_OF_UNDYING, 24, 0.4D, 0.14D)),
            Map.entry(FiveUpItem.class, new Burst(ParticleTypes.TOTEM_OF_UNDYING, 24, 0.4D, 0.14D)),

            // Invincibility: the loudest burst in the game, because it is the biggest state change.
            Map.entry(StarPowerItem.class, new Burst(ParticleTypes.END_ROD, 32, 0.45D, 0.20D)),

            // The trap. Deliberately dull and grey so it never reads as a reward mid-run.
            Map.entry(PoisonMushroomItem.class, new Burst(ParticleTypes.SMOKE, 14, 0.28D, 0.03D)));

    private PickupParticles() {
    }

    /** The burst for an item. Never null; unknown pickups fall back to the generic glow. */
    public static Burst forItem(Item item) {
        return forClass(item.getClass());
    }

    /**
     * The burst for an item class, walking up the hierarchy so a subclass inherits its parent's
     * identity rather than silently falling back to the generic glow.
     */
    public static Burst forClass(Class<?> type) {
        for (Class<?> c = type; c != null && Item.class.isAssignableFrom(c); c = c.getSuperclass()) {
            Burst burst = BURSTS.get(c);
            if (burst != null) {
                return burst;
            }
        }
        return DEFAULT;
    }

    /**
     * Spawns the burst for a pickup at the player's chest height.
     *
     * <p>Star Power gets a second layer of the mod's own glow on top. Two particle types reading
     * at once is what separates "the best pickup in the game" from "a pickup".
     */
    public static void spawn(ServerPlayer player, ServerLevel level, Item item) {
        Burst burst = forItem(item);
        emit(player, level, burst);
        if (item instanceof StarPowerItem) {
            emit(player, level, new Burst(ModParticles.PICKUP_GLOW.get(), 12, 0.4D, 0.10D));
        }
    }

    private static void emit(ServerPlayer player, ServerLevel level, Burst burst) {
        for (int i = 0; i < burst.count(); i++) {
            double ox = player.getRandom().nextGaussian() * burst.spread();
            double oy = player.getRandom().nextGaussian() * burst.spread();
            double oz = player.getRandom().nextGaussian() * burst.spread();
            level.sendParticles(burst.type(), player.getX(), player.getY(0.5D), player.getZ(),
                    1, ox, oy, oz, burst.speed());
        }
    }
}
