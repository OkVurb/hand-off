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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
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

    /**
     * Particles borrowed from the Particle Effects mod, when it is installed.
     *
     * <p>Looked up by id from the particle registry rather than compiled against, so PlaneShift
     * runs identically with or without the mod: if a lookup misses, the built-in burst is used and
     * nothing announces itself. That matters because a mod that hard-depends on an optional mod is
     * not an optional dependency, it is a required one with extra steps.
     *
     * <p>The mappings are chosen so the borrowed particle means the same thing it means in its
     * home mod. Particle Effects draws one particle per status effect, so a Fire Flower borrowing
     * its fire-resistance swirl reads as "fire" to anyone who has seen it before; a Star Power
     * borrowing absorption reads as "you are protected". Borrowing at random would just be noise
     * with someone else's art.
     */
    private static final Map<String, String> BORROWED = Map.ofEntries(
            Map.entry("fire_flower", "particle_effects:fire_resistance"),
            Map.entry("ice_flower", "particle_effects:slowness"),
            Map.entry("star_power", "particle_effects:absorption"),
            Map.entry("super_mushroom", "particle_effects:health_boost"),
            Map.entry("mega_mushroom", "particle_effects:strength"),
            Map.entry("mini_mushroom", "particle_effects:speed"),
            Map.entry("propeller_mushroom", "particle_effects:jump_boost"),
            Map.entry("cloud_flower", "particle_effects:slow_falling"),
            Map.entry("leaf", "particle_effects:slow_falling"),
            Map.entry("acorn", "particle_effects:haste"),
            Map.entry("tanooki_suit", "particle_effects:resistance"),
            Map.entry("cat_suit", "particle_effects:haste"),
            Map.entry("hammer", "particle_effects:strength"),
            Map.entry("boomerang", "particle_effects:luck"),
            Map.entry("extra_pip", "particle_effects:instant_health"),
            Map.entry("three_up", "particle_effects:regeneration"),
            Map.entry("five_up", "particle_effects:regeneration"),
            Map.entry("poison_mushroom", "particle_effects:poison"),
            Map.entry("star_coin", "particle_effects:glowing"),
            Map.entry("coin", "particle_effects:luck"));

    /** Resolved once; empty when the mod is absent or an id has been renamed. */
    private static final Map<String, ParticleOptions> BORROWED_RESOLVED = resolveBorrowed();

    private static Map<String, ParticleOptions> resolveBorrowed() {
        Map<String, ParticleOptions> resolved = new java.util.HashMap<>();
        BORROWED.forEach((key, id) -> {
            Identifier location = Identifier.tryParse(id);
            if (location == null) {
                return;
            }
            BuiltInRegistries.PARTICLE_TYPE.getOptional(location).ifPresent(type -> {
                if (type instanceof ParticleOptions options) {
                    resolved.put(key, options);
                }
            });
        });
        return Map.copyOf(resolved);
    }

    /**
     * A second, borrowed layer for a pickup, or null when none applies.
     *
     * <p>Layered on top of the built-in burst rather than replacing it, so the shape language
     * established in this class — Mega wide, Mini tight, poison dull — survives regardless of
     * which optional mods are installed.
     */
    public static ParticleOptions borrowedLayer(String key) {
        return BORROWED_RESOLVED.get(key);
    }

    /** Which pickup key an item corresponds to, for the borrowed-particle lookup. */
    private static String keyOf(Item item) {
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        return id == null ? "" : id.getPath();
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
            Map.entry(SuperMushroomItem.class, new Burst(ParticleTypes.END_ROD, 10, 0.3D, 0.05D)),

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
        ParticleOptions borrowed = borrowedLayer(keyOf(item));
        if (borrowed != null) {
            emit(player, level, new Burst(borrowed, 10, burst.spread() * 1.3D, burst.speed()));
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
