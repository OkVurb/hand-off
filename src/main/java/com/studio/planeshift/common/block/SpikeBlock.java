package com.studio.planeshift.common.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Spike hazard: damages any entity that touches it.
 */
public class SpikeBlock extends Block {

    public static final MapCodec<SpikeBlock> CODEC = simpleCodec(SpikeBlock::new);
    public static final float DAMAGE = 4.0F;

    public SpikeBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    /**
     * {@code isInside} is true only when the entity's box actually overlaps this block
     * this tick. Damage pacing is left to the victim's own hurt-immunity window, so
     * touching several spikes at once still only costs one hit — a fixed
     * {@code gameTime % 20} gate would instead delay the first hit by up to a second
     * and let simultaneous spikes stack.
     */
    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
                                InsideBlockEffectApplier applier, boolean isInside) {
        if (level.isClientSide() || !isInside) {
            return;
        }
        entity.hurtServer((ServerLevel) level, level.damageSources().generic(), DAMAGE);
    }
}
