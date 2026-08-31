package com.studio.planeshift.common.network;

import com.studio.planeshift.PlaneShift;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * C2S: select a course from the world map.
 *
 * <p>Only the course id is sent; the server resolves the dimension, region and rail.
 */
public record CourseSelectPayload(String courseId) implements CustomPacketPayload {

    public static final Type<CourseSelectPayload> TYPE = new Type<>(PlaneShift.id("course_select"));

    public static final StreamCodec<ByteBuf, CourseSelectPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, CourseSelectPayload::courseId,
                    CourseSelectPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
