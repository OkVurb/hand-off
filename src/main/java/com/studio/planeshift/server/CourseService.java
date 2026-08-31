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

    private static final int COURSE_SPACING = 256;
    private static final double RAIL_HALF_DEPTH = 0.75D;

    private CourseService() {
    }

    public static boolean loadCourse(ServerPlayer player, String courseId) {
        MinecraftServer server = player.level().getServer();
        if (server == null) {
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

        player.teleportTo(courseLevel, originX, startY, originZ,
                Collections.emptySet(), 0.0F, 0.0F, false);

        CourseStructureService.place(courseLevel, def);

        PlaneMode mode = def.startMode();
        Optional<PlaneRail> rail = mode == PlaneMode.SIDE_ON
                ? Optional.of(new PlaneRail(net.minecraft.core.Direction.Axis.X, start.getZ(), RAIL_HALF_DEPTH, true))
                : Optional.empty();
        CourseThemeService.set(player, def.theme());
        CourseStateAccess.update(player, s -> new CourseState(
                mode == PlaneMode.SIDE_ON ? PlayState.PLAYING_2_5D : PlayState.PLAYING_3D,
                mode,
                rail,
                s.roleId(),
                s.formSlot(),
                Optional.empty(),
                CourseState.MAX_PIPS,
                0L,
                Optional.of(net.minecraft.core.GlobalPos.of(courseLevel.dimension(), start)),
                s.coins(),
                s.starCoins(),
                s.lives(),
                def.killY()
        ));
        CourseScoringService.startCourse(player);
        return true;
    }

    public static void returnToHub(ServerPlayer player) {
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
