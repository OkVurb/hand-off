package com.studio.planeshift.common.course;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.studio.planeshift.common.form.FormSlot;
import com.studio.planeshift.common.mode.PlaneMode;
import com.studio.planeshift.common.mode.PlaneRail;
import com.studio.planeshift.common.mode.PlayState;
import com.studio.planeshift.common.mode.TransitionSync;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

/**
 * Per-player course state: the single authoritative snapshot of "where this player is
 * in the PlaneShift state machine".
 *
 * <p>Owned by the server via a data attachment; synced to the owning client for camera,
 * input projection and HUD. The client never writes it back.
 *
 * <p>Invariants (Design Bible, "Game and course state model"):
 * <ul>
 *   <li>One authoritative mode per player, plus transaction id and timestamps.</li>
 *   <li>A course never saves in the middle of an uncommitted transition - persistence
 *       strips {@link #transition} and resolves to the committed mode.</li>
 *   <li>Disconnect during a course resolves to last checkpoint or hub, never an
 *       undefined position.</li>
 * </ul>
 *
 * @param state         current state machine node
 * @param mode          committed perspective (also valid during TRANSITION: the mode
 *                      being left until the commit tick arrives)
 * @param rail          active movement plane, present in and while entering 2.5D
 * @param roleId        selected role, if any
 * @param formSlot      active/reserve Form loadout
 * @param transition    presentation data for an in-flight transaction
 * @param pips          remaining health pips (course damage model, max {@link #MAX_PIPS})
 * @param invulnUntil   game time until post-damage invulnerability ends
 * @param checkpoint    last activated checkpoint
 * @param coins         course currency count
 * @param starCoins     secret star coin collectibles
 * @param lives         extra lives remaining (1-Up pickups)
 * @param killY         falling below this Y returns the player to the checkpoint
 */
