package com.drmangotea.tfmg.content.decoration.concrete;

import com.drmangotea.tfmg.base.TFMGShapes;
import com.drmangotea.tfmg.base.blocks.TFMGDirectionalBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Dried counterpart of {@link RebarPillarBlock}: same footprint, solid concrete
 * instead of an open cage. Without it a concretelogged rebar pillar dried into a
 * full rebar concrete cube and silently lost its shape.
 */
public class RebarConcretePillarBlock extends TFMGDirectionalBlock {

    public RebarConcretePillarBlock(Properties p_49795_) {
        super(p_49795_);
    }

    @Override
    public VoxelShape getShape(BlockState p_60555_, BlockGetter p_60556_, BlockPos p_60557_, CollisionContext p_60558_) {
        return TFMGShapes.REBAR_PILLAR.get(p_60555_.getValue(FACING));
    }
}
