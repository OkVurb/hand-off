package com.studio.planeshift.common.role;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * A playable role (Design Bible, "Playable role system").
 *
 * <p>Role contract: "Differences stay near 10-15% and never change hitbox size, max
 * health, interaction reach, or required objective access." The codec enforces those
 * bounds so a datapack cannot create a mandatory character.
 *
 * <p>Loaded from {@code data/<ns>/planeshift/role/*.json} as a synced datapack registry.
 *
 * @param runMultiplier      ground speed multiplier, bounded to the 15% contract
 * @param jumpMultiplier     jump strength multiplier, bounded to the 15% contract
 * @param tractionMultiplier acceleration/friction feel multiplier (advisory for tuning)
 * @param signature          behavioural trait hook
 * @param floatTicks         FLOAT_GLIDE only: max hold-jump float duration in ticks
 * @param accentColor        role accent (ARGB) used by HUD and effects; shape-first rule
 *                           means colour is never the only signal
 */
public record PlayerRole(
        float runMultiplier,
        float jumpMultiplier,
        float tractionMultiplier,
        RoleSignature signature,
        int floatTicks,
        int accentColor
) {
    /** The bible's 1.25 s float for the Glider, at 20 TPS. */
    public static final int DEFAULT_FLOAT_TICKS = 25;

    public static final Codec<PlayerRole> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.floatRange(0.85F, 1.15F).optionalFieldOf("run_multiplier", 1.0F)
                    .forGetter(PlayerRole::runMultiplier),
            Codec.floatRange(0.85F, 1.15F).optionalFieldOf("jump_multiplier", 1.0F)
                    .forGetter(PlayerRole::jumpMultiplier),
            Codec.floatRange(0.7F, 1.15F).optionalFieldOf("traction_multiplier", 1.0F)
                    .forGetter(PlayerRole::tractionMultiplier),
            RoleSignature.CODEC.fieldOf("signature").forGetter(PlayerRole::signature),
            Codec.intRange(0, 60).optionalFieldOf("float_ticks", 0).forGetter(PlayerRole::floatTicks),
            Codec.INT.optionalFieldOf("accent_color", 0xFFFFFFFF).forGetter(PlayerRole::accentColor)
    ).apply(instance, PlayerRole::new));
}
