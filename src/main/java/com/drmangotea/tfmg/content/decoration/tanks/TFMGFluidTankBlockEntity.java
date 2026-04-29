package com.drmangotea.tfmg.content.decoration.tanks;

import com.drmangotea.tfmg.content.decoration.tanks.aluminum.AluminumTankBlock;
import com.drmangotea.tfmg.content.decoration.tanks.cast_iron.CastIronTankBlock;
import com.drmangotea.tfmg.mixin.accessor.FluidTankBlockEntityAccessor;
import com.drmangotea.tfmg.registry.TFMGBlockEntities;
import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import static java.lang.Math.abs;

public class TFMGFluidTankBlockEntity extends FluidTankBlockEntity {

    public TFMGFluidTankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                TFMGBlockEntities.TFMG_FLUID_TANK.get(),
                (be, context) -> {
                    if (((FluidTankBlockEntityAccessor) be).tfmg$getFluidCapability() == null)
                        ((FluidTankBlockEntityAccessor) be).tfmg$refreshCapability();
                    return ((FluidTankBlockEntityAccessor) be).tfmg$getFluidCapability();
                }
        );
    }

    // Create's FluidTankBlock.isTank(state) checks for FluidTankBlock instance,
    // which our alu/cast iron tanks (extends Block, not FluidTankBlock) fail.
    // The result was that BOTTOM/TOP/SHAPE state values never updated past the
    // first tier of a stack: every block stayed at BOTTOM=true,TOP=true and the
    // model rendered as if it were a stand-alone tank.
    public static boolean isAnyTank(BlockState state) {
        return FluidTankBlock.isTank(state)
                || state.getBlock() instanceof AluminumTankBlock
                || state.getBlock() instanceof CastIronTankBlock;
    }

    @Override
    public void removeController(boolean keepFluids) {
        boolean wasClientSide = level == null || level.isClientSide;
        super.removeController(keepFluids);
        if (wasClientSide)
            return;
        BlockState state = getBlockState();
        if (isAnyTank(state) && !FluidTankBlock.isTank(state)) {
            state = state.setValue(FluidTankBlock.BOTTOM, true);
            state = state.setValue(FluidTankBlock.TOP, true);
            state = state.setValue(FluidTankBlock.SHAPE,
                    ((FluidTankBlockEntityAccessor) this).tfmg$getWindow()
                            ? FluidTankBlock.Shape.WINDOW
                            : FluidTankBlock.Shape.PLAIN);
            getLevel().setBlock(worldPosition, state,
                    Block.UPDATE_CLIENTS | Block.UPDATE_INVISIBLE | Block.UPDATE_KNOWN_SHAPE);
        }
    }

    @Override
    public void notifyMultiUpdated() {
        BlockState state = this.getBlockState();
        if (isAnyTank(state)) {
            state = state.setValue(FluidTankBlock.BOTTOM, getController().getY() == getBlockPos().getY());
            state = state.setValue(FluidTankBlock.TOP, getController().getY() + height - 1 == getBlockPos().getY());
            level.setBlock(getBlockPos(), state, Block.UPDATE_CLIENTS | Block.UPDATE_INVISIBLE);
        }
        if (isController())
            setWindows(window);
        onFluidStackChanged(tankInventory.getFluid());
        updateBoilerState();
        setChanged();
    }

    @Override
    public void setWindows(boolean window) {
        this.window = window;
        for (int yOffset = 0; yOffset < height; yOffset++) {
            for (int xOffset = 0; xOffset < width; xOffset++) {
                for (int zOffset = 0; zOffset < width; zOffset++) {
                    BlockPos pos = this.worldPosition.offset(xOffset, yOffset, zOffset);
                    BlockState blockState = level.getBlockState(pos);
                    if (!isAnyTank(blockState))
                        continue;
                    FluidTankBlock.Shape shape = FluidTankBlock.Shape.PLAIN;
                    if (window) {
                        if (width == 1)
                            shape = FluidTankBlock.Shape.WINDOW;
                        if (width == 2)
                            shape = xOffset == 0
                                    ? zOffset == 0 ? FluidTankBlock.Shape.WINDOW_NW : FluidTankBlock.Shape.WINDOW_SW
                                    : zOffset == 0 ? FluidTankBlock.Shape.WINDOW_NE : FluidTankBlock.Shape.WINDOW_SE;
                        if (width == 3 && abs(abs(xOffset) - abs(zOffset)) == 1)
                            shape = FluidTankBlock.Shape.WINDOW;
                    }
                    level.setBlock(pos, blockState.setValue(FluidTankBlock.SHAPE, shape),
                            Block.UPDATE_CLIENTS | Block.UPDATE_INVISIBLE | Block.UPDATE_KNOWN_SHAPE);
                    level.getChunkSource().getLightEngine().checkBlock(pos);
                }
            }
        }
    }
}
