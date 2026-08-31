package com.studio.planeshift.common.network;

import com.studio.planeshift.PlaneShift;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * S2C: open the Toad shop screen.
 */
public record OpenToadShopPayload() implements CustomPacketPayload {

    public static final Type<OpenToadShopPayload> TYPE = new Type<>(PlaneShift.id("open_toad_shop"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenToadShopPayload> STREAM_CODEC =
            StreamCodec.<RegistryFriendlyByteBuf, OpenToadShopPayload>of(
                    (buf, payload) -> { },
                    buf -> new OpenToadShopPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
