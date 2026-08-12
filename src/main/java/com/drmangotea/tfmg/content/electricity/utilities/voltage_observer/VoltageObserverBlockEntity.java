package com.drmangotea.tfmg.content.electricity.utilities.voltage_observer;


import com.drmangotea.tfmg.base.blocks.WallMountBlock;
import com.drmangotea.tfmg.content.electricity.base.ElectricBlockEntity;
import com.simibubi.create.foundation.blockEntity.ComparatorUtil;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

import static com.drmangotea.tfmg.content.electricity.utilities.voltage_observer.VoltageObserverBlock.POWERED;


public class VoltageObserverBlockEntity extends ElectricBlockEntity {

    boolean update = false;

    // Last values actually published to the world. onNetworkChanged fires on
    // every voltage or power move, which on a live network is most ticks, and
    // the observer used to rewrite its blockstate and poke all six neighbours
    // each time even when nothing it exposes had changed.
    private int lastComparatorLevel = -1;

    ObservedElectricBehaviour observedElectricBehaviour;

    public VoltageObserverBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        behaviours.add(observedElectricBehaviour = new ObservedElectricBehaviour(this));
    }


    @Override
    public void onNetworkChanged(int oldVoltage, int oldPower) {
        super.onNetworkChanged(oldVoltage, oldPower);
        update = true;
    }

    public int getComparatorOutput() {
        return ComparatorUtil.fractionToRedstoneLevel((double) getData().getVoltage() /250);
    }

    @Override
    public void tick() {
        super.tick();
        if(update){
            if (!level.isClientSide) {
                boolean powered = getData().getVoltage() != 0;
                int comparatorLevel = getComparatorOutput();

                // Write the blockstate only when the redstone output flips.
                // Read the real state rather than a cached copy, so a state
                // changed from outside is still corrected.
                if (getBlockState().getValue(POWERED) != powered)
                    level.setBlock(getBlockPos(), getBlockState().setValue(POWERED, powered), 2);
                // Still poke the neighbours when only the comparator reading
                // moved: a comparator tracks the voltage continuously and has
                // no other way to learn it changed.
                if (comparatorLevel != lastComparatorLevel) {
                    level.updateNeighborsAt(getBlockPos(), getBlockState().getBlock());
                    lastComparatorLevel = comparatorLevel;
                }
            }
            update = false;
        }
        if (observedElectricBehaviour != null) {
            observedElectricBehaviour.setObservedPos(getConnectedPos());
        }
    }

    @Override
    public boolean hasElectricitySlot(Direction direction) {
        return direction == getBlockState().getValue(WallMountBlock.FACING);
    }

    private BlockPos getConnectedPos() {
        return getBlockPos().relative(getBlockState().getValue(WallMountBlock.FACING));
    }
}
