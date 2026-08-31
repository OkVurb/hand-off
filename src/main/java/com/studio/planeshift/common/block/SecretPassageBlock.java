package com.studio.planeshift.common.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.Block;

/**
 * Secret Passage — "Looks like an ordinary brick wall, but the player can walk right
 * through it to hidden bonus rooms."
 *
 * <p>Course authors use this for fake walls and optional paths. It uses the same face
 * texture as the brick block so it reads as solid until the player stumbles through.
 */
public class SecretPassageBlock extends Block {

    public static final MapCodec<SecretPassageBlock> CODEC = simpleCodec(SecretPassageBlock::new);

    public SecretPassageBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }
}
