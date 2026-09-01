package com.studio.planeshift.common.network;

import com.studio.planeshift.PlaneShift;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * S2C: the run is over.
 *
 * <p>Carries the course id so the screen can offer a retry that re-enters the course the player
 * just lost, rather than making them find it again on the map. The score travels with it for the
 * same reason the results payload carries one: the state that held it has already been reset by
 * the time the screen opens.
 */
public record GameOverPayload(String courseId, int score) implements CustomPacketPayload {

    public static final Type<GameOverPayload> TYPE = new Type<>(PlaneShift.id("game_over"));

    public static final StreamCodec<ByteBuf, GameOverPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, GameOverPayload::courseId,
                    ByteBufCodecs.VAR_INT, GameOverPayload::score,
                    GameOverPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
