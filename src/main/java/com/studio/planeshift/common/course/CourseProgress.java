package com.studio.planeshift.common.course;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * What a player has actually accomplished, persisted across sessions.
 *
 * <p>Kept separate from {@link CourseState}, which is the snapshot of the run in progress and is
 * reset every time a course loads. This is the save file: which courses have been cleared, how
 * many star coins were found in each, and the best score and remaining clock. It is what the world
 * map draws and what the unlock rules read.
 *
 * <p>{@link #currentCourse} lives here rather than in {@code CourseState} for one reason: the
 * things that need to know which course a player is in — recording a clear, crediting a star coin
 * — all also need this record, and putting it here means a reconnecting player is still attributed
 * to the right course instead of losing their star coins to whichever course they load next.
 */
public record CourseProgress(Optional<String> currentCourse, Map<String, Record> records) {

    /** Star coins hidden in each course. Collecting all three is the completionist goal. */
    public static final int STAR_COINS_PER_COURSE = 3;

    /** How many courses a player may have records for, so a malformed save cannot grow forever. */
    private static final int MAX_RECORDS = 512;

    public static final CourseProgress DEFAULT = new CourseProgress(Optional.empty(), Map.of());

    /**
     * One course's result.
     *
     * @param cleared      whether the flagpole has ever been touched
     * @param starCoins    star coins found, 0 to {@link #STAR_COINS_PER_COURSE}
     * @param bestScore    highest score achieved on this course
     * @param bestTimeLeft most clock left at the flagpole, as a speedrun measure
     */
    public record Record(boolean cleared, int starCoins, int bestScore, int bestTimeLeft) {

        public static final Record EMPTY = new Record(false, 0, 0, 0);

        public static final Codec<Record> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.BOOL.optionalFieldOf("cleared", false).forGetter(Record::cleared),
                Codec.intRange(0, STAR_COINS_PER_COURSE).optionalFieldOf("star_coins", 0)
                        .forGetter(Record::starCoins),
                Codec.intRange(0, CourseState.MAX_VALUE).optionalFieldOf("best_score", 0)
                        .forGetter(Record::bestScore),
                Codec.intRange(0, CourseState.MAX_VALUE).optionalFieldOf("best_time_left", 0)
                        .forGetter(Record::bestTimeLeft)
        ).apply(instance, Record::new));

        public static final StreamCodec<ByteBuf, Record> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, Record::cleared,
                ByteBufCodecs.VAR_INT, Record::starCoins,
                ByteBufCodecs.VAR_INT, Record::bestScore,
                ByteBufCodecs.VAR_INT, Record::bestTimeLeft,
                Record::new);

        public boolean allStarCoins() {
            return starCoins >= STAR_COINS_PER_COURSE;
        }
    }

    public static final MapCodec<CourseProgress> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.optionalFieldOf("current_course").forGetter(CourseProgress::currentCourse),
            Codec.unboundedMap(Codec.STRING, Record.CODEC).optionalFieldOf("records", Map.of())
                    .forGetter(CourseProgress::records)
    ).apply(instance, CourseProgress::new));

    public static final StreamCodec<ByteBuf, CourseProgress> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8), CourseProgress::currentCourse,
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, Record.STREAM_CODEC),
            CourseProgress::records,
            CourseProgress::new);

    public Record record(String courseId) {
        return records.getOrDefault(courseId, Record.EMPTY);
    }

    public boolean cleared(String courseId) {
        return record(courseId).cleared();
    }

    public int starCoins(String courseId) {
        return record(courseId).starCoins();
    }

    public int clearedCount() {
        return (int) records.values().stream().filter(Record::cleared).count();
    }

    public int totalStarCoins() {
        return records.values().stream().mapToInt(Record::starCoins).sum();
    }

    public CourseProgress withCurrentCourse(Optional<String> courseId) {
        return new CourseProgress(courseId, records);
    }

    /**
     * Applies a change to one course's record.
     *
     * <p>Copy-on-write with an insertion-ordered map, so the save file reads in the order courses
     * were first played rather than in hash order — a small thing, but it makes a save file
     * diffable when something goes wrong.
     */
    public CourseProgress withRecord(String courseId, java.util.function.UnaryOperator<Record> change) {
        Record existing = record(courseId);
        Record updated = change.apply(existing);
        if (existing.equals(updated)) {
            return this;
        }
        if (!records.containsKey(courseId) && records.size() >= MAX_RECORDS) {
            return this;
        }
        Map<String, Record> copy = new LinkedHashMap<>(records);
        copy.put(courseId, updated);
        return new CourseProgress(currentCourse, Map.copyOf(copy));
    }

    /**
     * Records a clear, keeping the best score and the best remaining clock rather than the latest.
     * A player who beats a course badly after beating it well has not lost their record.
     */
    public CourseProgress withClear(String courseId, int score, int timeLeft) {
        return withRecord(courseId, r -> new Record(true,
                r.starCoins(),
                Math.max(r.bestScore(), Math.max(0, score)),
                Math.max(r.bestTimeLeft(), Math.max(0, timeLeft))));
    }

    /** Credits one star coin, capped so replaying a course cannot inflate the total. */
    public CourseProgress withStarCoin(String courseId) {
        return withRecord(courseId, r -> r.starCoins() >= STAR_COINS_PER_COURSE
                ? r
                : new Record(r.cleared(), r.starCoins() + 1, r.bestScore(), r.bestTimeLeft()));
    }
}
