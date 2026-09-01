package com.studio.planeshift.server;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.studio.planeshift.PlaneShift;
import com.studio.planeshift.common.course.CourseTheme;
import com.studio.planeshift.common.network.OpenCourseMapPayload;
import com.studio.planeshift.common.network.OpenTesterPayload;
import com.studio.planeshift.common.network.OpenTitleScreenPayload;
import com.studio.planeshift.common.course.CourseState;
import com.studio.planeshift.common.mode.PlaneMode;
import com.studio.planeshift.common.mode.PlayState;
import com.studio.planeshift.common.registry.ModRegistries;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * {@code /planeshift} commands.
 *
 * <p>Player-facing: role selection (hub only) and leaving a course. Operator-facing:
 * debug mode shifts, form grants, checkpoints and coins. Debug commands stand in for
 * the hub wardrobe/portal UI until that content lands.
 */
public final class PlaneShiftCommands {

    private PlaneShiftCommands() {
    }

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("planeshift")
                .requires(CommandSourceStack::isPlayer)
                .then(Commands.literal("role")
                        .then(Commands.argument("id", IdentifierArgument.id())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(
                                        context.getSource().registryAccess()
                                                .lookupOrThrow(ModRegistries.ROLE)
                                                .listElements().map(holder -> holder.key().identifier()),
                                        builder))
                                .executes(context -> selectRole(context.getSource(),
                                        IdentifierArgument.getId(context, "id")))))
                .then(Commands.literal("leave")
                        .executes(context -> leaveCourse(context.getSource())))
                .then(Commands.literal("mode")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("side_on")
                                .executes(context -> debugShift(context.getSource(), PlaneMode.SIDE_ON)))
                        .then(Commands.literal("free_3d")
                                .executes(context -> debugShift(context.getSource(), PlaneMode.FREE_3D))))
                .then(Commands.literal("form")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("id", IdentifierArgument.id())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(
                                        context.getSource().registryAccess()
                                                .lookupOrThrow(ModRegistries.FORM)
                                                .listElements().map(holder -> holder.key().identifier()),
                                        builder))
                                .executes(context -> grantForm(context.getSource(),
                                        IdentifierArgument.getId(context, "id")))))
                .then(Commands.literal("checkpoint")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(context -> setCheckpoint(context.getSource())))
                .then(Commands.literal("course")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("id", StringArgumentType.word())
                                .executes(context -> startCourse(context.getSource(),
                                        StringArgumentType.getString(context, "id")))))
                .then(Commands.literal("map")
                        .executes(context -> openCourseMap(context.getSource())))
                .then(Commands.literal("test")
                        .executes(context -> openTester(context.getSource())))
                .then(Commands.literal("title")
                        .executes(context -> openTitleScreen(context.getSource())))
                .then(Commands.literal("theme")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests((context, builder) ->
                                        net.minecraft.commands.SharedSuggestionProvider.suggest(
                                                java.util.Arrays.stream(CourseTheme.values())
                                                        .map(CourseTheme::getSerializedName), builder))
                                .executes(context -> setTheme(context.getSource(),
                                        StringArgumentType.getString(context, "name")))))
                .then(Commands.literal("coins")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("amount", IntegerArgumentType.integer(0, 10_000))
                                .executes(context -> addCoins(context.getSource(),
                                        IntegerArgumentType.getInteger(context, "amount"))))));
    }

    private static int selectRole(CommandSourceStack source, Identifier roleId) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return 0;
        }
        CourseState state = CourseStateAccess.get(player);
        // "Server owns role selection; changes occur in the hub, at course start."
        if (state.inCourse() && !source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
            source.sendFailure(Component.translatable("commands.planeshift.role.in_course"));
            return 0;
        }
        if (RoleService.select(player, roleId)) {
            source.sendSuccess(() -> Component.translatable("commands.planeshift.role.selected",
                    Component.translatable("role." + roleId.getNamespace() + "." + roleId.getPath())), false);
            return 1;
        }
        source.sendFailure(Component.translatable("commands.planeshift.role.unknown", roleId.toString()));
        return 0;
    }

    private static int leaveCourse(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return 0;
        }
        ModeTransitionService.abortIfActive(player);
        CourseService.returnToHub(player);
        CourseStateAccess.update(player, s -> s
                .withTransition(Optional.empty())
                .withMode(PlaneMode.FREE_3D, Optional.empty())
                .withState(PlayState.HUB));
        source.sendSuccess(() -> Component.translatable("commands.planeshift.leave"), false);
        return 1;
    }

    private static int debugShift(CommandSourceStack source, PlaneMode target) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return 0;
        }
        ModeTransitionService.requestManualShift(player, target);
        source.sendSuccess(() -> Component.translatable("commands.planeshift.mode.requested",
                target.getSerializedName()), true);
        return 1;
    }

    private static int grantForm(CommandSourceStack source, Identifier formId) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return 0;
        }
        if (FormService.grant(player, formId)) {
            source.sendSuccess(() -> Component.translatable("commands.planeshift.form.granted",
                    formId.toString()), true);
            return 1;
        }
        source.sendFailure(Component.translatable("commands.planeshift.form.unknown", formId.toString()));
        return 0;
    }

    private static int setCheckpoint(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return 0;
        }
        CourseStateAccess.update(player, s -> s.withCheckpoint(
                Optional.of(GlobalPos.of(player.level().dimension(), player.blockPosition()))));
        source.sendSuccess(() -> Component.translatable("commands.planeshift.checkpoint.set"), true);
        return 1;
    }

    private static int openTitleScreen(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return 0;
        }
        PacketDistributor.sendToPlayer(player, OpenTitleScreenPayload.INSTANCE);
        source.sendSuccess(() -> Component.translatable("commands.planeshift.title.opened"), false);
        return 1;
    }

    private static int openCourseMap(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return 0;
        }
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, OpenCourseMapPayload.INSTANCE);
        source.sendSuccess(() -> Component.translatable("commands.planeshift.map.opened"), false);
        return 1;
    }

    /** Opens the tester menu without needing the F6 binding to be free. */
    private static int openTester(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            return 0;
        }
        PacketDistributor.sendToPlayer(player, OpenTesterPayload.INSTANCE);
        return 1;
    }

    private static int startCourse(CommandSourceStack source, String courseId) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return 0;
        }
        if (CourseService.loadCourse(player, courseId)) {
            source.sendSuccess(() -> Component.translatable("commands.planeshift.course.started", courseId), true);
            return 1;
        }
        source.sendFailure(Component.translatable("commands.planeshift.course.unknown", courseId));
        return 0;
    }

    private static int setTheme(CommandSourceStack source, String name) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return 0;
        }
        try {
            CourseTheme theme = CourseTheme.valueOf(name.toUpperCase(java.util.Locale.ROOT));
            CourseThemeService.set(player, theme);
            source.sendSuccess(() -> Component.translatable("commands.planeshift.theme.set", theme.getSerializedName()), true);
            return 1;
        } catch (IllegalArgumentException e) {
            source.sendFailure(Component.translatable("commands.planeshift.theme.unknown", name));
            return 0;
        }
    }

    private static int addCoins(CommandSourceStack source, int amount) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return 0;
        }
        long total = (long) CourseStateAccess.get(player).coins() + amount;
        CourseStateAccess.update(player, s -> s.withCoins((int) Math.min(total, (long) CourseState.MAX_VALUE)));
        source.sendSuccess(() -> Component.translatable("commands.planeshift.coins.added", amount), true);
        return 1;
    }
}
