package com.studio.planeshift.common.network;

import com.studio.planeshift.PlaneShift;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * C2S: a request from the tester menu.
 *
 * <p>One payload for every tester action rather than one payload per action, because the set is
 * expected to churn constantly during playtesting and a wire format that has to be extended for
 * every new button would be extended badly. The action name is validated server-side against a
 * fixed table — an unknown name does nothing.
 *
 * @param action what to do, e.g. {@code give} or {@code clock}
 * @param arg    the action's parameter, empty when it takes none
 */
public record TesterActionPayload(String action, String arg) implements CustomPacketPayload {

    public static final Type<TesterActionPayload> TYPE = new Type<>(PlaneShift.id("tester_action"));

    /**
     * Both strings are length-capped. This is a client-controlled payload, so the codec is the
     * first place an oversized string can be refused rather than allocated.
     */
    public static final StreamCodec<ByteBuf, TesterActionPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.stringUtf8(32), TesterActionPayload::action,
                    ByteBufCodecs.stringUtf8(64), TesterActionPayload::arg,
                    TesterActionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
