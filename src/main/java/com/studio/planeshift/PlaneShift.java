package com.studio.planeshift;

import com.mojang.logging.LogUtils;
import com.studio.planeshift.common.PlaneShiftConfig;
import com.studio.planeshift.common.network.ModNetworking;
import com.studio.planeshift.common.registry.ModAttachments;
import com.studio.planeshift.common.registry.ModAttributes;
import com.studio.planeshift.common.registry.ModBlocks;
import com.studio.planeshift.common.registry.ModCreativeTabs;
import com.studio.planeshift.common.registry.ModEffects;
import com.studio.planeshift.common.registry.ModEntities;
import com.studio.planeshift.common.registry.ModItems;
import com.studio.planeshift.common.registry.ModParticles;
import com.studio.planeshift.common.registry.ModRegistries;
import com.studio.planeshift.common.registry.ModSounds;
import com.studio.planeshift.server.PlaneShiftCommands;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/**
 * PlaneShift entry point.
 *
 * <p>Architecture contract (Design Bible, "Technical architecture"):
 * <ul>
 *   <li>{@code common} owns mode rules, Form definitions, role stats and codecs.
 *       It must never reference client classes or render state.</li>
 *   <li>{@code server} owns validation, AI, damage, saves, checkpoints and rewards.
 *       It must never own camera transforms.</li>
 *   <li>{@code client} owns camera, keybinds, HUD, animation, particles and sound mix.
 *       It is never the final gameplay authority.</li>
 * </ul>
 * The {@code checkClientClassLeak} Gradle task enforces the first two rules in CI.
 */
@Mod(PlaneShift.MOD_ID)
public final class PlaneShift {

    public static final String MOD_ID = "planeshift";
    public static final Logger LOGGER = LogUtils.getLogger();

    public PlaneShift(IEventBus modBus, ModContainer container) {
        LOGGER.info("PlaneShift bootstrapping (common)");
        ModAttachments.ATTACHMENTS.register(modBus);
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModEntities.ENTITY_TYPES.register(modBus);
        ModCreativeTabs.TABS.register(modBus);
        ModAttributes.ATTRIBUTES.register(modBus);
        ModEffects.EFFECTS.register(modBus);
        ModSounds.SOUNDS.register(modBus);
        ModParticles.PARTICLES.register(modBus);

        modBus.addListener(ModRegistries::onNewDataPackRegistries);
        modBus.addListener(ModEntities::onCreateAttributes);
        modBus.addListener(ModAttributes::onModifyAttributes);
        modBus.addListener(ModNetworking::onRegisterPayloadHandlers);
        modBus.addListener(com.studio.planeshift.server.test.PlaneShiftGameTests::registerFunctions);
        modBus.addListener(com.studio.planeshift.server.test.PlaneShiftGameTests::onRegisterGameTests);

        NeoForge.EVENT_BUS.addListener(PlaneShiftCommands::onRegisterCommands);

        container.registerConfig(ModConfig.Type.CLIENT, PlaneShiftConfig.CLIENT_SPEC);
        container.registerConfig(ModConfig.Type.SERVER, PlaneShiftConfig.SERVER_SPEC);
    }

    /** All PlaneShift resources use lowercase snake_case identifiers in the mod namespace. */
    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
