package com.studio.planeshift.server;

import com.studio.planeshift.common.course.CourseProgress;
import com.studio.planeshift.common.course.WorldDefinition;
import com.studio.planeshift.common.course.WorldMapLayout;
import com.studio.planeshift.common.course.WorldRegistry;
import com.studio.planeshift.common.registry.ModItems;
import com.studio.planeshift.common.registry.ModSounds;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Handles the map's non-course nodes: Toad Houses and cannons.
 *
 * <p>Validated against the generated map rather than trusting the id in the packet. A Toad House
 * pays out a power-up, so an unvalidated id is an item printer; the node has to actually exist in
 * a world the player has already reached.
 */
public final class MapNodeService {

    /** What a Toad House can hand out. The useful Forms, deliberately not the joke ones. */
    private static final List<Supplier<? extends Item>> GIFTS = List.of(
            ModItems.SUPER_MUSHROOM, ModItems.FIRE_FLOWER, ModItems.ICE_FLOWER,
            ModItems.LEAF, ModItems.PROPELLER_MUSHROOM, ModItems.CAT_SUIT,
            ModItems.TANOOKI, ModItems.THREE_UP);

    private MapNodeService() {
    }

    public static void enter(ServerPlayer player, String nodeId) {
        if (!PayloadRateLimiter.allow(player, PayloadRateLimiter.Action.LOAD_COURSE)) {
            return;
        }

        WorldMapLayout.Node node = findNode(player, nodeId);
        if (node == null) {
            return;
        }

        switch (node.type()) {
            case TOAD_HOUSE -> visitToadHouse(player, node);
            case CANNON -> fireCannon(player, node);
            default -> {
                // Courses arrive through CourseSelectPayload; nothing to do here.
            }
        }
    }

    /**
     * Finds the node in a world the player can actually reach.
     *
     * <p>Scans every world rather than trusting a world index from the client, and requires the
     * world itself to be unlocked — otherwise a packet naming world five's Toad House would pay
     * out during world one.
     */
    private static WorldMapLayout.Node findNode(ServerPlayer player, String nodeId) {
        CourseProgress progress = ProgressionService.get(player);
        for (WorldDefinition world : WorldRegistry.allWorlds()) {
            if (!WorldRegistry.isWorldUnlocked(progress, world)) {
                continue;
            }
            WorldMapLayout layout = WorldMapLayout.forWorld(world);
            for (WorldMapLayout.Node node : layout.nodes()) {
                if (node.id().equals(nodeId) && !node.isPlayable()) {
                    return node;
                }
            }
        }
        return null;
    }

    /**
     * Hands over a power-up.
     *
     * <p>Dropped at the player rather than inserted into the inventory, for the same reason the
     * tester menu does it: PlaneShift power-ups apply through the pickup path, so an item placed
     * directly into a slot would skip the Form grant, the sound and the particles.
     */
    private static void visitToadHouse(ServerPlayer player, WorldMapLayout.Node node) {
        int roll = player.getRandom().nextInt(GIFTS.size());
        player.drop(new ItemStack(GIFTS.get(roll).get()), false, false);
        player.level().playSound(null, player.blockPosition(), ModSounds.POWER_UP.get(),
                SoundSource.PLAYERS, 0.9F, 1.0F);
        player.sendSystemMessage(Component.translatable("message.planeshift.toad_house"));
    }

    /**
     * The cannon: skips to the first course of the next world, once that world is already open.
     *
     * <p>It refuses when the next world is locked rather than unlocking it. A shortcut that also
     * grants access is not a shortcut, it is a cheat code — it would let a player reach the last
     * world without clearing a single castle, and every unlock rule elsewhere would be decorative.
     */
    private static void fireCannon(ServerPlayer player, WorldMapLayout.Node node) {
        String worldId = node.id().substring("cannon_".length());
        int index = WorldRegistry.worldIndex(worldId);
        if (index < 0 || index + 1 >= WorldRegistry.worldCount()) {
            player.sendSystemMessage(Component.translatable("message.planeshift.cannon_end"));
            return;
        }
        WorldDefinition next = WorldRegistry.allWorlds().get(index + 1);
        if (!WorldRegistry.isWorldUnlocked(ProgressionService.get(player), next)) {
            player.sendSystemMessage(Component.translatable("message.planeshift.cannon_locked"));
            return;
        }
        player.level().playSound(null, player.blockPosition(), ModSounds.WARP.get(),
                SoundSource.PLAYERS, 1.0F, 0.8F);
        CourseService.loadCourse(player, next.courseIds().get(0));
    }
}
