package com.drmangotea.tfmg.base.blocks;

import com.drmangotea.tfmg.content.electricity.base.IElectric;
import com.drmangotea.tfmg.content.electricity.base.VoltageAlteringBlockEntity;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
        if (!moved && !state.isAir() && oldState != state) {
            ElectricRotationHook.onRotated(level, pos);
        }
    }

    /** Shared helper so TFMGHorizontalDirectionalBlock and TFMGDirectionalBlock can both
     *  reconnect the rotated block and its 6 neighbours. We avoid calling onRemoved
     *  because it tears down the network entry which then causes a race with the
     *  neighbour reconnect — Asuka retest of ALT-01 showed this made the bug worse. */
    public static final class ElectricRotationHook {
        private ElectricRotationHook() {}

        /** Called from onPlace. Forces the rotated/placed BE and its 6 neighbours to
         *  re-run onPlaced on the next tick so the new slot direction is picked up
         *  without dropping the existing network. */
        public static void onRotated(Level level, BlockPos pos) {
            if (level.getBlockEntity(pos) instanceof IElectric ie) {
                ie.getData().connectNextTick = true;
                ie.getData().updateNextTick = true;
                if (ie instanceof VoltageAlteringBlockEntity vae)
                    vae.updateInFront = true;
            }
            for (Direction d : Direction.values()) {
                if (level.getBlockEntity(pos.relative(d)) instanceof IElectric n) {
                    n.getData().connectNextTick = true;
                    n.getData().updateNextTick = true;
                    if (n instanceof VoltageAlteringBlockEntity vae)
                        vae.updateInFront = true;
                }
            }
        }
    }
}
