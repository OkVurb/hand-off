package com.studio.planeshift.common.form;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * A data-defined power Form (Design Bible, "Power-up framework").
 *
 * <p>"A Form is a data-defined gameplay capability with a clear lifecycle, visuals,
 * sounds, networking rules, and fallback. It is not a hard-coded costume branch."
 *
 * <p>Loaded from {@code data/<ns>/planeshift/form/*.json} as a synced datapack registry.
 * Balance budget: every Form has one combat role, one traversal/puzzle role, and one
 * explicit weakness or limit — the limits here are the codec-enforced part.
 *
 * @param category        catalog category
 * @param action          the trusted behaviour this Form binds to
 * @param maxCharges      action charges granted on pickup (bounded)
 * @param cooldownTicks   per-action cooldown (bounded to keep actions readable)
 * @param actionPower     action-specific magnitude (bolt speed, dash impulse, pull radius)
 * @param reserveEligible whether this Form may sit in the reserve slot
 * @param accentColor     material motif accent (ARGB); never the only signal
 */
public record FormDefinition(
        FormCategory category,
        FormActionKind action,
        int maxCharges,
        int cooldownTicks,
        float actionPower,
        boolean reserveEligible,
        int accentColor
) {
    public static final Codec<FormDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            FormCategory.CODEC.fieldOf("category").forGetter(FormDefinition::category),
            FormActionKind.CODEC.fieldOf("action").forGetter(FormDefinition::action),
            Codec.intRange(1, 64).optionalFieldOf("max_charges", 1).forGetter(FormDefinition::maxCharges),
            Codec.intRange(0, 1200).optionalFieldOf("cooldown_ticks", 20).forGetter(FormDefinition::cooldownTicks),
            Codec.floatRange(0.0F, 16.0F).optionalFieldOf("action_power", 1.0F).forGetter(FormDefinition::actionPower),
            Codec.BOOL.optionalFieldOf("reserve_eligible", true).forGetter(FormDefinition::reserveEligible),
            Codec.INT.optionalFieldOf("accent_color", 0xFFFFFFFF).forGetter(FormDefinition::accentColor)
    ).apply(instance, FormDefinition::new));
}
