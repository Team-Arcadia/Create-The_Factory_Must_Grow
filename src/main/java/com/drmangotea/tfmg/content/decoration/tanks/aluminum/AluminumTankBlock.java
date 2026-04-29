package com.drmangotea.tfmg.content.decoration.tanks.aluminum;


import com.drmangotea.tfmg.registry.TFMGBlockEntities;
import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class AluminumTankBlock extends FluidTankBlock {

    public static AluminumTankBlock regular(Properties p_i48440_1_) {
        return new AluminumTankBlock(p_i48440_1_, false);
    }

    protected AluminumTankBlock(Properties p_i48440_1_, boolean creative) {
        super(p_i48440_1_, creative);
    }

    @Override
    public BlockEntityType<? extends FluidTankBlockEntity> getBlockEntityType() {
        return TFMGBlockEntities.TFMG_FLUID_TANK.get();
    }
}
