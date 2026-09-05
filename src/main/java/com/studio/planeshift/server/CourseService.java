package com.studio.planeshift.server;

import com.studio.planeshift.PlaneShift;
import com.studio.planeshift.common.course.CourseDefinition;
import com.studio.planeshift.common.course.CourseState;
import com.studio.planeshift.common.course.CourseTheme;
import com.studio.planeshift.common.mode.PlaneMode;
import com.studio.planeshift.common.mode.PlaneRail;
import com.studio.planeshift.common.mode.PlayState;
import com.studio.planeshift.common.registry.ModRegistries;
import java.util.Collections;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.portal.TeleportTransition;

/**
 * Course loading and teleportation (Design Bible, "World map").
 *
 * <p>Vertical-slice: hard-coded region offsets for a few course ids. Later this will
 * load structure templates or procedural generators from datapack definitions.
 */
public final class CourseService {

    private static final double RAIL_HALF_DEPTH = 0.75D;

    private CourseService() {
    }

    public static boolean loadCourse(ServerPlayer player, String courseId) {
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return false;
        }
        // Reached from a C2S payload: resolving a course and teleporting across dimensions is
        // far too expensive to run at packet rate.
        if (!PayloadRateLimiter.allow(player, PayloadRateLimiter.Action.LOAD_COURSE)) {
            return false;
        }

        Identifier id = Identifier.tryBuild(PlaneShift.MOD_ID, courseId);
        if (id == null) {
            PlaneShift.LOGGER.warn("Invalid course id from {}: {}", player.getName().getString(), courseId);
            return false;
        }

        CourseDefinition def = resolveCourse(player, id);
        if (def == null) {
            PlaneShift.LOGGER.warn("Unknown course id for {}: {}", player.getName().getString(), courseId);
            return false;
        }

        // Gating is enforced here rather than only in the map screen. The screen greys locked
        // courses out because that is honest UI; this is what makes it true.
        if (!ProgressionService.isUnlocked(player, courseId)) {
            player.sendSystemMessage(net.minecraft.network.chat.Component
                    .translatable("message.planeshift.course_locked"));
            return false;
        }

        ServerLevel courseLevel = server.getLevel(def.dimensionKey());
        if (courseLevel == null) {
            PlaneShift.LOGGER.error("Course dimension not found: {}", def.dimensionKey());
            return false;
        }

        ModeTransitionService.abortIfActive(player);

        BlockPos start = def.startPos();
        double originX = start.getX() + 0.5;
        double startY = start.getY();
        double originZ = start.getZ() + 0.5;

        // Build/reset the lane before moving the player. The original vertical slice spawned at
        // y=64 in an otherwise empty dimension whose flat terrain is down near minY, so a player
        // could fall through the kill plane before ever touching a course block.
        CourseStructureService.place(courseLevel, def, courseId);

        player.teleportTo(courseLevel, originX, startY, originZ,
                Collections.emptySet(), 0.0F, 0.0F, false);

        PlaneMode mode = def.startMode();
        Optional<PlaneRail> rail = mode == PlaneMode.SIDE_ON
                // BlockPos identifies a block corner; the playable lane runs through its centre.
                ? Optional.of(new PlaneRail(net.minecraft.core.Direction.Axis.X, originZ, RAIL_HALF_DEPTH, true))
                : Optional.empty();
        CourseThemeService.set(player, def.theme());
        CourseStateAccess.update(player, s -> new CourseState(
                mode == PlaneMode.SIDE_ON ? PlayState.PLAYING_2_5D : PlayState.PLAYING_3D,
                mode,
                rail,
                s.roleId(),
                s.formSlot(),
                Optional.empty(),
                Math.max(1, s.pips()),
                0L,
                Optional.of(net.minecraft.core.GlobalPos.of(courseLevel.dimension(), start)),
                s.coins(),
                s.starCoins(),
                s.lives(),
                def.killY(),
                // Score is zeroed by startCourse below; the clock and auto-scroll come from the
                // course definition so each course can set its own rules.
                0,
                def.timeLimitTicks(),
                def.autoScroll(),
                def.theme()
        ));
        // The course movement baseline has to follow the state change, not precede it: it reads
        // inCourse() to decide whether to apply.
        CourseMovementService.refresh(player);
        CourseScoringService.startCourse(player);
        ProgressionService.enterCourse(player, courseId);
        // A player who has moved on should not still be being thanked for the last course.
        ToadDialogueService.clear(player);
        return true;
    }

    /**
     * Abandons the course from the pause menu.
     *
     * <p>Distinct from {@link #returnToHub} because leaving is a decision with consequences: the
     * run is over, so the score is dropped and the progress record is closed out. Nothing is
     * recorded as cleared — walking out of a course is not finishing it.
     */
    public static void leaveCourse(ServerPlayer player) {
        if (!CourseStateAccess.get(player).inCourse()) {
            return;
        }
        CourseScoringService.clear(player.getUUID());
        MovementRuleService.clearMeter(player);
        ProgressionService.leaveCourse(player);
        ToadDialogueService.clear(player);
        returnToHub(player);
        CourseStateAccess.update(player, s -> s
                .withState(PlayState.HUB)
                .withMode(PlaneMode.FREE_3D, Optional.empty())
                .withScore(0));
    }

    public static void returnToHub(ServerPlayer player) {
        // Drop the course movement baseline; the hub is ordinary Minecraft, and leaving a player
        // in the hub with course jump height makes the hub feel broken instead of the course
        // feeling special.
        CourseMovementService.clear(player);
        // Drop the course rules first: the hub has no clock and never auto-scrolls, and a stale
        // countdown would keep ticking the player toward a death they cannot see coming.
        CourseTimerService.clear(player);
        TeleportTransition transition = player.findRespawnPositionAndUseSpawnBlock(false, TeleportTransition.DO_NOTHING);
        player.teleport(transition);
    }

    private static CourseDefinition resolveCourse(ServerPlayer player, Identifier id) {
        Optional<Registry<CourseDefinition>> registry = player.registryAccess().lookup(ModRegistries.COURSE);
        ResourceKey<CourseDefinition> key = ResourceKey.create(ModRegistries.COURSE, id);
        if (registry.isPresent()) {
            var holder = registry.get().get(key);
            if (holder.isPresent()) {
                return holder.get().value();
            }
        }
        return null;
    }
}

