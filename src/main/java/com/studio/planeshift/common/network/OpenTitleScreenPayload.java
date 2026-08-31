package com.studio.planeshift.common.network;

import com.studio.planeshift.PlaneShift;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Tells the client to open the PlaneShift title screen.
 */
public record OpenTitleScreenPayload() implements CustomPacketPayload {

    public static final Type<OpenTitleScreenPayload> TYPE = new Type<>(PlaneShift.id("open_title_screen"));

    public static final OpenTitleScreenPayload INSTANCE = new OpenTitleScreenPayload();

    public static final StreamCodec<ByteBuf, OpenTitleScreenPayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
