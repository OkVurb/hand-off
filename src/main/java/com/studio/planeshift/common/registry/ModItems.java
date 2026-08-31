package com.studio.planeshift.common.registry;

import com.studio.planeshift.PlaneShift;
import com.studio.planeshift.common.item.CoinItem;
import com.studio.planeshift.common.item.ExtraPipItem;
import com.studio.planeshift.common.item.FireFlowerItem;
import com.studio.planeshift.common.item.FiveUpItem;
import com.studio.planeshift.common.item.FormCharmItem;
import com.studio.planeshift.common.item.BoomerangItem;
import com.studio.planeshift.common.item.HammerItem;
import com.studio.planeshift.common.item.IceFlowerItem;
import com.studio.planeshift.common.item.AcornItem;
import com.studio.planeshift.common.item.CloudFlowerItem;
import com.studio.planeshift.common.item.LeafItem;
import com.studio.planeshift.common.item.MegaMushroomItem;
import com.studio.planeshift.common.item.MiniMushroomItem;
import com.studio.planeshift.common.item.PropellerMushroomItem;
import com.studio.planeshift.common.item.StarCoinItem;
import com.studio.planeshift.common.item.StarPowerItem;
import com.studio.planeshift.common.item.SuperMushroomItem;
import com.studio.planeshift.common.item.TanookiSuitItem;
import com.studio.planeshift.common.item.ThreeUpItem;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.function.Supplier;

/**
 * Items. Coins are the abundant course currency; Form charms are deterministic
 * teaching pickups for the four vertical-slice Forms.
 */
