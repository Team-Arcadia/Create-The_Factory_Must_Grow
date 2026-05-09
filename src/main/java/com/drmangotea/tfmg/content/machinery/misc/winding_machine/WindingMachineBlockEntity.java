package com.drmangotea.tfmg.content.machinery.misc.winding_machine;

import com.drmangotea.tfmg.base.lang.TFMGLang;
import com.drmangotea.tfmg.base.lang.TFMGTexts;
import com.drmangotea.tfmg.recipes.WindingRecipe;
import com.drmangotea.tfmg.registry.*;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.item.SmartInventory;

import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.animation.LerpedFloat;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;

import java.util.List;
import java.util.Optional;

import static com.drmangotea.tfmg.content.machinery.misc.winding_machine.WindingMachineBlock.POWERED;
import static com.simibubi.create.content.kinetics.base.HorizontalKineticBlock.HORIZONTAL_FACING;

public class WindingMachineBlockEntity extends KineticBlockEntity implements IHaveGoggleInformation {

    LerpedFloat spoolSpeed = LerpedFloat.linear();
    float angle;
    public SmartInventory inventory;
    public ItemStack spool = ItemStack.EMPTY;
    public WindingRecipe recipe;
    public int amountWinded = 0;
    public boolean update = false;

    protected ScrollValueBehaviour turnPercentage;

