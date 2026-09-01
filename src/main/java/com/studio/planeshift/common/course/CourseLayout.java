package com.studio.planeshift.common.course;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.util.StringRepresentable;

/**
 * Datapack description of a course's shape: where the pits are and which set pieces get built.
 *
 * <p>Before this existed, {@code CourseLayoutPlan} hard-coded six pits per theme and
 * {@code CourseStructureService} decided what to build by switching on the theme. That made every
 * course of a theme identical and meant a new course could only differ by terrain colour. A course
 * JSON can now either list its pits outright or ask for a count and let them be derived, and can
 * name exactly which features it wants.
 *
 * <p>Everything is optional. A course that says nothing gets the theme's defaults, so the fifty
 * existing course files keep working untouched.
 */
public record CourseLayout(
        Optional<List<Gap>> gaps,
        Optional<Integer> gapCount,
        int maxGapWidth,
        int safeStart,
        int safeFinish,
        Optional<Set<Feature>> features,
        Optional<Integer> setPieceCount
) {

    /** Widest pit the movement rules can clear from a standing start with a running jump. */
    public static final int JUMPABLE_LIMIT = 7;
    /** Ground kept clear of pits either side of the spawn and the flagpole. */
    public static final int DEFAULT_SAFE_MARGIN = 15;

    /** A single pit, inclusive on both ends, measured in blocks from the course start. */
    public record Gap(int from, int to) {
        public static final Codec<Gap> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.intRange(0, 512).fieldOf("from").forGetter(Gap::from),
                Codec.intRange(0, 512).fieldOf("to").forGetter(Gap::to)
        ).apply(instance, Gap::new));

        public int width() {
            return to - from + 1;
        }
    }

    /**
     * Set pieces a course can ask for.
     *
     * <p>Named rather than derived from the theme so an author can put a castle finale in a snow
     * course, or leave the conveyor gauntlet out of a course that is already busy.
     */
    public enum Feature implements StringRepresentable {
        DONUT_BRIDGE("donut_bridge"),
        DONUT_GAUNTLET("donut_gauntlet"),
        NOTE_BLOCK_RUN("note_block_run"),
        SECRET_VINE("secret_vine"),
        COIN_HEAVEN("coin_heaven"),
        VERTICAL_CLIMB("vertical_climb"),
        CONVEYOR_GAUNTLET("conveyor_gauntlet"),
        STAIRCASE("staircase"),
        MOVING_PLATFORMS("moving_platforms"),
        CASTLE_FINALE("castle_finale"),
        GHOST_LOOP("ghost_loop");

        public static final Codec<Feature> CODEC = StringRepresentable.fromEnum(Feature::values);

        private final String name;

        Feature(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public static final CourseLayout DEFAULT = new CourseLayout(
            Optional.empty(), Optional.empty(), JUMPABLE_LIMIT,
            DEFAULT_SAFE_MARGIN, DEFAULT_SAFE_MARGIN, Optional.empty(), Optional.empty());

    public static final Codec<CourseLayout> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Gap.CODEC.listOf().optionalFieldOf("gaps").forGetter(CourseLayout::gaps),
            Codec.intRange(0, 24).optionalFieldOf("gap_count").forGetter(CourseLayout::gapCount),
            Codec.intRange(2, JUMPABLE_LIMIT).optionalFieldOf("max_gap_width", JUMPABLE_LIMIT)
                    .forGetter(CourseLayout::maxGapWidth),
            Codec.intRange(6, 64).optionalFieldOf("safe_start", DEFAULT_SAFE_MARGIN)
                    .forGetter(CourseLayout::safeStart),
            Codec.intRange(6, 64).optionalFieldOf("safe_finish", DEFAULT_SAFE_MARGIN)
                    .forGetter(CourseLayout::safeFinish),
            Feature.CODEC.listOf().xmap(CourseLayout::toSet, List::copyOf)
                    .optionalFieldOf("features").forGetter(CourseLayout::features),
            Codec.intRange(0, 24).optionalFieldOf("set_piece_count").forGetter(CourseLayout::setPieceCount)
    ).apply(instance, CourseLayout::new));

    private static Set<Feature> toSet(List<Feature> list) {
        return list.isEmpty() ? EnumSet.noneOf(Feature.class) : EnumSet.copyOf(list);
    }

    /** Features this course builds, falling back to the theme's default set. */
    public Set<Feature> resolvedFeatures(CourseTheme theme) {
        return features.orElseGet(() -> defaultFeatures(theme));
    }

    /**
     * The theme defaults, which reproduce what the hard-coded generator used to do: every theme
     * gets the shared set pieces, lava additionally gets the castle finale, ghost house the loop.
     */
    public static Set<Feature> defaultFeatures(CourseTheme theme) {
        EnumSet<Feature> set = EnumSet.of(
                Feature.DONUT_BRIDGE, Feature.DONUT_GAUNTLET, Feature.NOTE_BLOCK_RUN,
                Feature.SECRET_VINE, Feature.COIN_HEAVEN, Feature.VERTICAL_CLIMB,
                Feature.CONVEYOR_GAUNTLET, Feature.STAIRCASE, Feature.MOVING_PLATFORMS);
        if (theme == CourseTheme.LAVA) {
            set.add(Feature.CASTLE_FINALE);
        }
        if (theme == CourseTheme.GHOST_HOUSE) {
            set.add(Feature.GHOST_LOOP);
        }
        return set;
    }
}
