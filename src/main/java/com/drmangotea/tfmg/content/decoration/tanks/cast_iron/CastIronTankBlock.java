package com.drmangotea.tfmg.content.decoration.tanks.cast_iron;


import com.drmangotea.tfmg.registry.TFMGBlockEntities;
import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class CastIronTankBlock extends FluidTankBlock {

    public static CastIronTankBlock regular(Properties p_i48440_1_) {
        return new CastIronTankBlock(p_i48440_1_, false);
    }

    protected CastIronTankBlock(Properties p_i48440_1_, boolean creative) {
        super(p_i48440_1_, creative);
    }

    @Override
    public BlockEntityType<? extends FluidTankBlockEntity> getBlockEntityType() {
        return TFMGBlockEntities.TFMG_FLUID_TANK.get();
    }
}