public record CourseState(
        PlayState state,
        PlaneMode mode,
        Optional<PlaneRail> rail,
        Optional<Identifier> roleId,
        FormSlot formSlot,
        Optional<TransitionSync> transition,
        int pips,
        long invulnUntil,
        Optional<GlobalPos> checkpoint,
        int coins,
        int starCoins,
        int lives,
        double killY
) {
    public static final int MAX_PIPS = 2;
    public static final int STARTING_LIVES = 3;
    public static final int MAX_VALUE = 1_000_000;
    public static final double DEFAULT_KILL_Y = -50.0D;

    public static final CourseState DEFAULT = new CourseState(
            PlayState.HUB, PlaneMode.FREE_3D, Optional.empty(), Optional.empty(),
            FormSlot.EMPTY, Optional.empty(), MAX_PIPS, 0L, Optional.empty(), 0, 0, STARTING_LIVES, DEFAULT_KILL_Y);

    /**
     * Persistence codec. Deliberately excludes {@link #transition}: an uncommitted
     * transaction must never be saved; on load the player resumes in the committed mode.
     */
    public static final MapCodec<CourseState> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            PlayState.CODEC.optionalFieldOf("state", PlayState.HUB).forGetter(CourseState::state),
            PlaneMode.CODEC.optionalFieldOf("mode", PlaneMode.FREE_3D).forGetter(CourseState::mode),
            PlaneRail.CODEC.optionalFieldOf("rail").forGetter(CourseState::rail),
            Identifier.CODEC.optionalFieldOf("role").forGetter(CourseState::roleId),
            FormSlot.CODEC.optionalFieldOf("form_slot", FormSlot.EMPTY).forGetter(CourseState::formSlot),
            com.mojang.serialization.Codec.intRange(0, MAX_PIPS).optionalFieldOf("pips", MAX_PIPS)
                    .forGetter(CourseState::pips),
            com.mojang.serialization.Codec.LONG.optionalFieldOf("invuln_until", 0L)
                    .forGetter(CourseState::invulnUntil),
            GlobalPos.CODEC.optionalFieldOf("checkpoint").forGetter(CourseState::checkpoint),
            com.mojang.serialization.Codec.intRange(0, 1_000_000).optionalFieldOf("coins", 0)
                    .forGetter(CourseState::coins),
            com.mojang.serialization.Codec.intRange(0, 1_000_000).optionalFieldOf("star_coins", 0)
                    .forGetter(CourseState::starCoins),
            com.mojang.serialization.Codec.intRange(0, 1_000_000).optionalFieldOf("lives", STARTING_LIVES)
                    .forGetter(CourseState::lives),
            com.mojang.serialization.Codec.DOUBLE.optionalFieldOf("kill_y", DEFAULT_KILL_Y)
                    .forGetter(CourseState::killY)
    ).apply(instance, (state, mode, rail, role, formSlot, pips, invuln, checkpoint, coins, starCoins, lives, killY) ->
            sanitize(new CourseState(state, mode, rail, role, formSlot,
                    Optional.empty(), pips, invuln, checkpoint, coins, starCoins, lives, killY))));

    /** Network codec: the full snapshot, including transition presentation data. */
    public static final StreamCodec<ByteBuf, CourseState> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public CourseState decode(ByteBuf buf) {
            PlayState state = PlayState.STREAM_CODEC.decode(buf);
            PlaneMode mode = PlaneMode.STREAM_CODEC.decode(buf);
            Optional<PlaneRail> rail = ByteBufCodecs.optional(PlaneRail.STREAM_CODEC).decode(buf);
            Optional<Identifier> roleId = ByteBufCodecs.optional(Identifier.STREAM_CODEC).decode(buf);
            FormSlot formSlot = FormSlot.STREAM_CODEC.decode(buf);
            Optional<TransitionSync> transition = ByteBufCodecs.optional(TransitionSync.STREAM_CODEC).decode(buf);
            int pips = ByteBufCodecs.VAR_INT.decode(buf);
            long invulnUntil = ByteBufCodecs.VAR_LONG.decode(buf);
            Optional<GlobalPos> checkpoint = ByteBufCodecs.optional(GlobalPos.STREAM_CODEC).decode(buf);
            int coins = ByteBufCodecs.VAR_INT.decode(buf);
            int starCoins = ByteBufCodecs.VAR_INT.decode(buf);
            int lives = ByteBufCodecs.VAR_INT.decode(buf);
            double killY = ByteBufCodecs.DOUBLE.decode(buf);
            return new CourseState(state, mode, rail, roleId, formSlot, transition,
                    pips, invulnUntil, checkpoint, coins, starCoins, lives, killY);
        }

        @Override
        public void encode(ByteBuf buf, CourseState state) {
            PlayState.STREAM_CODEC.encode(buf, state.state());
            PlaneMode.STREAM_CODEC.encode(buf, state.mode());
            ByteBufCodecs.optional(PlaneRail.STREAM_CODEC).encode(buf, state.rail());
            ByteBufCodecs.optional(Identifier.STREAM_CODEC).encode(buf, state.roleId());
            FormSlot.STREAM_CODEC.encode(buf, state.formSlot());
            ByteBufCodecs.optional(TransitionSync.STREAM_CODEC).encode(buf, state.transition());
            ByteBufCodecs.VAR_INT.encode(buf, state.pips());
            ByteBufCodecs.VAR_LONG.encode(buf, state.invulnUntil());
            ByteBufCodecs.optional(GlobalPos.STREAM_CODEC).encode(buf, state.checkpoint());
            ByteBufCodecs.VAR_INT.encode(buf, state.coins());
            ByteBufCodecs.VAR_INT.encode(buf, state.starCoins());
            ByteBufCodecs.VAR_INT.encode(buf, state.lives());
            ByteBufCodecs.DOUBLE.encode(buf, state.killY());
        }
    };

    /** A state loaded from disk can never resume mid-transition or mid-death. */
    private static CourseState sanitize(CourseState loaded) {
        PlayState state = switch (loaded.state()) {
            case TRANSITION, DOWNED, COURSE_READY, RESULTS -> PlayState.HUB;
            default -> loaded.state();
        };

        // Enforce mode/rail consistency and strip rails outside side-on play.
        if (state.isHub()) {
            return new CourseState(PlayState.HUB, PlaneMode.FREE_3D, Optional.empty(),
                    loaded.roleId(), loaded.formSlot(), Optional.empty(),
                    loaded.pips(), loaded.invulnUntil(), loaded.checkpoint(),
                    loaded.coins(), loaded.starCoins(), loaded.lives(), loaded.killY());
        }
        PlaneMode mode = loaded.mode();
        Optional<PlaneRail> rail = loaded.rail();
        if (mode == PlaneMode.SIDE_ON && rail.isEmpty()) {
            mode = PlaneMode.FREE_3D;
            state = PlayState.PLAYING_3D;
        } else if (mode == PlaneMode.FREE_3D) {
            rail = Optional.empty();
            state = PlayState.PLAYING_3D;
        } else if (mode == PlaneMode.SIDE_ON) {
            state = PlayState.PLAYING_2_5D;
        }

        return new CourseState(state, mode, rail, loaded.roleId(), loaded.formSlot(), Optional.empty(),
                loaded.pips(), loaded.invulnUntil(), loaded.checkpoint(),
                loaded.coins(), loaded.starCoins(), loaded.lives(), loaded.killY());
    }

    /** The perspective the client should present right now. */
    public PlaneMode presentedMode() {
        return mode;
    }

    public boolean in2_5D() {
        return state == PlayState.PLAYING_2_5D;
    }

    public boolean inCourse() {
        return state.inCourse();
    }

    public boolean isHub() {
        return state.isHub();
    }

    public boolean invulnerable(long gameTime) {
        return gameTime < invulnUntil;
    }

    // Withers: attachments store immutable snapshots; services replace and re-sync them.

    public CourseState withState(PlayState newState) {
        return new CourseState(newState, mode, rail, roleId, formSlot, transition,
                pips, invulnUntil, checkpoint, coins, starCoins, lives, killY);
    }

    public CourseState withMode(PlaneMode newMode, Optional<PlaneRail> newRail) {
        PlayState newState = state == PlayState.HUB ? PlayState.HUB : PlayState.playingFor(newMode);
        return new CourseState(newState, newMode, newRail, roleId, formSlot, Optional.empty(),
                pips, invulnUntil, checkpoint, coins, starCoins, lives, killY);
    }

    public CourseState withTransition(Optional<TransitionSync> newTransition) {
        PlayState newState = newTransition.isPresent() ? PlayState.TRANSITION : state;
        return new CourseState(newState, mode, rail, roleId, formSlot, newTransition,
                pips, invulnUntil, checkpoint, coins, starCoins, lives, killY);
    }

    public CourseState withRole(Optional<Identifier> newRole) {
        return new CourseState(state, mode, rail, newRole, formSlot, transition,
                pips, invulnUntil, checkpoint, coins, starCoins, lives, killY);
    }

    public CourseState withFormSlot(FormSlot newSlot) {
        return new CourseState(state, mode, rail, roleId, newSlot, transition,
                pips, invulnUntil, checkpoint, coins, starCoins, lives, killY);
    }

    public CourseState withPips(int newPips, long newInvulnUntil) {
        return new CourseState(state, mode, rail, roleId, formSlot, transition,
                Math.max(0, Math.min(MAX_PIPS, newPips)), newInvulnUntil, checkpoint, coins, starCoins, lives, killY);
    }

    public CourseState withCheckpoint(Optional<GlobalPos> newCheckpoint) {
        return new CourseState(state, mode, rail, roleId, formSlot, transition,
                pips, invulnUntil, newCheckpoint, coins, starCoins, lives, killY);
    }

    public CourseState withCoins(int newCoins) {
        return new CourseState(state, mode, rail, roleId, formSlot, transition,
                pips, invulnUntil, checkpoint, clampValue(newCoins), starCoins, lives, killY);
    }

    public CourseState withStarCoins(int newStarCoins) {
        return new CourseState(state, mode, rail, roleId, formSlot, transition,
                pips, invulnUntil, checkpoint, coins, clampValue(newStarCoins), lives, killY);
    }

    public CourseState withLives(int newLives) {
        return new CourseState(state, mode, rail, roleId, formSlot, transition,
                pips, invulnUntil, checkpoint, coins, starCoins, clampValue(newLives), killY);
    }

    private static int clampValue(int value) {
        if (value < 0) return 0;
        if (value > MAX_VALUE) return MAX_VALUE;
        return value;
    }
}
