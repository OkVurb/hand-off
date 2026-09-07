package com.studio.planeshift.common.course;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.studio.planeshift.common.form.FormSlot;
import com.studio.planeshift.common.mode.PlaneMode;
import com.studio.planeshift.common.mode.PlayState;
import com.studio.planeshift.common.mode.PlaneRail;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The state that crosses the wire has to come back the same on the other side.
 *
 * <p>{@link CourseState#STREAM_CODEC} is written by hand, as two long parallel lists of fields in
 * matching order. Nothing enforces that they match. Get the order wrong, or add a field to one list
 * and forget the other, and it does not throw -- the bytes are still consumed, just interpreted as
 * the wrong fields, so a player quietly ends up with someone else's coin count in their lives
 * counter and the failure surfaces as a gameplay oddity nobody traces back to networking.
 *
 * <p>The existing suite has a {@code withersPreserveNewFields} test, which says this record has
 * been extended before and that extending it is where the bugs come from. It covers the withers.
 * This covers the wire, which was the half without a net.
 */
class CourseStateCodecTest {

    /**
     * A state with every field set away from its default.
     *
     * <p>Defaults are useless for this: a codec that reads two fields in the wrong order still
     * round-trips correctly when both hold the same default value, so the test would pass on
     * exactly the mistake it exists to catch. Every value here is distinct.
     */
    private static CourseState populated() {
        return new CourseState(
                PlayState.PLAYING_2_5D,
                PlaneMode.SIDE_ON,
                Optional.of(new PlaneRail(Direction.Axis.X, 12.5D, 0.75D, false)),
                Optional.of(Identifier.fromNamespaceAndPath("planeshift", "test_role")),
                new FormSlot(
                        Optional.of(Identifier.fromNamespaceAndPath("planeshift", "fire")),
                        3, 900L,
                        Optional.of(Identifier.fromNamespaceAndPath("planeshift", "ice"))),
                Optional.empty(),
                1,
                4242L,
                Optional.of(new GlobalPos(
                        ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
                                Identifier.fromNamespaceAndPath("planeshift", "course")),
                        new BlockPos(3, 64, -7))),
                57,
                2,
                4,
                -33.5D,
                123456,
                777,
                true,
                CourseTheme.WATER);
    }

    @Test
    @DisplayName("a fully populated state survives the wire unchanged")
    void roundTrips() {
        CourseState before = populated();
        ByteBuf buf = Unpooled.buffer();
        CourseState.STREAM_CODEC.encode(buf, before);
        CourseState after = CourseState.STREAM_CODEC.decode(buf);
        assertEquals(before, after, "CourseState changed crossing the wire; encode and decode "
                + "have drifted out of field order");
        assertEquals(0, buf.readableBytes(),
                "decode left bytes unread, so encode wrote a field decode does not read");
    }

    @Test
    @DisplayName("the default state round-trips too")
    void defaultRoundTrips() {
        ByteBuf buf = Unpooled.buffer();
        CourseState.STREAM_CODEC.encode(buf, CourseState.DEFAULT);
        assertEquals(CourseState.DEFAULT, CourseState.STREAM_CODEC.decode(buf));
        assertEquals(0, buf.readableBytes());
    }

    @Test
    @DisplayName("every theme survives, including any added later")
    void everyThemeRoundTrips() {
        for (CourseTheme theme : CourseTheme.values()) {
            // No wither for theme, so the record is rebuilt from the populated one. That is fine
            // here: the point is that every enum constant survives its own encoding.
            CourseState p = populated();
            CourseState before = new CourseState(p.state(), p.mode(), p.rail(), p.roleId(),
                    p.formSlot(), p.transition(), p.pips(), p.invulnUntil(), p.checkpoint(),
                    p.coins(), p.starCoins(), p.lives(), p.killY(), p.score(), p.timeLeft(),
                    p.autoScroll(), theme);
            ByteBuf buf = Unpooled.buffer();
            CourseState.STREAM_CODEC.encode(buf, before);
            assertEquals(before, CourseState.STREAM_CODEC.decode(buf),
                    theme + " did not survive the wire");
        }
        assertTrue(CourseTheme.values().length > 0);
    }
}
