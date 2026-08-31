package com.studio.planeshift.server;

import com.studio.planeshift.common.course.CourseState;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodData;

/**
 * Keeps the hunger bar full so the platformer focus stays on pips/lives, not food.
 *
 * <p>Food and saturation are set to maximum every tick; exhaustion is zeroed so sprint,
 * jumping, and power-up use never drain the hunger bar.
 */
public final class HungerService {

    private static final Field EXHAUSTION_FIELD = resolveExhaustionField();

    private HungerService() {
    }

    private static Field resolveExhaustionField() {
        try {
            Field field = FoodData.class.getDeclaredField("exhaustionLevel");
            field.setAccessible(true);
            return field;
        } catch (Exception ignored) {
            return null;
        }
    }

    public static void tick(ServerPlayer player) {
        CourseState state = CourseStateAccess.get(player);
        if (!state.inCourse()) {
            return;
        }
        FoodData food = player.getFoodData();
        food.setFoodLevel(20);
        food.setSaturation(20.0F);
        clearExhaustion(food);
    }

    private static void clearExhaustion(FoodData food) {
        if (EXHAUSTION_FIELD == null) {
            return;
        }
        try {
            EXHAUSTION_FIELD.setFloat(food, 0.0F);
        } catch (Exception ignored) {
        }
    }
}
