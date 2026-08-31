package com.studio.planeshift.client.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Covers {@link PlaneConstrainedInput#railProjection}, the geometry that lets 2.5D movement
 * stop depending on the player's yaw.
 *
 * <p>The projection is only correct if it survives the engine's own rotation. These tests
 * therefore re-implement {@code Entity#getInputVector} exactly as the engine applies it and
 * assert on the resulting world-space direction, not on the raw vector.
 */
class PlaneConstrainedInputTest {

    private static final float EPSILON = 1.0E-4F;

    /**
     * Mirror of {@code Entity#getInputVector}: it maps {@code (strafe, forward)} through the
     * player's yaw. Copied rather than called because the real method is protected static on
     * {@code Entity} and needs no entity to be meaningful.
     */
    private static Vec3 engineWorldVector(Vec2 move, float playerYaw) {
        float sin = Mth.sin(playerYaw * Mth.DEG_TO_RAD);
        float cos = Mth.cos(playerYaw * Mth.DEG_TO_RAD);
        return new Vec3(move.x * cos - move.y * sin, 0.0D, move.y * cos + move.x * sin);
    }

    /** The unit world direction the engine walks for a given yaw. */
    private static Vec3 directionOf(float yaw) {
        return new Vec3(-Mth.sin(yaw * Mth.DEG_TO_RAD), 0.0D, Mth.cos(yaw * Mth.DEG_TO_RAD));
    }

    @ParameterizedTest(name = "player looking at yaw {0} still travels along the rail")
    @ValueSource(floats = {-180.0F, -135.0F, -90.0F, -45.0F, 0.0F, 30.0F, 45.0F, 90.0F,
            135.0F, 179.0F, 270.0F, 361.0F})
    @DisplayName("world travel direction is independent of where the player looks")
    void projectionCancelsPlayerYaw(float playerYaw) {
        float travelYaw = 57.0F;

        Vec3 actual = engineWorldVector(
                PlaneConstrainedInput.railProjection(playerYaw, travelYaw), playerYaw);
        Vec3 expected = directionOf(travelYaw);

        assertEquals(expected.x, actual.x, EPSILON, "world X for player yaw " + playerYaw);
        assertEquals(expected.z, actual.z, EPSILON, "world Z for player yaw " + playerYaw);
    }

    @ParameterizedTest(name = "rail heading {0} is reproduced exactly")
    @ValueSource(floats = {-180.0F, -90.0F, 0.0F, 45.0F, 90.0F, 123.5F, 180.0F, 270.0F})
    @DisplayName("every rail heading is walked, for an awkward player yaw")
    void everyRailHeadingIsReproduced(float travelYaw) {
        float playerYaw = -213.75F;

        Vec3 actual = engineWorldVector(
                PlaneConstrainedInput.railProjection(playerYaw, travelYaw), playerYaw);
        Vec3 expected = directionOf(travelYaw);

        assertEquals(expected.x, actual.x, EPSILON, "world X for rail " + travelYaw);
        assertEquals(expected.z, actual.z, EPSILON, "world Z for rail " + travelYaw);
    }

    @Test
    @DisplayName("the two screen directions are exact opposites")
    void oppositeInputsGiveOppositeTravel() {
        float playerYaw = 33.0F;
        float right = 100.0F;
        float left = right - 180.0F;

        Vec3 movingRight = engineWorldVector(
                PlaneConstrainedInput.railProjection(playerYaw, right), playerYaw);
        Vec3 movingLeft = engineWorldVector(
                PlaneConstrainedInput.railProjection(playerYaw, left), playerYaw);

        assertEquals(-movingRight.x, movingLeft.x, EPSILON, "world X should invert");
        assertEquals(-movingRight.z, movingLeft.z, EPSILON, "world Z should invert");
    }

    @Test
    @DisplayName("facing along the rail reduces to plain forward walking")
    void alignedPlayerWalksStraightForward() {
        // The behaviour the old implementation produced by force-rotating the player: when the
        // avatar already faces the travel direction the projection must be pure forward.
        Vec2 move = PlaneConstrainedInput.railProjection(75.0F, 75.0F);

        assertEquals(0.0F, move.x, EPSILON, "no strafe component when aligned");
        assertEquals(1.0F, move.y, EPSILON, "full forward component when aligned");
    }

    @ParameterizedTest(name = "unit length preserved at player yaw {0}")
    @ValueSource(floats = {0.0F, 17.0F, 90.0F, 144.0F, 200.0F, 359.0F})
    @DisplayName("the vector stays unit length so speed never varies with facing")
    void projectionIsUnitLength(float playerYaw) {
        // Entity#getInputVector normalises anything longer than 1, which would silently scale
        // movement. Staying at exactly 1 keeps the walk speed constant in every direction.
        Vec2 move = PlaneConstrainedInput.railProjection(playerYaw, 21.0F);
        float length = Mth.sqrt(move.x * move.x + move.y * move.y);

        assertEquals(1.0F, length, EPSILON, "projection length at player yaw " + playerYaw);
        assertTrue(length <= 1.0F + EPSILON, "must not exceed 1, or the engine rescales it");
    }
}
