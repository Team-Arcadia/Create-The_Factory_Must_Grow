package com.drmangotea.tfmg.content.machinery.metallurgy.casting_basin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import net.createmod.catnip.platform.NeoForgeCatnipServices;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

import static net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING;

public class CastingBasinRenderer extends SafeBlockEntityRenderer<CastingBasinBlockEntity> {
    public CastingBasinRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    protected void renderSafe(CastingBasinBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        if (be.tank.isEmpty())
            return;
        BlockState blockState = be.getBlockState();
        // Render fluid Y-max proportional to fill ratio, clamped inside the
        // basin geometry (0.1..0.9). The previous code divided fluidLevel by
        // a constant 400 which was calibrated on the old 144 mB tank — after
        // raising the tank capacity to 1000 mB, a full basin would render
        // 2.5 blocks of fluid spilling out the top.
        int capacity = Math.max(1, be.tank.getCapacity());
        float fillFraction = Math.min(1f, be.fluidLevel.getValue(partialTicks) / (float) capacity);
        float maxY = 0.1f + fillFraction * 0.8f;
        NeoForgeCatnipServices.FLUID_RENDERER.renderFluidBox(be.tank.getFluid(), 0.1f, 0.1f, 0.1f, 0.9f, maxY, 0.9f, buffer, ms, light, false, false);
        if (be.flowTimer > 0) {

            Direction facing = blockState.getValue(FACING);
            if (facing == Direction.NORTH) {
                NeoForgeCatnipServices.FLUID_RENDERER.renderFluidBox(be.tank.getFluid(), (7 / 16f), (8 / 16f), (8 / 16f), (9 / 16f), (9 / 16f), (14 / 16f), buffer, ms, light, false, false);
                NeoForgeCatnipServices.FLUID_RENDERER.renderFluidBox(be.tank.getFluid(), (7 / 16f), (1 / 16f), (8 / 16f), (9 / 16f), (8 / 16f), (10 / 16f), buffer, ms, light, false, false);
            }
            if (facing == Direction.SOUTH) {
                NeoForgeCatnipServices.FLUID_RENDERER.renderFluidBox(be.tank.getFluid(), (7 / 16f), (2 / 16f), (6 / 16f), (9 / 16f), (9 / 16f), (8 / 16f), buffer, ms, light, false, false);
                NeoForgeCatnipServices.FLUID_RENDERER.renderFluidBox(be.tank.getFluid(), (7 / 16f), (8 / 16f), (2 / 16f), (9 / 16f), (9 / 16f), (6 / 16f), buffer, ms, light, false, false);
            }
            if (facing == Direction.WEST) {
                NeoForgeCatnipServices.FLUID_RENDERER.renderFluidBox(be.tank.getFluid(), (8 / 16f), (2 / 16f), (7 / 16f), (10 / 16f), (9 / 16f), (9 / 16f), buffer, ms, light, false, false);
                NeoForgeCatnipServices.FLUID_RENDERER.renderFluidBox(be.tank.getFluid(), (10 / 16f), (8 / 16f), (7 / 16f), (14 / 16f), (9 / 16f), (9 / 16f), buffer, ms, light, false, false);
            }
            if (facing == Direction.EAST) {
                NeoForgeCatnipServices.FLUID_RENDERER.renderFluidBox(be.tank.getFluid(), (6 / 16f), (2 / 16f), (7 / 16f), (8 / 16f), (9 / 16f), (9 / 16f), buffer, ms, light, false, false);
                NeoForgeCatnipServices.FLUID_RENDERER.renderFluidBox(be.tank.getFluid(), (2 / 16f), (8 / 16f), (7 / 16f), (6 / 16f), (9 / 16f), (9 / 16f), buffer, ms, light, false, false);
            }

        }

    }
}
