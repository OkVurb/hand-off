package com.studio.planeshift.common.network;

import com.studio.planeshift.PlaneShift;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * C2S: the player entered a non-course node on the world map — a Toad House or a cannon.
 *
 * <p>Separate from {@code CourseSelectPayload} because these are not courses and must not be
 * routed through course loading. The id is validated server-side against the world's own generated
 * map, so a handcrafted packet cannot invent a Toad House that pays out repeatedly.
 */
public record MapNodePayload(String nodeId) implements CustomPacketPayload {

    public static final Type<MapNodePayload> TYPE = new Type<>(PlaneShift.id("map_node"));

    public static final StreamCodec<ByteBuf, MapNodePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.stringUtf8(64), MapNodePayload::nodeId,
                    MapNodePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
