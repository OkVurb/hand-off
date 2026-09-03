package com.studio.planeshift.common.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Course ice: low friction and translucent, so a slide reads as a slide before the player steps on
 * it.
 *
 * <p>Deliberately does <em>not</em> melt. Vanilla ice melts under light, which in a hand-placed
 * course means an ice run silently disappearing between two visits — a platformer surface has to
 * still be there when the player comes back to it.
 */
public class CourseIceBlock extends HalfTransparentBlock {

    public static final MapCodec<CourseIceBlock> CODEC = simpleCodec(CourseIceBlock::new);

    public CourseIceBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends HalfTransparentBlock> codec() {
        return CODEC;
    }
}
