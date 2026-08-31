package com.studio.planeshift.common.mode;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.function.IntFunction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;

/**
 * The two gameplay perspectives.
 *
 * <p>Bible non-negotiable: "The camera mode is a gameplay rule, not a cosmetic toggle.
 * Collision, targeting, level bounds, networking and course readability must all agree
 * with the active mode." The server owns which mode a player is in; the client only
 * presents it.
 */
public enum PlaneMode implements StringRepresentable {
    /** Side-on 2.5D: perspective rail camera, plane-constrained movement. */
    SIDE_ON("side_on"),
    /** Free 3D: authored third-person camera, full-space movement. */
    FREE_3D("free_3d");

    public static final Codec<PlaneMode> CODEC = StringRepresentable.fromEnum(PlaneMode::values);
    private static final IntFunction<PlaneMode> BY_ID =
            ByIdMap.continuous(Enum::ordinal, values(), ByIdMap.OutOfBoundsStrategy.WRAP);
    public static final StreamCodec<ByteBuf, PlaneMode> STREAM_CODEC =
            ByteBufCodecs.idMapper(BY_ID, Enum::ordinal);

    private final String name;

    PlaneMode(String name) {
        this.name = name;
    }

    public PlaneMode opposite() {
        return this == SIDE_ON ? FREE_3D : SIDE_ON;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }
}
