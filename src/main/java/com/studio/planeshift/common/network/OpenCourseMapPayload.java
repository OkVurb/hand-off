package com.studio.planeshift.common.network;

import com.studio.planeshift.PlaneShift;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** S2C: the server wants the client to open the course map. */
public record OpenCourseMapPayload() implements CustomPacketPayload {

    public static final OpenCourseMapPayload INSTANCE = new OpenCourseMapPayload();
    public static final Type<OpenCourseMapPayload> TYPE = new Type<>(PlaneShift.id("open_course_map"));

    public static final StreamCodec<io.netty.buffer.ByteBuf, OpenCourseMapPayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
