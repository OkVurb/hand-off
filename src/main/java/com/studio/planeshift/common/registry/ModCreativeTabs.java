package com.studio.planeshift.common.registry;

import com.studio.planeshift.PlaneShift;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** One creative tab holding every PlaneShift course object and pickup. */
public final class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, PlaneShift.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN =
            TABS.register("main", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.planeshift.main"))
                    .icon(() -> new ItemStack(ModItems.COIN.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.COIN.get());
                        output.accept(ModItems.STAR_POWER.get());
                        output.accept(ModItems.EXTRA_PIP.get());
                        output.accept(ModItems.THREE_UP.get());
                        output.accept(ModItems.FIVE_UP.get());
                        output.accept(ModItems.SUPER_MUSHROOM.get());
                        output.accept(ModItems.MEGA_MUSHROOM.get());
                        output.accept(ModItems.MINI_MUSHROOM.get());
                        output.accept(ModItems.FIRE_FLOWER.get());
                        output.accept(ModItems.ICE_FLOWER.get());
                        output.accept(ModItems.LEAF.get());
                        output.accept(ModItems.PROPELLER_MUSHROOM.get());
                        output.accept(ModItems.ACORN.get());
                        output.accept(ModItems.CLOUD_FLOWER.get());
                        output.accept(ModItems.HAMMER.get());
                        output.accept(ModItems.BOOMERANG.get());
                        output.accept(ModItems.TANOOKI.get());
                        output.accept(ModItems.CAT_SUIT.get());
                        output.accept(ModItems.STAR_COIN.get());
                        output.accept(ModItems.EMBER_CHARM.get());
                        output.accept(ModItems.GALE_CHARM.get());
                        output.accept(ModItems.BARRIER_CHARM.get());
                        output.accept(ModItems.MAGNET_CHARM.get());
                        output.accept(ModItems.COURSE_GRASS_BLOCK_ITEM.get());
                        output.accept(ModItems.COURSE_DIRT_BLOCK_ITEM.get());
                        output.accept(ModItems.COURSE_CLOUD_BLOCK_ITEM.get());
                        output.accept(ModItems.COURSE_SAND_BLOCK_ITEM.get());
                        output.accept(ModItems.COURSE_SNOW_BLOCK_ITEM.get());
                        output.accept(ModItems.COURSE_CASTLE_BLOCK_ITEM.get());
                        output.accept(ModItems.COURSE_EMBER_BLOCK_ITEM.get());
                        output.accept(ModItems.COURSE_WOOD_BLOCK_ITEM.get());
                        output.accept(ModItems.COURSE_HARD_BLOCK_ITEM.get());
                        output.accept(ModItems.SHIFT_GATE_ITEM.get());
                        output.accept(ModItems.CHECKPOINT_BEACON_ITEM.get());
                        output.accept(ModItems.SPRING_PAD_ITEM.get());
                        output.accept(ModItems.PRIZE_CACHE_ITEM.get());
                        output.accept(ModItems.COIN_BLOCK_ITEM.get());
                        output.accept(ModItems.COIN_RING_BLOCK_ITEM.get());
                        output.accept(ModItems.QUESTION_BLOCK_ITEM.get());
                        output.accept(ModItems.HIDDEN_QUESTION_BLOCK_ITEM.get());
                        output.accept(ModItems.BRICK_BLOCK_ITEM.get());
                        output.accept(ModItems.ROTATING_BLOCK_ITEM.get());
                        output.accept(ModItems.CONVEYOR_BELT_ITEM.get());
                        output.accept(ModItems.NOTE_BLOCK_ITEM.get());
                        output.accept(ModItems.MUSIC_BLOCK_ITEM.get());
                        output.accept(ModItems.ON_OFF_BLOCK_ITEM.get());
                        output.accept(ModItems.ON_OFF_SWITCH_ITEM.get());
                        output.accept(ModItems.P_SWITCH_ITEM.get());
                        output.accept(ModItems.SPIKE_BLOCK_ITEM.get());
                        output.accept(ModItems.FLAG_POLE_ITEM.get());
                        output.accept(ModItems.WARP_PIPE_ITEM.get());
                        output.accept(ModItems.SECRET_PASSAGE_ITEM.get());
                        output.accept(ModItems.BULLET_BILL_CANNON_ITEM.get());
                        output.accept(ModItems.COURSE_ICE_BLOCK_ITEM.get());
                        output.accept(ModItems.MUNCHER_ITEM.get());
                        output.accept(ModItems.KEYHOLE_ITEM.get());
                        output.accept(ModItems.COURSE_KEY.get());
                        output.accept(ModItems.SEMISOLID_PLATFORM_ITEM.get());
                        output.accept(ModItems.TRAMPOLINE_ITEM.get());
                        output.accept(ModItems.GOOMBA_SPAWN_EGG.get());
                        output.accept(ModItems.KOOPA_SPAWN_EGG.get());
                        output.accept(ModItems.THWOMP_SPAWN_EGG.get());
                        output.accept(ModItems.BULLET_BILL_SPAWN_EGG.get());
                        output.accept(ModItems.BOO_SPAWN_EGG.get());
                        output.accept(ModItems.LAKITU_SPAWN_EGG.get());
                        output.accept(ModItems.HAMMER_BRO_SPAWN_EGG.get());
                        output.accept(ModItems.SPINY_SPAWN_EGG.get());
                        output.accept(ModItems.BUZZY_BEETLE_SPAWN_EGG.get());
                        output.accept(ModItems.PIRANHA_PLANT_SPAWN_EGG.get());
                        output.accept(ModItems.TOAD_SPAWN_EGG.get());
                        output.accept(ModItems.BOWSER_SPAWN_EGG.get());
                        output.accept(ModItems.MOVING_PLATFORM_SPAWN_EGG.get());
                    })
                    .build());

    private ModCreativeTabs() {
    }
}
