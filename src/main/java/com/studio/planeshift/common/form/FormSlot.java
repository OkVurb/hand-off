package com.studio.planeshift.common.form;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

/**
 * A player's Form loadout: one active Form plus one reserve slot
 * (Design Bible, "Reserve inventory and balance rules").
 *
 * <p>"A single reserve slot creates strategic choice without turning the mod into an
 * inventory management game." Only the server grants, spends, replaces or removes a
 * Form; this record is the synced, persisted result.
 *
 * @param active        the active Form id, if any
 * @param charges       remaining action charges of the active Form
 * @param cooldownUntil game time until the active Form's action is ready again
 * @param reserve       the reserved Form id, if any
 */
public record FormSlot(
        Optional<Identifier> active,
        int charges,
        long cooldownUntil,
        Optional<Identifier> reserve
) {
    public static final FormSlot EMPTY = new FormSlot(Optional.empty(), 0, 0L, Optional.empty());

    public static final Codec<FormSlot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.optionalFieldOf("active").forGetter(FormSlot::active),
            Codec.intRange(0, 64).optionalFieldOf("charges", 0).forGetter(FormSlot::charges),
            Codec.LONG.optionalFieldOf("cooldown_until", 0L).forGetter(FormSlot::cooldownUntil),
            Identifier.CODEC.optionalFieldOf("reserve").forGetter(FormSlot::reserve)
    ).apply(instance, FormSlot::new));

    public static final StreamCodec<ByteBuf, FormSlot> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.optional(Identifier.STREAM_CODEC), FormSlot::active,
            ByteBufCodecs.VAR_INT, FormSlot::charges,
            ByteBufCodecs.VAR_LONG, FormSlot::cooldownUntil,
            ByteBufCodecs.optional(Identifier.STREAM_CODEC), FormSlot::reserve,
            FormSlot::new);

    public boolean hasActive() {
        return active.isPresent();
    }

    public boolean actionReady(long gameTime) {
        return hasActive() && charges > 0 && gameTime >= cooldownUntil;
    }

    public FormSlot withActive(Identifier form, int charges) {
        return new FormSlot(Optional.of(form), charges, 0L, reserve);
    }

    public FormSlot withCharges(int newCharges, long newCooldownUntil) {
        return new FormSlot(active, Math.max(0, newCharges), newCooldownUntil, reserve);
    }

    /** Damage rule: "a normal hit removes the Form first". Reserve survives by default. */
    public FormSlot loseActive() {
        return new FormSlot(Optional.empty(), 0, 0L, reserve);
    }

    public FormSlot withReserve(Optional<Identifier> newReserve) {
        return new FormSlot(active, charges, cooldownUntil, newReserve);
    }
}
