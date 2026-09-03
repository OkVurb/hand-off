package com.studio.planeshift.server;

import com.studio.planeshift.PlaneShift;
import com.studio.planeshift.common.PlaneShiftConfig;
import com.studio.planeshift.common.TesterActions;
import com.studio.planeshift.common.course.CourseProgress;
import com.studio.planeshift.common.course.CourseState;
import com.studio.planeshift.common.course.WorldDefinition;
import com.studio.planeshift.common.course.WorldRegistry;
import com.studio.planeshift.common.registry.ModEntities;
import com.studio.planeshift.common.registry.ModItems;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Server side of the tester menu.
 *
 * <p>Everything the menu can do is enumerated here and dispatched by name. The client sends a
 * string; this decides whether it means anything. That ordering matters — the menu is a
 * convenience for reaching these actions, never the thing that authorises them, so a handcrafted
 * packet gets exactly the same treatment as a button press.
 *
 * <p>Gated on operator permission, or on the {@code testerMenu} server config for playtest
 * instances where there is no operator. Off that switch, a public server cannot have players
 * handing themselves Star Power and unlocking every course.
 */
public final class TesterService {

    /** Items the menu can grant, in the order the menu shows them. */
    private static final Map<String, Supplier<? extends Item>> GRANTS = buildGrants();

    /** Enemies the menu can spawn, in the order the menu shows them. */
    private static final Map<String, Supplier<? extends EntityType<?>>> SPAWNS = buildSpawns();

    /** How far in front of the player a spawned enemy appears. */
    private static final double SPAWN_DISTANCE = 4.0D;

    private TesterService() {
    }

    private static Map<String, Supplier<? extends Item>> buildGrants() {
        Map<String, Supplier<? extends Item>> map = new LinkedHashMap<>();
        map.put("super_mushroom", ModItems.SUPER_MUSHROOM);
        map.put("mega_mushroom", ModItems.MEGA_MUSHROOM);
        map.put("mini_mushroom", ModItems.MINI_MUSHROOM);
        map.put("fire_flower", ModItems.FIRE_FLOWER);
        map.put("ice_flower", ModItems.ICE_FLOWER);
        map.put("leaf", ModItems.LEAF);
        map.put("propeller_mushroom", ModItems.PROPELLER_MUSHROOM);
        map.put("cloud_flower", ModItems.CLOUD_FLOWER);
        map.put("tanooki_suit", ModItems.TANOOKI);
        map.put("cat_suit", ModItems.CAT_SUIT);
        map.put("hammer", ModItems.HAMMER);
        map.put("boomerang", ModItems.BOOMERANG);
        map.put("acorn", ModItems.ACORN);
        map.put("star_power", ModItems.STAR_POWER);
        map.put("poison_mushroom", ModItems.POISON_MUSHROOM);
        map.put("extra_pip", ModItems.EXTRA_PIP);
        map.put("three_up", ModItems.THREE_UP);
        map.put("five_up", ModItems.FIVE_UP);
        map.put("coin", ModItems.COIN);
        map.put("star_coin", ModItems.STAR_COIN);
        return map;
    }

    private static Map<String, Supplier<? extends EntityType<?>>> buildSpawns() {
        Map<String, Supplier<? extends EntityType<?>>> map = new LinkedHashMap<>();
        map.put("goomba", ModEntities.GOOMBA);
        map.put("koopa", ModEntities.KOOPA);
        map.put("buzzy_beetle", ModEntities.BUZZY_BEETLE);
        map.put("spiny", ModEntities.SPINY);
        map.put("lakitu", ModEntities.LAKITU);
        map.put("boo", ModEntities.BOO);
        map.put("thwomp", ModEntities.THWOMP);
        map.put("hammer_bro", ModEntities.HAMMER_BRO);
        map.put("piranha_plant", ModEntities.PIRANHA_PLANT);
        map.put("bullet_bill", ModEntities.BULLET_BILL);
        map.put("bowser", ModEntities.BOWSER);
        map.put("toad", ModEntities.TOAD);
        return map;
    }

    public static boolean allowed(ServerPlayer player) {
        // Operator, or the playtest switch. Checked in that order so turning the switch off on a
        // real server still leaves operators able to use it.
        MinecraftServer server = player.level().getServer();
        return (server != null && server.getPlayerList().isOp(player.nameAndId()))
                || PlaneShiftConfig.SERVER.testerMenu.get();
    }

