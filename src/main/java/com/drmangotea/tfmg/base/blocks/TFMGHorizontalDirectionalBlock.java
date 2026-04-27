package com.drmangotea.tfmg.base.blocks;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

public class TFMGHorizontalDirectionalBlock extends HorizontalDirectionalBlock implements IWrenchable {
    public static final MapCodec<TFMGHorizontalDirectionalBlock> CODEC = simpleCodec(TFMGHorizontalDirectionalBlock::new);
    public TFMGHorizontalDirectionalBlock(Properties p_54120_) {
        super(p_54120_);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
        super.createBlockStateDefinition(builder);
    }
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        return this.defaultBlockState().setValue(FACING, pContext.getHorizontalDirection().getOpposite());
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
        super.onPlace(state, level, pos, oldState, moved);
        if (!moved && !state.isAir() && oldState != state) {
            TFMGDirectionalBlock.ElectricRotationHook.onRotated(level, pos);
        }
    }

    @Override
    public BlockState updateAfterWrenched(BlockState newState, UseOnContext context) {
        TFMGDirectionalBlock.ElectricRotationHook.beforeRotation(context.getLevel(), context.getClickedPos());
        return Block.updateFromNeighbourShapes(newState, context.getLevel(), context.getClickedPos());
    }
}
