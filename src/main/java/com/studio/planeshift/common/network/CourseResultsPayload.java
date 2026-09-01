package com.studio.planeshift.common.network;

import com.studio.planeshift.PlaneShift;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * S2C: the course is over; show the breakdown.
 *
 * <p>Everything the results screen needs travels in the packet rather than being read off the
 * synced course state, because clearing a course resets that state — by the time the screen opens,
 * the score and the clock the player just earned are already gone.
 *
 * @param courseId     which course was cleared
 * @param score        final score for this run
 * @param timeLeft     ticks left on the clock, or 0 for an untimed course
 * @param timeBonus    points awarded for the remaining clock
 * @param coins        coins carried out
 * @param starCoins    star coins found on this run
 * @param lives        lives remaining
 * @param newBestScore whether this run beat the stored record
 */
public record CourseResultsPayload(String courseId, int score, int timeLeft, int timeBonus,
                                   int coins, int starCoins, int lives, boolean newBestScore)
        implements CustomPacketPayload {

    public static final Type<CourseResultsPayload> TYPE = new Type<>(PlaneShift.id("course_results"));

    public static final StreamCodec<ByteBuf, CourseResultsPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, CourseResultsPayload::courseId,
                    ByteBufCodecs.VAR_INT, CourseResultsPayload::score,
                    ByteBufCodecs.VAR_INT, CourseResultsPayload::timeLeft,
                    ByteBufCodecs.VAR_INT, CourseResultsPayload::timeBonus,
                    ByteBufCodecs.VAR_INT, CourseResultsPayload::coins,
                    ByteBufCodecs.VAR_INT, CourseResultsPayload::starCoins,
                    ByteBufCodecs.VAR_INT, CourseResultsPayload::lives,
                    ByteBufCodecs.BOOL, CourseResultsPayload::newBestScore,
                    CourseResultsPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
