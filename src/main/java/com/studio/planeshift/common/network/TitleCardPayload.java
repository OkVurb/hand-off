package com.studio.planeshift.common.network;

import com.studio.planeshift.PlaneShift;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * S2C: name the course the player has just entered.
 *
 * <p>The reference opens every level on a card naming the world and the level, held for a moment
 * over black before play starts. It is the one piece of interface that tells the player where they
 * are in a fifty-course run, and the mod dropped them into a level with no announcement at all.
 *
 * <p>Carries the two display strings rather than a course id, which settles a question the backlog
 * had been holding open. The alternative was to sync the id and have the client look up its name,
 * which means the client needs the world table, needs it to agree with the server's, and gets a
 * blank card the day they disagree. The server already knows both strings; sending them is one
 * packet and no shared state.
 *
 * <p>Purely presentational. Nothing waits for it and nothing is gated on it arriving.
 *
 * @param world the world's display name, e.g. "Volcano"
 * @param level the course's name within that world, e.g. "3-4"
 */
public record TitleCardPayload(String world, String level) implements CustomPacketPayload {

    public static final Type<TitleCardPayload> TYPE = new Type<>(PlaneShift.id("title_card"));

    public static final StreamCodec<ByteBuf, TitleCardPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, TitleCardPayload::world,
            ByteBufCodecs.STRING_UTF8, TitleCardPayload::level,
            TitleCardPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
