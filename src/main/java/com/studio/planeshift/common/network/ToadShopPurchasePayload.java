package com.studio.planeshift.common.network;

import com.studio.planeshift.PlaneShift;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * C2S: buy an item from the Toad shop by slot index.
 */
public record ToadShopPurchasePayload(int slot) implements CustomPacketPayload {

    public static final Type<ToadShopPurchasePayload> TYPE = new Type<>(PlaneShift.id("toad_shop_purchase"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ToadShopPurchasePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, ToadShopPurchasePayload::slot,
                    ToadShopPurchasePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
