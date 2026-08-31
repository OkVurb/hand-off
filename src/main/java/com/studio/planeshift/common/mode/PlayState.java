package com.studio.planeshift.common.mode;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import java.util.function.IntFunction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;

/**
 * Game and course state model (Design Bible, "Game and course state model").
 *
 * <p>Formal state prevents camera, movement, death and multiplayer from disagreeing.
 * Each transition has one owner (the server) and a bounded timeout.
 */
public enum PlayState implements StringRepresentable {
    /** Full 3D, peaceful. Role/loadout, unlocks, party. Exit: enter course portal. */
    HUB("hub"),
    /** Limited control while the server loads metadata, spawns and counts down. */
    COURSE_READY("course_ready"),
    /** Plane-constrained play. Exit: shift gate, death or goal. */
    PLAYING_2_5D("playing_2_5d"),
    /** Full-space play. Exit: shift gate, death or goal. */
    PLAYING_3D("playing_3d"),
    /** Mapped/soft-limited control while the server validates and commits a new mode. */
    TRANSITION("transition"),
    /** No control; server resolves damage result and recovery. */
    DOWNED("downed"),
    /** Menu and celebration; score, rewards, progress. */
    RESULTS("results");

    public static final Codec<PlayState> CODEC = StringRepresentable.fromEnum(PlayState::values);
    private static final IntFunction<PlayState> BY_ID =
            ByIdMap.continuous(Enum::ordinal, values(), ByIdMap.OutOfBoundsStrategy.WRAP);
    public static final StreamCodec<ByteBuf, PlayState> STREAM_CODEC =
            ByteBufCodecs.idMapper(BY_ID, Enum::ordinal);

    private final String name;

    PlayState(String name) {
        this.name = name;
    }

    /** The perspective this state mandates, if it mandates one. */
    public Optional<PlaneMode> mandatedMode() {
        return switch (this) {
            case PLAYING_2_5D -> Optional.of(PlaneMode.SIDE_ON);
            case PLAYING_3D -> Optional.of(PlaneMode.FREE_3D);
            default -> Optional.empty();
        };
    }

    public static PlayState playingFor(PlaneMode mode) {
        return mode == PlaneMode.SIDE_ON ? PLAYING_2_5D : PLAYING_3D;
    }

    public boolean inCourse() {
        return this == PLAYING_2_5D || this == PLAYING_3D;
    }

    public boolean isHub() {
        return this == HUB;
    }

    public boolean acceptsShiftRequests() {
        return this == PLAYING_2_5D || this == PLAYING_3D || this == HUB;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }
}
