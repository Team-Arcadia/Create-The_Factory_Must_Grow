package com.drmangotea.tfmg.base.blocks;

import com.drmangotea.tfmg.content.electricity.base.IElectric;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

public class TFMGDirectionalBlock extends DirectionalBlock implements IWrenchable {

    public TFMGDirectionalBlock(Properties p_54120_) {
        super(p_54120_);
    }
    public static final MapCodec<TFMGDirectionalBlock> CODEC = simpleCodec(TFMGDirectionalBlock::new);
    @Override
    protected MapCodec<? extends DirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
        super.createBlockStateDefinition(builder);
    }
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        return this.defaultBlockState().setValue(FACING, pContext.getNearestLookingDirection().getOpposite());
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
        super.onPlace(state, level, pos, oldState, moved);
        if (!moved && oldState.getBlock() == state.getBlock() && oldState != state) {
            if (level.getBlockEntity(pos) instanceof IElectric ie) {
                ie.getData().connectNextTick = true;
            }
        }
    }
}
