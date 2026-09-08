package com.studio.planeshift.client.render;

import com.studio.planeshift.client.ClientCourseState;
import com.studio.planeshift.common.course.CourseTheme;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * The colour of the air in a course.
 *
 * <p>Plan entry 2.4: aerial perspective in this mod desaturated everything toward one grey, and the
 * reference does not do that. A volcano tints the <em>whole scene</em> warm orange — the foreground
 * included — because the air itself is hot; a snow world runs cold and blue through the same air.
 * The note said the implementation could not express a warm key over the whole frame, and it was
 * right: the haze lived in the far-layer block palette, so it could only ever reach the things that
 * were far away.
 *
 * <p>This reaches everything, because it is the fog colour rather than a material. Fog colour is
 * Minecraft's own name for "what colour is the air", and it is mixed into the whole frame by the
 * renderer without anything being drawn on top of the world — which matters here, since the mod's
 * one hard rule about the reference's look is that nothing is an overlay.
 *
 * <h2>Colour only; distance is left alone</h2>
 *
 * <p>Deliberately does not touch how far the player can see. That was settled earlier and for a
 * good reason: the reference's water does not close in around you, and neither does its lava-lit
 * air. Tinting is a look; shortening the view is a handicap, and the two get confused because
 * vanilla ships them in the same object.
 *
 * <h2>Mixed, not replaced</h2>
 *
 * <p>{@link #STRENGTH} blends toward the theme colour rather than setting it. A full replacement
 * makes every course look like it is being viewed through coloured glass, which is the failure mode
 * of every "atmospheric tint" ever added to a game; a partial mix reads as light in the air, which
 * is what it is meant to be.
 */
public final class CourseAtmosphere {

    /** How far toward the theme colour the air is pulled. */
    private static final float STRENGTH = 0.55F;

    private CourseAtmosphere() {
    }

    /** Tints the air for the course the player is in. */
    public static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        var state = ClientCourseState.get();
        if (!state.inCourse()) {
            return;
        }
        float[] key = keyFor(state.theme());
        if (key == null) {
            return;
        }
        event.setRed(mix(event.getRed(), key[0]));
        event.setGreen(mix(event.getGreen(), key[1]));
        event.setBlue(mix(event.getBlue(), key[2]));
    }

    private static float mix(float from, float to) {
        return from + (to - from) * STRENGTH;
    }

    /**
     * The colour of the air, per theme.
     *
     * <p>Not every theme gets one. A grass level's air is ordinary air, and giving it a tint
     * anyway would spend the effect on the one world that does not need it — the point of a warm
     * volcano is that it is warm compared to somewhere.
     */
    private static float[] keyFor(CourseTheme theme) {
        return switch (theme) {
            // Hot air over lava: strongly orange, and the reference pushes this further than feels
            // reasonable until you see it next to a grass level.
            case LAVA -> new float[] {0.62F, 0.26F, 0.12F};
            // Cold and blue, and paler than the volcano is dark: snow scatters light rather than
            // absorbing it, so the air brightens instead of deepening.
            case SNOW -> new float[] {0.68F, 0.78F, 0.92F};
            // Dust in the air, low and yellow.
            case DESERT -> new float[] {0.78F, 0.66F, 0.42F};
            // Green-black, and the one theme where the air is doing the work of a light source.
            case GHOST_HOUSE -> new float[] {0.16F, 0.22F, 0.20F};
            // Nearly nothing, deliberately. The cave backdrop draws no scenery at all so the void
            // behind the terrain stays empty; tinting that void would fill it back in.
            case UNDERGROUND -> new float[] {0.06F, 0.06F, 0.09F};
            // Water and sky already have their own strong colour from the fluid and the skybox,
            // and grass wants ordinary air.
            default -> null;
        };
    }
}
