package com.studio.planeshift.common.network;

import com.studio.planeshift.PlaneShift;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * C2S: abandon the current course and return to the hub.
 *
 * <p>Exists so the pause menu can offer a way out that is not "Save and Quit to Title". Quitting
 * to the title unloads the world; this leaves the course the way finishing it does, so the clock,
 * the auto-scroll rule and the course movement baseline are all cleared properly.
 */
public record LeaveCoursePayload() implements CustomPacketPayload {

    public static final LeaveCoursePayload INSTANCE = new LeaveCoursePayload();
    public static final Type<LeaveCoursePayload> TYPE = new Type<>(PlaneShift.id("leave_course"));

    public static final StreamCodec<io.netty.buffer.ByteBuf, LeaveCoursePayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
