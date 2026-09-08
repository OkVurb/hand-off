package com.studio.planeshift.common.network;

import com.studio.planeshift.PlaneShift;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * S2C: hold black bars across the top and bottom of the screen for a while.
 *
 * <p>The one piece of interface this mod draws that is allowed to say "stop playing". Everything
 * else — the title card, the iris, the credits — deliberately keeps the player in control, and the
 * letterbox is the exception that makes those readable as *not* cutscenes.
 *
 * @param ticks how long the bars stay down, or zero to raise them early
 */
public record LetterboxPayload(int ticks) implements CustomPacketPayload {

    public static final Type<LetterboxPayload> TYPE = new Type<>(PlaneShift.id("letterbox"));

    public static final StreamCodec<ByteBuf, LetterboxPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, LetterboxPayload::ticks,
                    LetterboxPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
