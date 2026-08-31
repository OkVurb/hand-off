package com.studio.planeshift.common.mode;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;

/**
 * The client-visible slice of a {@link ModeTransaction}: enough to blend the camera
 * between bases and show transition progress, never enough to change collision.
 *
 * <p>"Camera state may interpolate; collision basis changes only at the commit point."
 *
 * @param txId       server transaction id, echoed for correction matching
 * @param fromMode   presentation basis being left
 * @param toMode     presentation basis being entered (camera aims here during the blend)
 * @param targetRail destination rail when {@code toMode} is 2.5D, for camera aim only
 * @param startTick  game time the blend started
 * @param commitTick game time the collision basis changes
 */
public record TransitionSync(
        long txId,
        PlaneMode fromMode,
        PlaneMode toMode,
        java.util.Optional<PlaneRail> targetRail,
        long startTick,
        long commitTick
) {

    public static final Codec<TransitionSync> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.fieldOf("tx_id").forGetter(TransitionSync::txId),
            PlaneMode.CODEC.fieldOf("from_mode").forGetter(TransitionSync::fromMode),
            PlaneMode.CODEC.fieldOf("to_mode").forGetter(TransitionSync::toMode),
            PlaneRail.CODEC.optionalFieldOf("target_rail").forGetter(TransitionSync::targetRail),
            Codec.LONG.fieldOf("start_tick").forGetter(TransitionSync::startTick),
            Codec.LONG.fieldOf("commit_tick").forGetter(TransitionSync::commitTick)
    ).apply(instance, TransitionSync::new));

    public static final StreamCodec<ByteBuf, TransitionSync> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, TransitionSync::txId,
            PlaneMode.STREAM_CODEC, TransitionSync::fromMode,
            PlaneMode.STREAM_CODEC, TransitionSync::toMode,
            ByteBufCodecs.optional(PlaneRail.STREAM_CODEC), TransitionSync::targetRail,
            ByteBufCodecs.VAR_LONG, TransitionSync::startTick,
            ByteBufCodecs.VAR_LONG, TransitionSync::commitTick,
            TransitionSync::new);

    /** Blend progress in [0, 1] at the given time, with partial-tick smoothing. */
    public float progress(long gameTime, float partialTick) {
        float duration = commitTick - startTick;
        if (duration <= 0.0F) {
            return 1.0F;
        }
        return Mth.clamp((gameTime - startTick + partialTick) / duration, 0.0F, 1.0F);
    }
}