    public static void handle(ServerPlayer player, String action, String arg) {
        if (!allowed(player)) {
            player.sendSystemMessage(Component.translatable("message.planeshift.tester.denied"));
            return;
        }
        // Cheap actions, but still rate limited: every one of them is reachable from a packet, and
        // "load a course" in particular rebuilds an entire corridor.
        if (!PayloadRateLimiter.allow(player, PayloadRateLimiter.Action.LOAD_COURSE)
                && action.equals(TesterActions.COURSE)) {
            return;
        }

        switch (action) {
            case TesterActions.GIVE -> give(player, arg);
            case TesterActions.SPAWN -> spawn(player, arg);
            case TesterActions.COURSE -> CourseService.loadCourse(player, arg);
            case TesterActions.CLOCK -> setClock(player, arg);
            case TesterActions.SCORE -> addScore(player, arg);
            case TesterActions.LIVES -> setLives(player, arg);
            case TesterActions.HEAL -> CourseStateAccess.update(player,
                    s -> s.withPips(CourseState.MAX_PIPS, 0L));
            case TesterActions.KILL -> DamageService.down(player, player.damageSources().fellOutOfWorld());
            case TesterActions.COMPLETE -> CourseCompletionService.onComplete(player);
            case TesterActions.LEAVE -> CourseService.leaveCourse(player);
            case TesterActions.UNLOCK_ALL -> unlockAll(player);
            case TesterActions.RESET_PROGRESS -> ProgressionService.update(player, p -> CourseProgress.DEFAULT);
            case TesterActions.AUTOSCROLL -> CourseStateAccess.update(player,
                    s -> s.withCourseRules(s.timeLeft(), !s.autoScroll()));
            default -> PlaneShift.LOGGER.debug("Unknown tester action from {}: {}",
                    player.getName().getString(), action);
        }
    }

    private static void give(ServerPlayer player, String key) {
        Supplier<? extends Item> item = GRANTS.get(key);
        if (item == null) {
            return;
        }
        // Dropped at the player rather than placed in the inventory: PlaneShift power-ups apply
        // through the pickup path, so handing one straight to the inventory would skip the Form
        // grant, the sound and the particles — exactly the things a tester wants to see.
        player.drop(new ItemStack(item.get()), false, false);
    }

    private static void spawn(ServerPlayer player, String key) {
        Supplier<? extends EntityType<?>> type = SPAWNS.get(key);
        if (type == null || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        Entity entity = type.get().create(level, EntitySpawnReason.COMMAND);
        if (entity == null) {
            return;
        }
        // Placed along the rail in front of the player, at their own depth, so a spawned enemy in
        // a 2.5D course lands on the lane instead of behind the camera plane.
        double facing = Math.signum(Math.cos(Math.toRadians(player.getYRot() + 90.0F)));
        entity.snapTo(player.getX() + (facing == 0.0D ? SPAWN_DISTANCE : facing * SPAWN_DISTANCE),
                player.getY(), player.getZ(), player.getYRot(), 0.0F);
        level.addFreshEntity(entity);
    }

    private static void setClock(ServerPlayer player, String arg) {
        int seconds = parse(arg, 100);
        CourseStateAccess.update(player, s -> s.withTimeLeft(seconds <= 0
                ? 1
                : seconds * 20));
    }

    private static void addScore(ServerPlayer player, String arg) {
        CourseScoringService.addScore(player, parse(arg, 1000));
    }

    private static void setLives(ServerPlayer player, String arg) {
        int lives = parse(arg, 3);
        CourseStateAccess.update(player, s -> s.withLives(lives));
    }

    /**
     * Clears every course in every world.
     *
     * <p>Marked cleared with a zero score rather than a fabricated one, so unlocking for testing
     * never invents a record that looks like a real run.
     */
    private static void unlockAll(ServerPlayer player) {
        ProgressionService.update(player, progress -> {
            CourseProgress updated = progress;
            for (WorldDefinition world : WorldRegistry.allWorlds()) {
                for (String courseId : world.courseIds()) {
                    updated = updated.withClear(courseId, 0, 0);
                }
            }
            return updated;
        });
        player.sendSystemMessage(Component.translatable("message.planeshift.tester.unlocked",
                WorldRegistry.totalCourses()));
    }

    private static int parse(String arg, int fallback) {
        try {
            return Integer.parseInt(arg.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

}
