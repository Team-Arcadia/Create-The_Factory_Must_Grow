package com.drmangotea.tfmg.content.decoration.concrete;

import com.drmangotea.tfmg.base.TFMGShapes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Dried counterpart of {@link RebarFloorBlock}: same footprint, solid concrete
 * instead of an open cage. Without it a concretelogged rebar floor dried into a
 * full rebar concrete cube and silently lost its shape.
 */
public class RebarConcreteFloorBlock extends Block {

    public RebarConcreteFloorBlock(Properties p_49795_) {
        super(p_49795_);
    }

    @Override
    public VoxelShape getShape(BlockState p_60555_, BlockGetter p_60556_, BlockPos p_60557_, CollisionContext p_60558_) {
        return TFMGShapes.REBAR_FLOOR;
    }
}
