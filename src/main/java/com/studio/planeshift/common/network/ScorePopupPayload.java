package com.studio.planeshift.common.network;

import com.studio.planeshift.PlaneShift;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * S2C: show a floating score number in the world.
 *
 * <p>Sent when a stomp scores, so the player can read the combo ladder climbing without watching
 * the HUD counter. The value is the amount awarded, or zero when the chain paid out a 1-Up
 * instead — the client renders those differently because "1-UP" is the more important message.
 *
 * <p>Purely presentational. The score itself already lives in {@code CourseState} and is synced
 * with the rest of it; nothing about the game state depends on this packet arriving.
 *
 * @param x      world position to float above
 * @param y      world position to float above
 * @param z      world position to float above
 * @param amount points awarded, or 0 for a 1-Up
 */
public record ScorePopupPayload(double x, double y, double z, int amount) implements CustomPacketPayload {

    public static final Type<ScorePopupPayload> TYPE = new Type<>(PlaneShift.id("score_popup"));

    public static final StreamCodec<ByteBuf, ScorePopupPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public ScorePopupPayload decode(ByteBuf buf) {
            return new ScorePopupPayload(
                    ByteBufCodecs.DOUBLE.decode(buf),
                    ByteBufCodecs.DOUBLE.decode(buf),
                    ByteBufCodecs.DOUBLE.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf));
        }

        @Override
        public void encode(ByteBuf buf, ScorePopupPayload payload) {
            ByteBufCodecs.DOUBLE.encode(buf, payload.x());
            ByteBufCodecs.DOUBLE.encode(buf, payload.y());
            ByteBufCodecs.DOUBLE.encode(buf, payload.z());
            ByteBufCodecs.VAR_INT.encode(buf, payload.amount());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
