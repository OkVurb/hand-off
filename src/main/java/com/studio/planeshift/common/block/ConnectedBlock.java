package com.studio.planeshift.common.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * A block that draws its edges only where it actually ends.
 *
 * <h2>Why this is possible without a connected-textures mod</h2>
 *
 * <p>No CTM mod has a 1.21.11 NeoForge build, and vanilla block models cannot see their
 * neighbours. But vanilla has done this for years by a different route: fences, glass panes and
 * redstone all carry boolean properties that are recomputed when a neighbour changes, and the
 * blockstate picks a different model for each combination. That is connected textures, arrived at
 * from the state side rather than the model side, and it needs nothing but the game.
 *
 * <h2>Why it is cheap here specifically</h2>
 *
 * <p>Full connected textures need 47 tiles, because a block can be seen from any angle and the
 * corners matter. This game locks the camera side-on, so exactly one face is ever visible and only
 * four edges of it exist: up, down, east, west. Sixteen combinations, and a course is built almost
 * entirely from flat walls where most blocks land on the one that draws no edges at all.
 *
 * <h2>What it fixes</h2>
 *
 * <p>The banding. {@code BlockTextureGen.lit} bakes a highlight on the top row and a shadow on the
 * bottom so a player can see where one block ends and the next begins — necessary, because
 * Minecraft shades whole faces and an unlit run of blocks is one flat slab. The cost was that a
 * wall put a shadow directly against a highlight every sixteen pixels and read as stripes; the
 * previous pass softened that ramp and got the average seam from 111 down to 54, but softening is
 * a compromise between two things that were never really in conflict.
 *
 * <p>They are not in conflict once the block knows whether it has a neighbour. An edge is drawn
 * where the material genuinely stops and omitted where it continues, which is what the lighting
 * was always trying to approximate. Interior blocks have no seam at all, and the outline of a
 * platform gets sharper rather than weaker.
 */
public class ConnectedBlock extends Block {

    public static final MapCodec<ConnectedBlock> CODEC = simpleCodec(ConnectedBlock::new);

    /**
     * The four edges of the visible face.
     *
     * <p>Not north/south: the lane runs along X and the camera looks down Z, so the two faces
     * those would describe are the one the player is looking through and the one behind the
     * block. Neither is ever seen, and giving them properties would quadruple the state count to
     * describe edges that cannot appear on screen.
     */
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty WEST = BooleanProperty.create("west");

    public ConnectedBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(UP, false).setValue(DOWN, false)
                .setValue(EAST, false).setValue(WEST, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(UP, DOWN, EAST, WEST);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return connect(defaultBlockState(), context.getLevel(), context.getClickedPos());
    }

    /**
     * Recomputes this block's edges against its neighbours.
     *
     * <p>Only ever connects to the same block. A castle wall meeting a brick wall is a real edge
     * between two materials and should be drawn as one; hiding it would make two different
     * surfaces read as one continuous thing, which is worse than the grid this exists to remove.
     */
    public BlockState connect(BlockState state, BlockGetter level, BlockPos pos) {
        return state
                .setValue(UP, sameBlock(level, pos.above()))
                .setValue(DOWN, sameBlock(level, pos.below()))
                .setValue(EAST, sameBlock(level, pos.east()))
                .setValue(WEST, sameBlock(level, pos.west()));
    }

    /**
     * Recomputes the edges whenever a neighbour changes.
     *
     * <p>This is what makes it live rather than a placement-time snapshot: build a wall upward and
     * each block below has to drop its top edge as the next one lands on it.
     */
    @Override
    protected BlockState updateShape(BlockState state,
                                     net.minecraft.world.level.LevelReader level,
                                     net.minecraft.world.level.ScheduledTickAccess tickAccess,
                                     BlockPos pos,
                                     Direction direction,
                                     BlockPos neighborPos,
                                     BlockState neighborState,
                                     net.minecraft.util.RandomSource random) {
        return connect(state, level, pos);
    }

    private boolean sameBlock(BlockGetter level, BlockPos pos) {
        return level.getBlockState(pos).is(this);
    }

    /**
     * The texture index for a connection state.
     *
     * <p>A four-bit mask, and the numbering is the contract between this class and the generator
     * that draws the sixteen sheets: bit 0 up, bit 1 down, bit 2 west, bit 3 east. Fifteen is a
     * block surrounded on all four sides and is the one with no edges drawn at all — in a wall of
     * any size it is most of them.
     */
    public static int mask(BlockState state) {
        int m = 0;
        if (state.getValue(UP)) {
            m |= 1;
        }
        if (state.getValue(DOWN)) {
            m |= 2;
        }
        if (state.getValue(WEST)) {
            m |= 4;
        }
        if (state.getValue(EAST)) {
            m |= 8;
        }
        return m;
    }

    /** Direction order matching {@link #mask}, for the generator and for tests. */
    public static final Direction[] EDGES = {
            Direction.UP, Direction.DOWN, Direction.WEST, Direction.EAST};
}
