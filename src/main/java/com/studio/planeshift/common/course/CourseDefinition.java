package com.studio.planeshift.common.course;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.studio.planeshift.common.mode.PlaneMode;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Optional;

/**
 * Datapack course definition. Authors can override the default course layout, start
 * position, theme and optional structure template without touching Java.
 */
public record CourseDefinition(
        Identifier dimension,
        BlockPos startPos,
        PlaneMode startMode,
        CourseTheme theme,
        double killY,
        Optional<Identifier> structure,
        int length,
        int timeLimitSeconds,
        boolean autoScroll,
        Optional<CourseLayout> layout
) {
    public static final int DEFAULT_LENGTH = 720;

    /** Classic arcade clock length. Zero means the course is untimed. */
    public static final int DEFAULT_TIME_LIMIT_SECONDS = 900;

    public CourseDefinition(Identifier dimension, BlockPos startPos, PlaneMode startMode,
                            CourseTheme theme, double killY) {
        this(dimension, startPos, startMode, theme, killY, Optional.empty(), DEFAULT_LENGTH,
                DEFAULT_TIME_LIMIT_SECONDS, false, Optional.empty());
    }

    public static final Codec<CourseDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.fieldOf("dimension").forGetter(CourseDefinition::dimension),
            BlockPos.CODEC.fieldOf("start_pos").forGetter(CourseDefinition::startPos),
            PlaneMode.CODEC.fieldOf("start_mode").orElse(PlaneMode.SIDE_ON).forGetter(CourseDefinition::startMode),
            CourseTheme.CODEC.fieldOf("theme").orElse(CourseTheme.GRASS).forGetter(CourseDefinition::theme),
            Codec.DOUBLE.fieldOf("kill_y").orElse(CourseState.DEFAULT_KILL_Y).forGetter(CourseDefinition::killY),
            Identifier.CODEC.optionalFieldOf("structure").forGetter(CourseDefinition::structure),
            Codec.intRange(64, 2048).optionalFieldOf("length", DEFAULT_LENGTH).forGetter(CourseDefinition::length),
            Codec.intRange(0, 9999).optionalFieldOf("time_limit_seconds", DEFAULT_TIME_LIMIT_SECONDS)
                    .forGetter(CourseDefinition::timeLimitSeconds),
            Codec.BOOL.optionalFieldOf("auto_scroll", false).forGetter(CourseDefinition::autoScroll),
            CourseLayout.CODEC.optionalFieldOf("layout").forGetter(CourseDefinition::layout)
    ).apply(instance, CourseDefinition::new));

    /** Starting clock in ticks, or { CourseState#NO_TIME_LIMIT} when untimed. */
    public int timeLimitTicks() {
        return timeLimitSeconds <= 0 ? CourseState.NO_TIME_LIMIT : timeLimitSeconds * 20;
    }

    public ResourceKey<Level> dimensionKey() {
        return ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, dimension);
    }
}
