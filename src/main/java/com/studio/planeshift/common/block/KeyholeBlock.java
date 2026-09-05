package com.studio.planeshift.common.block;

import com.mojang.serialization.MapCodec;
import com.studio.planeshift.common.registry.ModItems;
import com.studio.planeshift.common.registry.ModSounds;
import com.studio.planeshift.server.CourseCompletionService;
import com.studio.planeshift.server.CourseScoringService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The other way out of a ghost house.
 *
 * <p>A ghost house was a theme with no objective of its own. It looked different and it played
 * exactly like everything else — run right, touch the pole — which is why the navigation-puzzle
 * ruleset added to {@code CourseLesson} had nothing to actually navigate toward. A key hidden
 * somewhere in the course and a keyhole near the exit gives the theme the thing it has always been
 * about: the way out is not the way forward, and finding it is the level.
 *
 * <p>Completing through the keyhole is worth more than the pole. It has to be — a secret exit that
 * pays the same as the obvious one is a strictly worse choice, and nobody would ever take it twice.
 */
public class KeyholeBlock extends Block {

    public static final MapCodec<KeyholeBlock> CODEC = simpleCodec(KeyholeBlock::new);

    /** Set once the key has turned, so the course cannot be completed through it twice. */
    public static final BooleanProperty UNLOCKED = BooleanProperty.create("unlocked");

    /** Score for taking the hidden route, on top of everything the course already paid. */
    private static final int SECRET_EXIT_BONUS = 5000;

    private static final VoxelShape SHAPE = Shapes.box(0.15D, 0.0D, 0.15D, 0.85D, 1.0D, 0.85D);

    public KeyholeBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(UNLOCKED, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(UNLOCKED);
    }

    @Override
    protected VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level,
                                  BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /**
     * No collision at all.
     *
     * <p>The course is completed by walking <em>into</em> this block, so a solid door would stop
     * the player doing the one thing it exists for. Declared on the block rather than through the
     * block properties because the reason is about this block's behaviour, and a reader looking at
     * entityInside below should not have to go to the registry to find out why it fires.
     */
    @Override
    protected VoxelShape getCollisionShape(BlockState state, net.minecraft.world.level.BlockGetter level,
                                           BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    /**
     * Walking into it with the key finishes the course.
     *
     * <p>Contact rather than a right-click, deliberately. The player is mid-run in a platformer
     * with the mouse locked; asking them to aim at a block and press a button is a control scheme
     * from a different game. Carrying the key to the place is the whole puzzle, so arriving is the
     * interaction.
     */
    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
                                InsideBlockEffectApplier applier, boolean wasInside) {
        if (level.isClientSide() || state.getValue(UNLOCKED)
                || !(entity instanceof ServerPlayer player)) {
            return;
        }
        if (!consumeKey(player)) {
            // No key: say so once, quietly, rather than doing nothing. A door that is silent when
            // you walk into it is indistinguishable from a door that is broken.
            if (!wasInside) {
                level.playSound(null, pos, ModSounds.QUESTION_BUMP.get(),
                        SoundSource.BLOCKS, 0.7F, 0.6F);
                player.sendSystemMessage(Component.translatable("message.planeshift.keyhole_locked"));
            }
            return;
        }

        level.setBlock(pos, state.setValue(UNLOCKED, true), Block.UPDATE_ALL);
        level.playSound(null, pos, ModSounds.WARP.get(), SoundSource.BLOCKS, 1.0F, 1.2F);
        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.END_ROD,
                    pos.getX() + 0.5D, pos.getY() + 0.6D, pos.getZ() + 0.5D,
                    18, 0.3D, 0.4D, 0.3D, 0.03D);
        }

        // Paid before the slide, for the same reason the flagpole bonus is: onComplete reads the
        // running score to work out the end-of-course bonuses and then resets it.
        CourseScoringService.addScore(player, SECRET_EXIT_BONUS);
        CourseCompletionService.beginSlide(player, pos);
    }

    /** Takes one key from the player's inventory, or reports that they have none. */
    private static boolean consumeKey(ServerPlayer player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(ModItems.COURSE_KEY.get())) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }
}
