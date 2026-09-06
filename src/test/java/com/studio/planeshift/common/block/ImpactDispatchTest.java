package com.studio.planeshift.common.block;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.studio.planeshift.common.registry.ModBlocks;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

/**
 * Every block that reacts to a hit has decided what an impact from above does.
 *
 * <p>{@link HitFromBelowBlock#impact} exists to enforce one rule: a block's reward must not depend
 * on which side you hit it from. The rule was written down, in the right place, with the reasoning
 * attached -- and it stopped holding anyway. The dispatcher handled four blocks while thirteen
 * implemented the interface, and the most recent block to miss it was added four commits after
 * someone read that javadoc.
 *
 * <p>Which is the actual lesson: a convention only survives if joining it is part of writing the
 * thing. Nothing made it part of that, so this does. A new implementer of
 * {@link HitFromBelowBlock} fails here until it is classified, and the classification has to carry
 * a reason -- so the next person is forced to make the decision rather than to not notice there
 * was one.
 *
 * <p>Deliberately does not check that the dispatcher <em>behaves</em> correctly; that needs a
 * Level. It checks that nobody has been forgotten, which is the failure that actually happened.
 */
class ImpactDispatchTest {

    /** Blocks {@code HitFromBelowBlock.impact} routes to. */
    private static final Set<String> DISPATCHED = Set.of(
            "question_block",
            "coin_block",
            "rotating_block",
            "brick_block",
            "prize_cache",
            "toad_box");

    /** Blocks deliberately left out, and why. The reason is the point of the entry. */
    private static final Map<String, String> EXEMPT = Map.of(
            "hidden_question_block",
            "found by hitting it from underneath; landing on top would give away a secret the "
                    + "player never went looking for",
            "secret_vine",
            "same as the hidden question block: revealing it from above defeats it being hidden",
            "coin_ring_block",
            "passed through rather than struck",
            "p_switch",
            "implements stepOn, so a pound already triggers it; dispatching here too would fire "
                    + "it twice on the same landing",
            "on_off_switch",
            "implements stepOn, as above",
            "music_block",
            "implements stepOn, as above",
            "note_block",
            "implements stepOn, as above");

    @Test
    void everyHitFromBelowBlockIsEitherDispatchedOrDeliberatelyExempt() {
        List<String> unclassified = new ArrayList<>();
        ModBlocks.BLOCKS.getEntries().forEach(holder -> {
            if (!(holder.get() instanceof HitFromBelowBlock)) {
                return;
            }
            String id = holder.getId().getPath();
            if (!DISPATCHED.contains(id) && !EXEMPT.containsKey(id)) {
                unclassified.add(id);
            }
        });
        java.util.Collections.sort(unclassified);
        assertEquals(List.of(), unclassified,
                "these react to a hit from below but nobody has decided what a ground pound or a "
                        + "kicked shell does to them; add them to HitFromBelowBlock.impact, or to "
                        + "EXEMPT here with a reason");
    }

    @Test
    void aBlockIsNotBothDispatchedAndExempt() {
        Set<String> both = new TreeSet<>(DISPATCHED);
        both.retainAll(EXEMPT.keySet());
        assertEquals(Set.of(), both, "listed twice, so the intent is unreadable");
    }

    @Test
    void neitherListNamesABlockThatNoLongerReactsToHits() {
        // Catches the other direction: a block renamed or stripped of the interface leaves a stale
        // entry behind, and a stale exemption is how a real omission hides in plain sight.
        Set<String> live = new TreeSet<>();
        ModBlocks.BLOCKS.getEntries().forEach(holder -> {
            if (holder.get() instanceof HitFromBelowBlock) {
                live.add(holder.getId().getPath());
            }
        });

        Set<String> stale = new TreeSet<>(DISPATCHED);
        stale.addAll(EXEMPT.keySet());
        stale.removeAll(live);
        assertEquals(Set.of(), stale,
                "these are classified but no longer implement HitFromBelowBlock");
    }

    @Test
    void everyExemptionGivesAReason() {
        List<String> empty = new ArrayList<>();
        EXEMPT.forEach((id, reason) -> {
            // An exemption without a reason is indistinguishable from an oversight, which is the
            // state this whole test exists to make impossible.
            if (reason == null || reason.isBlank() || reason.length() < 20) {
                empty.add(id);
            }
        });
        java.util.Collections.sort(empty);
        assertEquals(List.of(), empty, "exempt without a usable reason");
    }
}
