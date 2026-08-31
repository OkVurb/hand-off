package com.studio.planeshift.server;

import com.studio.planeshift.PlaneShift;
import com.studio.planeshift.common.course.CourseState;
import com.studio.planeshift.common.entity.ToadEntity;
import com.studio.planeshift.common.registry.ModEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

/**
 * Handles purchases from a Toad shop using collected coins.
 */
public final class ToadShopService {

    private ToadShopService() {
    }

    public enum Offer {
        ONE_UP(20),
        THREE_UP(50),
        SUPER_MUSHROOM(25),
        MEGA_MUSHROOM(50),
        MINI_MUSHROOM(35),
        FIRE_FLOWER(40),
        ICE_FLOWER(40),
        HAMMER(40),
        BOOMERANG(40),
        TANOOKI(40);

        public final int price;

        Offer(int price) {
            this.price = price;
        }
    }

    public static void purchase(ServerPlayer player, int slot) {
        Offer[] offers = Offer.values();
        if (slot < 0 || slot >= offers.length) {
            return;
        }
        Offer offer = offers[slot];
        if (!nearToad(player)) {
            return;
        }
        CourseState state = CourseStateAccess.get(player);
        if (!state.inCourse()) {
            return;
        }
        if (state.coins() < offer.price) {
            player.level().playSound(null, player.blockPosition(),
                    SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.8F, 1.4F);
            return;
        }

        CourseStateAccess.update(player, s -> s.withCoins(s.coins() - offer.price));
        switch (offer) {
            case ONE_UP -> CourseStateAccess.update(player, s -> s.withLives(s.lives() + 1));
            case THREE_UP -> CourseStateAccess.update(player, s -> s.withLives(s.lives() + 3));
            case SUPER_MUSHROOM -> {
                CourseStateAccess.update(player, s -> s.withPips(CourseState.MAX_PIPS, 0L));
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 600, 0, false, false, true));
            }
            case MEGA_MUSHROOM -> {
                CourseStateAccess.update(player, s -> s.withPips(CourseState.MAX_PIPS, 0L));
                player.addEffect(new MobEffectInstance(ModEffects.MEGA_AURA, 300, 0, false, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 300, 2, false, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 300, 1, false, false, true));
            }
            case MINI_MUSHROOM -> {
                CourseStateAccess.update(player, s -> s.withPips(CourseState.MAX_PIPS, 0L));
                player.addEffect(new MobEffectInstance(ModEffects.MINI_AURA, 300, 0, false, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 300, 2, false, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.SPEED, 300, 2, false, false, true));
            }
            case FIRE_FLOWER -> {
                CourseStateAccess.update(player, s -> s.withPips(CourseState.MAX_PIPS, 0L));
                FormService.grant(player, PlaneShift.id("fire_flower"));
                player.addEffect(new MobEffectInstance(ModEffects.FIRE_AURA, 400, 0, false, false, true));
            }
            case ICE_FLOWER -> {
                CourseStateAccess.update(player, s -> s.withPips(CourseState.MAX_PIPS, 0L));
                FormService.grant(player, PlaneShift.id("ice_flower"));
                player.addEffect(new MobEffectInstance(ModEffects.ICE_AURA, 400, 0, false, false, true));
            }
            case HAMMER -> {
                CourseStateAccess.update(player, s -> s.withPips(CourseState.MAX_PIPS, 0L));
                FormService.grant(player, PlaneShift.id("hammer"));
                player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 400, 0, false, false, true));
            }
            case BOOMERANG -> {
                CourseStateAccess.update(player, s -> s.withPips(CourseState.MAX_PIPS, 0L));
                FormService.grant(player, PlaneShift.id("boomerang"));
            }
            case TANOOKI -> {
                CourseStateAccess.update(player, s -> s.withPips(CourseState.MAX_PIPS, 0L));
                FormService.grant(player, PlaneShift.id("tanooki"));
                player.addEffect(new MobEffectInstance(ModEffects.LEAF_AURA, 400, 0, false, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 400, 0, false, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 400, 1, false, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.SPEED, 400, 1, false, false, true));
            }
        }
        player.level().playSound(null, player.blockPosition(),
                SoundEvents.VILLAGER_YES, SoundSource.PLAYERS, 0.8F, 1.4F);
    }

    private static boolean nearToad(ServerPlayer player) {
        AABB range = player.getBoundingBox().inflate(4.0D);
        for (Entity entity : player.level().getEntities(player, range)) {
            if (entity instanceof ToadEntity) {
                return true;
            }
        }
        return false;
    }
}
