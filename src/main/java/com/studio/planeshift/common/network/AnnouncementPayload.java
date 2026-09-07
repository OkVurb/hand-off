package com.studio.planeshift.common.network;

import com.studio.planeshift.PlaneShift;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * S2C: a structural moment worth marking.
 *
 * <p>Sent when something changes about the run as a whole rather than about the course just
 * played -- the last course in the game being cleared, for now. The results screen holds it until
 * it draws, so the banner lands on the screen the player is already reading rather than flashing
 * over the flagpole.
 *
 * <p>A separate packet rather than another field on {@code CourseResultsPayload}, and that is a
 * constraint rather than a preference: that payload already uses all eight slots
 * {@code StreamCodec.composite} provides, and a ninth field would mean hand-writing the codec --
 * two parallel lists of fields that drift silently the first time somebody adds a tenth.
 *
 * @param key translation key for the banner text
 */
public record AnnouncementPayload(String key) implements CustomPacketPayload {

    public static final Type<AnnouncementPayload> TYPE = new Type<>(PlaneShift.id("announcement"));

    public static final StreamCodec<ByteBuf, AnnouncementPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, AnnouncementPayload::key,
                    AnnouncementPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
