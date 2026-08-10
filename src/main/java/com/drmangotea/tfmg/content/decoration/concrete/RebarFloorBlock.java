package com.drmangotea.tfmg.content.decoration.concrete;

import com.drmangotea.tfmg.base.TFMGShapes;
import com.drmangotea.tfmg.registry.TFMGBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class RebarFloorBlock extends SimpleConcreteloggedBlock {
    public RebarFloorBlock(Properties p_49795_) {
        super(p_49795_);
    }

    @Override
    public VoxelShape getShape(BlockState p_60555_, BlockGetter p_60556_, BlockPos p_60557_, CollisionContext p_60558_) {
        return TFMGShapes.REBAR_FLOOR;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource randomSource) {
        // Overrides the plain rebar concrete cube inherited from
        // SimpleConcreteloggedBlock, which dried the floor into a full block.
        tickDrying(level, state, TFMGBlocks.REBAR_CONCRETE_FLOOR.getDefaultState(), pos, randomSource);
    }
}