    public WindingMachineBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        setLazyTickRate(10);
        inventory = new SmartInventory(1, this)
                .withMaxStackSize(1)
                .whenContentsChanged(i -> this.onContentsChanged());

    }


    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                TFMGBlockEntities.WINDING_MACHINE.get(),
                (be, context) -> be.inventory
        );
    }


    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        int max = 100;
        turnPercentage = new ScrollValueBehaviour(TFMGLang.translateDirect("winding_machine.turn_percentage"),
                this, new WindingMachineValueBox());
        turnPercentage.between(1, max);
        turnPercentage.value = 20;
        behaviours.add(turnPercentage);

    }

    public void onContentsChanged() {

        findRecipe();
        if (inventory.isEmpty())
            amountWinded = 0;
    }

    public void findRecipe() {

        Optional<RecipeHolder<WindingRecipe>> optional = TFMGRecipeTypes.WINDING.find(new RecipeWrapper(inventory), level);
        Optional<RecipeHolder<WindingRecipe>> assemblyRecipe = SequencedAssemblyRecipe.getRecipe(this.level, new RecipeWrapper(inventory), TFMGRecipeTypes.WINDING.getType(), WindingRecipe.class);

        if (assemblyRecipe.isPresent()) {
            recipe = assemblyRecipe.get().value();
            return;
        }
        if (optional.isEmpty()) {
            recipe = null;
            return;
        }
        WindingRecipe windingRecipe = optional.get().value();

        if (windingRecipe.getIngredient().test(inventory.getItem(0))) {
            recipe = windingRecipe;
        }
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        onContentsChanged();

        if (spool.is(TFMGItems.EMPTY_SPOOL.get()) && !getBlockState().getValue(POWERED)) {
            level.setBlock(getBlockPos(), getBlockState().setValue(POWERED, true), 2);
            update = true;
        }
        if (!spool.is(TFMGItems.EMPTY_SPOOL.get()) && getBlockState().getValue(POWERED)) {
            level.setBlock(getBlockPos(), getBlockState().setValue(POWERED, false), 2);
            update = true;
        }
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        TFMGTexts.header("winding_machine")
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip, 1);

        if (!spool.isEmpty()) {
            TFMGLang.text(spool.getDisplayName().getString().replace("[","").replace("]",""))
                    .color(spool.getBarColor())
                    .forGoggles(tooltip);
            if(spool.get(TFMGDataComponents.SPOOL_AMOUNT)!=null)
                TFMGTexts.turnsLeft(spool.getOrDefault(TFMGDataComponents.SPOOL_AMOUNT, 0))
                    .color(spool.getBarColor())
                    .forGoggles(tooltip);

        if (recipe != null)
            TFMGTexts.progress(amountWinded + "/" + recipe.getProcessingDuration())
                    .color(spool.getBarColor())
                    .forGoggles(tooltip);
        }
        return true;
    }

    public void destroy() {
        super.destroy();
        ItemHelper.dropContents(level, worldPosition, inventory);
        Containers.dropItemStack(getLevel(), getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ(), spool);
    }

    @Override
    public void tick() {
        super.tick();
        performRecipe();
        if (update) {
            level.updateNeighborsAt(getBlockPos(), getBlockState().getBlock());
            update = false;
        }

        if (level.isClientSide)
            manageRotation();
    }

    public void performRecipe() {
        //Change these if you want. Just fallbacks if the component is null.
        int defaultResistance = 0;
        int defaultSpoolAmount = 0;
        int defaultCoilTurns = 0;

        if (getSpeed() == 0)
            return;

        // EARLY GUARD — short-circuit the whole tick when a resistor or coil
        // already meets its target. Both the resistor branch and the coil
        // branch below test '< target' but the generic recipe branch at the
        // bottom of this method does not — without this guard it kept
        // draining the spool every tick after the target was reached. The
        // user reported 5% target on a resistor consuming 90 spool durability
        // instead of 50; this guard runs first and caps it at exactly 50.
        ItemStack guardItem = inventory.getItem(0);
        int target = turnPercentage.getValue() * 10;
        if (guardItem.is(TFMGBlocks.RESISTOR.asItem())
                && guardItem.getOrDefault(TFMGDataComponents.RESISTANCE, defaultResistance) >= target) {
            return;
        }
        if ((guardItem.is(TFMGItems.ELECTROMAGNETIC_COIL.get()) || guardItem.is(TFMGBlocks.LARGE_COIL.get().asItem()))
                && guardItem.getOrDefault(TFMGDataComponents.COIL_TURNS, defaultCoilTurns) >= target) {
            return;
        }
        // Apply set() to a fresh ItemStack reference and write it back via
        // setStackInSlot so SmartInventory marks the slot dirty and the new
        // component value is sync'd / saved. The previous code mutated
        // inventory.getItem(0) in place, which left the slot's cached
        // serialised form stale — the user reported pulling out 10
        // resistors at the same target percentage and getting 5 different
        // resistance values instead of a single one, because the slot
        // would resync with whichever NBT the client last saw before the
        // server-side mutation propagated.
        if ((inventory.getItem(0).is(TFMGItems.ELECTROMAGNETIC_COIL.get())||inventory.getItem(0).is(TFMGBlocks.LARGE_COIL.get().asItem())) && spool.is(TFMGItems.COPPER_SPOOL.get()) && spool.getOrDefault(TFMGDataComponents.SPOOL_AMOUNT, defaultSpoolAmount) > 0 && inventory.getItem(0).getOrDefault(TFMGDataComponents.COIL_TURNS, defaultCoilTurns) < turnPercentage.getValue() * 10) {
            if(inventory.getItem(0).getOrDefault(TFMGDataComponents.COIL_TURNS, defaultCoilTurns) < turnPercentage.getValue() * 10){
                spool.set(TFMGDataComponents.SPOOL_AMOUNT, spool.getOrDefault(TFMGDataComponents.SPOOL_AMOUNT, defaultSpoolAmount) - 1);
                ItemStack copy = inventory.getItem(0).copy();
                copy.set(TFMGDataComponents.COIL_TURNS, copy.getOrDefault(TFMGDataComponents.COIL_TURNS, defaultCoilTurns) + 1);
                inventory.setStackInSlot(0, copy);
                setChanged();
                sendData();
                return;
            }
        }
        if(spool.has(TFMGDataComponents.SPOOL_AMOUNT))
            if (inventory.getItem(0).is(TFMGBlocks.RESISTOR.asItem()) && spool.is(TFMGItems.CONSTANTAN_SPOOL.get()) && spool.getOrDefault(TFMGDataComponents.SPOOL_AMOUNT, defaultSpoolAmount) > 0 && inventory.getItem(0).getOrDefault(TFMGDataComponents.RESISTANCE, defaultResistance) < turnPercentage.getValue() * 10) {
                if(inventory.getItem(0).getOrDefault(TFMGDataComponents.RESISTANCE, 0)< turnPercentage.getValue() * 10) {
                    spool.set(TFMGDataComponents.SPOOL_AMOUNT, spool.getOrDefault(TFMGDataComponents.SPOOL_AMOUNT, defaultSpoolAmount) - 1);
                    ItemStack copy = inventory.getItem(0).copy();
                    copy.set(TFMGDataComponents.RESISTANCE, copy.getOrDefault(TFMGDataComponents.RESISTANCE, defaultResistance) + 1);
                    inventory.setStackInSlot(0, copy);
                    setChanged();
                    sendData();
                    return;
                }
            }

        if(spool.has(TFMGDataComponents.SPOOL_AMOUNT))
            if (spool.getOrDefault(TFMGDataComponents.SPOOL_AMOUNT, defaultSpoolAmount) == 0 && !spool.is(TFMGItems.EMPTY_SPOOL.get()) && spool.getItem() instanceof SpoolItem)
                spool = TFMGItems.EMPTY_SPOOL.asStack();

        if (recipe == null) {
            return;
        }

        // Stop here if the slot item is a resistor/coil that has already
        // reached its scroll-value target. Without this guard the generic
        // recipe branch below kept ticking — draining the spool by one per
        // tick and incrementing amountWinded — even though the resistor was
        // already at its target ohm value. The user reported 5% target on a
        // resistor consuming 90 spool durability instead of 50, and 100%
        // target ending at 960 ohm instead of 1000 because the extra drain
        // burned through the spool before amountWinded had finished.
        ItemStack slotItem = inventory.getItem(0);
        if (slotItem.is(TFMGBlocks.RESISTOR.asItem())
                && slotItem.getOrDefault(TFMGDataComponents.RESISTANCE, 0) >= turnPercentage.getValue() * 10) {
            return;
        }
        if ((slotItem.is(TFMGItems.ELECTROMAGNETIC_COIL.get()) || slotItem.is(TFMGBlocks.LARGE_COIL.get().asItem()))
                && slotItem.getOrDefault(TFMGDataComponents.COIL_TURNS, 0) >= turnPercentage.getValue() * 10) {
            return;
        }



        if (amountWinded >= recipe.getProcessingDuration()) {
            inventory.setStackInSlot(0, recipe.rollResults(level.random).get(0));
            recipe = null;
            amountWinded = 0;

            sendData();
            setChanged();

        } else {
            if (spool.isEmpty() || spool.is(TFMGItems.EMPTY_SPOOL.get())) {
                return;
            }
            if (spool.getOrDefault(TFMGDataComponents.SPOOL_AMOUNT, 0) > 0) {
                if (recipe.getSpool().test(spool)) {
                    spool.set(TFMGDataComponents.SPOOL_AMOUNT, spool.getOrDefault(TFMGDataComponents.SPOOL_AMOUNT, 0) - 1);
                    amountWinded++;
                }
            } else {
                inventory.setStackInSlot(0, recipe.rollResults(level.random).get(0));
                sendData();
                setChanged();
            }

        }
    }

   //@Override
   //public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {

   //    if (cap == ForgeCapabilities.ITEM_HANDLER)
   //        return itemCapability.cast();

   //    return super.getCapability(cap, side);
   //}

    public void manageRotation() {
        float targetSpeed = (float) Math.min(Math.abs(getSpeed() * 1.5), 30);
        spoolSpeed.updateChaseTarget(targetSpeed);
        spoolSpeed.tickChaser();

    }

    @Override
    protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound,registries , clientPacket);
        compound.put("Inventory", inventory.serializeNBT(registries));

        compound.put("Spool", spool.saveOptional(registries));
        compound.putInt("AmountWinded", amountWinded);
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound,registries , clientPacket);
        inventory.deserializeNBT(registries,compound.getCompound("Inventory"));

  
        if (compound.contains("Spool")) {
            ItemStack.parse(registries, compound.getCompound("Spool")).ifPresent(i -> spool = i);
        }
        amountWinded = compound.getInt("AmountWinded");
        if (clientPacket)
            spoolSpeed.chase(getGeneratedSpeed(), 1 / 16f, LerpedFloat.Chaser.EXP);
    }

    public static class WindingMachineValueBox extends ValueBoxTransform.Sided {
        @Override
        protected Vec3 getSouthLocation() {
            return VecHelper.voxelSpace(8, 4, 16.05);
        }

        @Override
        protected boolean isSideActive(BlockState state, Direction direction) {
            return direction == state.getValue(HORIZONTAL_FACING);
        }
    }


}
