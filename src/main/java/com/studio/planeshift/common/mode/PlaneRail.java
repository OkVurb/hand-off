package com.studio.planeshift.common.mode;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec3;

/**
 * The movement plane for 2.5D play (Design Bible, "2.5D camera specification").
 *
 * <p>Rails are axis-aligned by design: Minecraft's collision grid, block silhouettes and
 * corridor authoring all read best along X or Z. {@code planeCoord} is the coordinate on
 * the <em>depth</em> axis (the horizontal axis that is not the travel axis) that the
 * plane passes through; {@code halfDepth} is the permitted drift either side of it.
 *
 * <p>Bible: "Keep depth drift within 0.05 block after collision resolution. Author
 * playable corridors around 1.5 blocks deep unless a set piece deliberately widens them."
 *
 * @param travelAxis   horizontal axis the course runs along ({@code X} or {@code Z})
 * @param planeCoord   centre of the corridor on the depth axis
 * @param halfDepth    permitted drift either side of {@code planeCoord}, in blocks
 * @param lookPositive whether the camera sits on the positive side of the depth axis
 */
public record PlaneRail(Direction.Axis travelAxis, double planeCoord, double halfDepth, boolean lookPositive) {

    /** Default corridor half-depth: a 1.5-block-deep corridor around the plane. */
    public static final double DEFAULT_HALF_DEPTH = 0.75D;
    /** Post-collision drift tolerance before the server snaps a player back. */
    public static final double DRIFT_TOLERANCE = 0.05D;

    public static final Codec<PlaneRail> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Direction.Axis.CODEC.fieldOf("travel_axis").forGetter(PlaneRail::travelAxis),
            Codec.DOUBLE.fieldOf("plane_coord").forGetter(PlaneRail::planeCoord),
            Codec.doubleRange(0.05D, 8.0D).optionalFieldOf("half_depth", DEFAULT_HALF_DEPTH)
                    .forGetter(PlaneRail::halfDepth),
            Codec.BOOL.optionalFieldOf("look_positive", true).forGetter(PlaneRail::lookPositive)
    ).apply(instance, PlaneRail::new));

    public static final StreamCodec<ByteBuf, PlaneRail> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.idMapper(i -> Direction.Axis.VALUES[i], Enum::ordinal), PlaneRail::travelAxis,
            ByteBufCodecs.DOUBLE, PlaneRail::planeCoord,
            ByteBufCodecs.DOUBLE, PlaneRail::halfDepth,
            ByteBufCodecs.BOOL, PlaneRail::lookPositive,
            PlaneRail::new);

    public PlaneRail {
        if (travelAxis == Direction.Axis.Y) {
            throw new IllegalArgumentException("PlaneRail travel axis must be horizontal (X or Z)");
        }
    }

    public Direction.Axis depthAxis() {
        return travelAxis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
    }

    /** The position's coordinate along the depth axis. */
    public double depthOf(Vec3 pos) {
        return depthAxis() == Direction.Axis.Z ? pos.z : pos.x;
    }

    /** How far outside the permitted corridor the position is (0 when inside). */
    public double driftBeyondCorridor(Vec3 pos) {
        return Math.max(0.0D, Math.abs(depthOf(pos) - planeCoord) - halfDepth);
    }

    /** Snaps a position onto the plane centre, preserving travel and vertical coordinates. */
    public Vec3 snapToPlane(Vec3 pos) {
        return depthAxis() == Direction.Axis.Z
                ? new Vec3(pos.x, pos.y, planeCoord)
                : new Vec3(planeCoord, pos.y, pos.z);
    }

    /** Removes the depth component from a velocity so momentum maps onto the plane. */
    public Vec3 flattenVelocity(Vec3 velocity) {
        return depthAxis() == Direction.Axis.Z
                ? new Vec3(velocity.x, velocity.y, 0.0D)
                : new Vec3(0.0D, velocity.y, velocity.z);
    }

    /**
     * Camera yaw (degrees) that gives the side-on read: the camera sits on the
     * {@code lookPositive} side of the depth axis and looks at the plane.
     * Minecraft yaw: 0 = +Z, 90 = -X, 180 = -Z, -90/270 = +X.
     */
    public float sideOnCameraYaw() {
        if (depthAxis() == Direction.Axis.Z) {
            // Camera at +Z looks toward -Z (yaw 180); camera at -Z looks toward +Z (yaw 0).
            return lookPositive ? 180.0F : 0.0F;
        }
        // Depth axis X: camera at +X looks toward -X (yaw 90); at -X looks toward +X (yaw -90).
        return lookPositive ? 90.0F : -90.0F;
    }

    /** Creates a rail from a gate position and the horizontal direction the gate faces. */
    public static PlaneRail fromGate(Vec3 gateCenter, Direction gateFacing) {
        Direction.Axis travel = gateFacing.getClockWise().getAxis();
        Direction.Axis depth = travel == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
        double coord = depth == Direction.Axis.Z ? gateCenter.z : gateCenter.x;
        boolean positive = gateFacing.getAxisDirection() == Direction.AxisDirection.POSITIVE;
        return new PlaneRail(travel, coord, DEFAULT_HALF_DEPTH, positive);
    }
}
