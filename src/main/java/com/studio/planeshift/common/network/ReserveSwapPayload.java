package com.studio.planeshift.common.network;

import com.studio.planeshift.PlaneShift;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** C2S: "swap my active and reserve Forms." Carries no data; the server owns the rest. */
public record ReserveSwapPayload() implements CustomPacketPayload {

    public static final ReserveSwapPayload INSTANCE = new ReserveSwapPayload();
    public static final Type<ReserveSwapPayload> TYPE = new Type<>(PlaneShift.id("reserve_swap"));

    public static final StreamCodec<io.netty.buffer.ByteBuf, ReserveSwapPayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
