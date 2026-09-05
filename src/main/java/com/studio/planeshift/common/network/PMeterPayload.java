package com.studio.planeshift.common.network;

import com.studio.planeshift.PlaneShift;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * S2C: the player's P-meter, as a 0..{@code PMeter.STEPS} display value.
 *
 * <p>Its own packet rather than a field on {@code CourseState}, and the reason is not packet size.
 * {@code CourseState} is registered with {@code .serialize(CourseState.CODEC)} — it is written to
 * disk with the player. A run-speed gauge is transient by definition; persisting it would mean
 * logging out at full speed and logging back in still holding it, which is both wrong and the kind
 * of wrong nobody would ever think to look for. State that should not survive a logout does not
 * belong in the record that is saved.
 *
 * <p>Sent only when the quantised step changes, which for a player running flat out is roughly
 * once every twelve ticks rather than twenty times a second.
 *
 * <p>Purely presentational: the speed bonus itself is an attribute modifier applied server-side,
 * and nothing about the game state depends on this packet arriving.
 *
 * @param step 0 for empty, {@code PMeter.STEPS} for full
 */
public record PMeterPayload(int step) implements CustomPacketPayload {

    public static final Type<PMeterPayload> TYPE = new Type<>(PlaneShift.id("p_meter"));

    public static final StreamCodec<ByteBuf, PMeterPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, PMeterPayload::step,
                    PMeterPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
