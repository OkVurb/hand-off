package com.studio.planeshift.common.registry;

import com.studio.planeshift.PlaneShift;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * PlaneShift's own entity attributes.
 *
 * <p>These exist so course tuning is data-driven and stackable rather than hard-coded. An
 * attribute can be modified by a Form, a role, a potion effect or a command without any of those
 * systems knowing about each other, which a constant in a service class cannot do.
 */
public final class ModAttributes {

    public static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(Registries.ATTRIBUTE, PlaneShift.MOD_ID);

    /**
     * Upward velocity applied to the player by a successful stomp.
     *
     * <p>Default matches the hand-tuned constant this replaced. The ceiling is deliberately well
     * above anything the game grants so a Form or a boss fight can push it hard without the
     * attribute itself becoming the limit.
     */
    public static final DeferredHolder<Attribute, Attribute> BOUNCE_HEIGHT =
            ATTRIBUTES.register("bounce_height", () -> new RangedAttribute(
                    "attribute.planeshift.bounce_height", 0.55D, 0.0D, 4.0D).setSyncable(true));

    /**
     * Attaches PlaneShift attributes to the player.
     *
     * <p>Vanilla builds the player attribute map without knowing about mod attributes, so every
     * one has to be added here or { getAttribute} returns null at runtime.
     */
    public static void onModifyAttributes(EntityAttributeModificationEvent event) {
        event.add(EntityType.PLAYER, BOUNCE_HEIGHT);
    }

    private ModAttributes() {
    }
}
