package com.studio.planeshift.server;

import com.studio.planeshift.common.course.CourseState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodData;

/**
 * Keeps the hunger bar full so the platformer focus stays on pips and lives, not food.
 *
 * <p>Runs on {@code PlayerTickEvent.Post}, which is after vanilla's own food tick, so whatever that
 * tick took off is put straight back the same tick. The bar is therefore always full and hunger
 * never becomes a thing the player manages.
 *
 * <h2>What used to be here</h2>
 *
 * <p>A reflective handle on {@code FoodData.exhaustionLevel}, resolved by field name and zeroed
 * every tick. It is gone for two reasons, and the second is the important one.
 *
 * <p>It was unnecessary: exhaustion matters only because it eventually drains saturation and then
 * food, and both of those are being set to maximum every tick regardless of what exhaustion has
 * accumulated. Zeroing it changed nothing observable.
 *
 * <p>And it failed silently. {@code getDeclaredField} was wrapped in a bare {@code catch}, so on
 * any mapping where that field is named differently the service quietly did nothing and said
 * nothing — the same shape as {@code CameraProfile.damping} and the {@code cameraSmoothing} option,
 * which this project has already been bitten by twice. A control that cannot report its own failure
 * is worse than no control, and this one was not even needed.
 */
public final class HungerService {

    private HungerService() {
    }

    public static void tick(ServerPlayer player) {
        CourseState state = CourseStateAccess.get(player);
        if (!state.inCourse()) {
            return;
        }
        FoodData food = player.getFoodData();
        food.setFoodLevel(20);
        food.setSaturation(20.0F);
    }
}
