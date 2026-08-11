package com.drmangotea.tfmg.content.engines;

import com.drmangotea.tfmg.base.lang.TFMGLang;
import com.drmangotea.tfmg.registry.TFMGDataComponents;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;

import com.tterrag.registrate.util.entry.FluidEntry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;


import java.util.List;

public class FluidContainingItem extends Item {

    public final FluidEntry<?> fluid;

    public static final int CAPACITY = 4000;

    public FluidContainingItem(Properties p_41383_, FluidEntry<?> fluid) {
        super(p_41383_);
        this.fluid = fluid;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(TFMGLang.translateDirect("tooltip.fluid_item", stack.getOrDefault(TFMGDataComponents.AMOUNT, 0))
                .withStyle(ChatFormatting.GREEN)
        );
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        if(!stack.has(TFMGDataComponents.AMOUNT))
            return false;

        return stack.getOrDefault(TFMGDataComponents.AMOUNT, 0) > 0;
    }

    @Override
    public int getBarColor(ItemStack stack) {
        if(!stack.has(TFMGDataComponents.AMOUNT))
            stack.set(TFMGDataComponents.AMOUNT, 0);

        return 0xC7C4A4;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        if(!stack.has(TFMGDataComponents.AMOUNT))
            stack.set(TFMGDataComponents.AMOUNT, 0);

        return Math.round( 13* ((float)stack.getOrDefault(TFMGDataComponents.AMOUNT, 0)/(float)CAPACITY));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {

        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        ItemStack stack = context.getItemInHand();


        if (context.getPlayer().isShiftKeyDown()&&stack.getOrDefault(TFMGDataComponents.AMOUNT, 0) > 0) {

            level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1f, 1f);
            stack.set(TFMGDataComponents.AMOUNT, 0);
            return InteractionResult.SUCCESS;
        }

        // Any block that exposes a fluid handler will do.
        //
        // This used to accept Create's own FluidTankBlockEntity and nothing
        // else, so the bottle could not be filled from a TFMG steel tank, a
        // vat, a pipe or a spout — every container a TFMG player actually
        // builds. The item then sat permanently at 0 mB with no way to fill it
        // and no hint that a vanilla-adjacent Create tank was the one thing
        // that worked.
        if (context.getPlayer() == null)
            return InteractionResult.PASS;

        IFluidHandler handler = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, context.getClickedFace());
        if (handler == null)
            handler = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, null);
        if (handler == null)
            return InteractionResult.PASS;

        int space = CAPACITY - stack.getOrDefault(TFMGDataComponents.AMOUNT, 0);
        if (space <= 0 || context.getPlayer().getCooldowns().isOnCooldown(stack.getItem()))
            return InteractionResult.PASS;

        // Ask for OUR fluid by name: draining by amount alone would happily
        // pull diesel into a cooling fluid bottle out of a multi-tank block.
        FluidStack drained = handler.drain(new FluidStack(fluid.get(), space), IFluidHandler.FluidAction.SIMULATE);
        if (drained.isEmpty())
            return InteractionResult.PASS;

        drained = handler.drain(new FluidStack(fluid.get(), space), IFluidHandler.FluidAction.EXECUTE);
        if (drained.isEmpty())
            return InteractionResult.PASS;

        level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1f, 1f);
        stack.set(TFMGDataComponents.AMOUNT, stack.getOrDefault(TFMGDataComponents.AMOUNT, 0) + drained.getAmount());
        context.getPlayer().getCooldowns().addCooldown(stack.getItem(), 20);

        return InteractionResult.SUCCESS;
    }
}
