package com.studio.planeshift.common.network;

import com.studio.planeshift.PlaneShift;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

/**
 * S2C: start the credits roll.
 *
 * <p>Carries nothing. The names are the same every time and live on the client, so sending them
 * would be sending a constant down a wire once per playthrough; what the server knows and the
 * client does not is simply <em>when</em>.
 */
public record CreditsPayload() implements CustomPacketPayload {

    public static final Type<CreditsPayload> TYPE = new Type<>(PlaneShift.id("credits"));

    public static final StreamCodec<ByteBuf, CreditsPayload> STREAM_CODEC =
            StreamCodec.unit(new CreditsPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