public final class ModItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(PlaneShift.MOD_ID);

    /** Mario-style pickups. */
    public static final DeferredItem<CoinItem> COIN =
            ITEMS.registerItem("coin", CoinItem::new, p -> p.stacksTo(64));
    public static final DeferredItem<StarPowerItem> STAR_POWER =
            ITEMS.registerItem("star_power", StarPowerItem::new, p -> p.stacksTo(16));
    public static final DeferredItem<ExtraPipItem> EXTRA_PIP =
            ITEMS.registerItem("extra_pip", ExtraPipItem::new, p -> p.stacksTo(16));
    public static final DeferredItem<ThreeUpItem> THREE_UP =
            ITEMS.registerItem("three_up", ThreeUpItem::new, p -> p.stacksTo(16));
    public static final DeferredItem<FiveUpItem> FIVE_UP =
            ITEMS.registerItem("five_up", FiveUpItem::new, p -> p.stacksTo(16));
    public static final DeferredItem<SuperMushroomItem> SUPER_MUSHROOM =
            ITEMS.registerItem("super_mushroom", SuperMushroomItem::new, p -> p.stacksTo(16));
    public static final DeferredItem<MegaMushroomItem> MEGA_MUSHROOM =
            ITEMS.registerItem("mega_mushroom", MegaMushroomItem::new, p -> p.stacksTo(16));
    public static final DeferredItem<MiniMushroomItem> MINI_MUSHROOM =
            ITEMS.registerItem("mini_mushroom", MiniMushroomItem::new, p -> p.stacksTo(16));
    public static final DeferredItem<FireFlowerItem> FIRE_FLOWER =
            ITEMS.registerItem("fire_flower", FireFlowerItem::new, p -> p.stacksTo(16));
    public static final DeferredItem<IceFlowerItem> ICE_FLOWER =
            ITEMS.registerItem("ice_flower", IceFlowerItem::new, p -> p.stacksTo(16));
    public static final DeferredItem<LeafItem> LEAF =
            ITEMS.registerItem("leaf", LeafItem::new, p -> p.stacksTo(16));
    public static final DeferredItem<PropellerMushroomItem> PROPELLER_MUSHROOM =
            ITEMS.registerItem("propeller_mushroom", PropellerMushroomItem::new, p -> p.stacksTo(16));
    public static final DeferredItem<AcornItem> ACORN =
            ITEMS.registerItem("acorn", AcornItem::new, p -> p.stacksTo(16));
    public static final DeferredItem<CloudFlowerItem> CLOUD_FLOWER =
            ITEMS.registerItem("cloud_flower", CloudFlowerItem::new, p -> p.stacksTo(16));
    public static final DeferredItem<StarCoinItem> STAR_COIN =
            ITEMS.registerItem("star_coin", StarCoinItem::new, p -> p.stacksTo(16));
    public static final DeferredItem<HammerItem> HAMMER =
            ITEMS.registerItem("hammer", HammerItem::new, p -> p.stacksTo(16));
    public static final DeferredItem<BoomerangItem> BOOMERANG =
            ITEMS.registerItem("boomerang", BoomerangItem::new, p -> p.stacksTo(16));
    public static final DeferredItem<TanookiSuitItem> TANOOKI =
            ITEMS.registerItem("tanooki", TanookiSuitItem::new, p -> p.stacksTo(16));

    /** Spawn eggs for creative testing. */
    public static final DeferredItem<SpawnEggItem> GOOMBA_SPAWN_EGG =
            registerSpawnEgg("goomba_spawn_egg", ModEntities.GOOMBA);
    public static final DeferredItem<SpawnEggItem> KOOPA_SPAWN_EGG =
            registerSpawnEgg("koopa_spawn_egg", ModEntities.KOOPA);
    public static final DeferredItem<SpawnEggItem> THWOMP_SPAWN_EGG =
            registerSpawnEgg("thwomp_spawn_egg", ModEntities.THWOMP);
    public static final DeferredItem<SpawnEggItem> SPINY_SPAWN_EGG =
            registerSpawnEgg("spiny_spawn_egg", ModEntities.SPINY);
    public static final DeferredItem<SpawnEggItem> BUZZY_BEETLE_SPAWN_EGG =
            registerSpawnEgg("buzzy_beetle_spawn_egg", ModEntities.BUZZY_BEETLE);
    public static final DeferredItem<SpawnEggItem> PIRANHA_PLANT_SPAWN_EGG =
            registerSpawnEgg("piranha_plant_spawn_egg", ModEntities.PIRANHA_PLANT);
    public static final DeferredItem<SpawnEggItem> TOAD_SPAWN_EGG =
            registerSpawnEgg("toad_spawn_egg", ModEntities.TOAD);
    public static final DeferredItem<SpawnEggItem> BOWSER_SPAWN_EGG =
            registerSpawnEgg("bowser_spawn_egg", ModEntities.BOWSER);
    public static final DeferredItem<SpawnEggItem> MOVING_PLATFORM_SPAWN_EGG =
            registerSpawnEgg("moving_platform_spawn_egg", ModEntities.MOVING_PLATFORM);
    public static final DeferredItem<SpawnEggItem> BULLET_BILL_SPAWN_EGG =
            registerSpawnEgg("bullet_bill_spawn_egg", ModEntities.BULLET_BILL);
    public static final DeferredItem<SpawnEggItem> BOO_SPAWN_EGG =
            registerSpawnEgg("boo_spawn_egg", ModEntities.BOO);
    public static final DeferredItem<SpawnEggItem> LAKITU_SPAWN_EGG =
            registerSpawnEgg("lakitu_spawn_egg", ModEntities.LAKITU);
    public static final DeferredItem<SpawnEggItem> HAMMER_BRO_SPAWN_EGG =
            registerSpawnEgg("hammer_bro_spawn_egg", ModEntities.HAMMER_BRO);

    public static final DeferredItem<FormCharmItem> EMBER_CHARM = registerCharm("ember_charm", "ember_core");
    public static final DeferredItem<FormCharmItem> GALE_CHARM = registerCharm("gale_charm", "gale_mantle");
    public static final DeferredItem<FormCharmItem> BARRIER_CHARM = registerCharm("barrier_charm", "barrier_block");
    public static final DeferredItem<FormCharmItem> MAGNET_CHARM = registerCharm("magnet_charm", "magnet_lantern");

    // Block items.
    public static final DeferredItem<?> SHIFT_GATE_ITEM =
            ITEMS.registerSimpleBlockItem(ModBlocks.SHIFT_GATE);
    public static final DeferredItem<?> CHECKPOINT_BEACON_ITEM =
            ITEMS.registerSimpleBlockItem(ModBlocks.CHECKPOINT_BEACON);
    public static final DeferredItem<?> SPRING_PAD_ITEM =
            ITEMS.registerSimpleBlockItem(ModBlocks.SPRING_PAD);
    public static final DeferredItem<?> PRIZE_CACHE_ITEM =
            ITEMS.registerSimpleBlockItem(ModBlocks.PRIZE_CACHE);

    public static final DeferredItem<?> COIN_BLOCK_ITEM =
            ITEMS.registerSimpleBlockItem(ModBlocks.COIN_BLOCK);
    public static final DeferredItem<?> COIN_RING_BLOCK_ITEM =
            ITEMS.registerSimpleBlockItem(ModBlocks.COIN_RING_BLOCK);

    public static final DeferredItem<?> QUESTION_BLOCK_ITEM =
            ITEMS.registerSimpleBlockItem(ModBlocks.QUESTION_BLOCK);
    public static final DeferredItem<?> HIDDEN_QUESTION_BLOCK_ITEM =
            ITEMS.registerSimpleBlockItem(ModBlocks.HIDDEN_QUESTION_BLOCK);
    public static final DeferredItem<?> BRICK_BLOCK_ITEM =
            ITEMS.registerSimpleBlockItem(ModBlocks.BRICK_BLOCK);

    public static final DeferredItem<?> CONVEYOR_BELT_ITEM =
            ITEMS.registerSimpleBlockItem(ModBlocks.CONVEYOR_BELT);
    public static final DeferredItem<?> NOTE_BLOCK_ITEM =
            ITEMS.registerSimpleBlockItem(ModBlocks.NOTE_BLOCK);
    public static final DeferredItem<?> MUSIC_BLOCK_ITEM =
            ITEMS.registerSimpleBlockItem(ModBlocks.MUSIC_BLOCK);
    public static final DeferredItem<?> ON_OFF_BLOCK_ITEM =
            ITEMS.registerSimpleBlockItem(ModBlocks.ON_OFF_BLOCK);
    public static final DeferredItem<?> ON_OFF_SWITCH_ITEM =
            ITEMS.registerSimpleBlockItem(ModBlocks.ON_OFF_SWITCH);
    public static final DeferredItem<?> P_SWITCH_ITEM =
            ITEMS.registerSimpleBlockItem(ModBlocks.P_SWITCH);
    public static final DeferredItem<?> SPIKE_BLOCK_ITEM =
            ITEMS.registerSimpleBlockItem(ModBlocks.SPIKE_BLOCK);

    public static final DeferredItem<?> FLAG_POLE_ITEM =
            ITEMS.registerSimpleBlockItem(ModBlocks.FLAG_POLE);
    public static final DeferredItem<?> WARP_PIPE_ITEM =
            ITEMS.registerSimpleBlockItem(ModBlocks.WARP_PIPE);
    public static final DeferredItem<?> SECRET_PASSAGE_ITEM =
            ITEMS.registerSimpleBlockItem(ModBlocks.SECRET_PASSAGE);

    private static DeferredItem<FormCharmItem> registerCharm(String name, String formPath) {
        return ITEMS.registerItem(name,
                properties -> new FormCharmItem(PlaneShift.id(formPath), properties),
                p -> p.stacksTo(16).rarity(Rarity.UNCOMMON));
    }

    private static DeferredItem<SpawnEggItem> registerSpawnEgg(String name, Supplier<? extends EntityType<?>> type) {
        return ITEMS.registerItem(name,
                properties -> new SpawnEggItem(properties.component(
                        net.minecraft.core.component.DataComponents.ENTITY_DATA,
                        net.minecraft.world.item.component.TypedEntityData.of(type.get(), new net.minecraft.nbt.CompoundTag()))),
                p -> p.stacksTo(64));
    }

    private ModItems() {
    }
}
