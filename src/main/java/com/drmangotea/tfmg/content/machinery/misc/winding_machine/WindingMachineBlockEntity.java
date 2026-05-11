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
            TFMGTexts.progress(amountWinded + "/" + currentRequiredDuration())
                    .color(spool.getBarColor())
                    .forGoggles(tooltip);
        }
        return true;
    }

    /**
     * Effective denominator for a winding recipe. Resistor/coil-producing
     * recipes scale with the scroll-value target so the goggle progress and
     * the actual spool drain agree (1 unit = 1 ohm / 1 turn).
     */
    private int currentRequiredDuration() {
        if (recipe == null)
            return 0;
        int duration = recipe.getProcessingDuration();
        if (!recipe.getRollableResults().isEmpty()) {
            ItemStack template = recipe.getRollableResults().get(0).getStack();
            if (template.is(TFMGBlocks.RESISTOR.asItem())
                    || template.is(TFMGItems.ELECTROMAGNETIC_COIL.get())
                    || template.is(TFMGBlocks.LARGE_COIL.get().asItem())) {
                duration = turnPercentage.getValue() * 10;
            }
        }
        return duration;
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
        // Server-only. tick() runs on both sides; this method mutates
        // authoritative state (spool component, slot stack, amountWinded)
        // so it must not run client-side.
        if (level == null || level.isClientSide)
            return;
        if (getSpeed() == 0)
            return;

        ItemStack slotItem = inventory.getItem(0);
        int target = turnPercentage.getValue() * 10;
        boolean isResistor = slotItem.is(TFMGBlocks.RESISTOR.asItem());
        boolean isCoil = slotItem.is(TFMGItems.ELECTROMAGNETIC_COIL.get())
                || slotItem.is(TFMGBlocks.LARGE_COIL.get().asItem());

        // Resistor + constantan spool path. ONE drain per tick, no
        // fall-through into the generic branch below. Returns after a
        // successful drain and also returns if the target is already met.
        if (isResistor && spool.is(TFMGItems.CONSTANTAN_SPOOL.get())) {
            int resistance = slotItem.getOrDefault(TFMGDataComponents.RESISTANCE, 0);
            if (resistance >= target)
                return;
            int spoolAmount = spool.getOrDefault(TFMGDataComponents.SPOOL_AMOUNT, 0);
            if (spoolAmount <= 0)
                return;
            spool.set(TFMGDataComponents.SPOOL_AMOUNT, spoolAmount - 1);
            ItemStack copy = slotItem.copy();
            copy.set(TFMGDataComponents.RESISTANCE, resistance + 1);
            inventory.setStackInSlot(0, copy);
            convertEmptyIfDrained();
            setChanged();
            sendData();
            return;
        }

        // Coil + copper spool path. Symmetric to the resistor path.
        if (isCoil && spool.is(TFMGItems.COPPER_SPOOL.get())) {
            int turns = slotItem.getOrDefault(TFMGDataComponents.COIL_TURNS, 0);
            if (turns >= target)
                return;
            int spoolAmount = spool.getOrDefault(TFMGDataComponents.SPOOL_AMOUNT, 0);
            if (spoolAmount <= 0)
                return;
            spool.set(TFMGDataComponents.SPOOL_AMOUNT, spoolAmount - 1);
            ItemStack copy = slotItem.copy();
            copy.set(TFMGDataComponents.COIL_TURNS, turns + 1);
            inventory.setStackInSlot(0, copy);
            convertEmptyIfDrained();
            setChanged();
            sendData();
            return;
        }

        // Generic winding recipe path (sequenced assembly etc.).
        if (recipe == null)
            return;
        // Resistor / coil items must NEVER take the generic branch; their
        // dedicated branches above are the only paths that should drain
        // their spool. The generic branch runs amountWinded++ which is
        // unrelated to RESISTANCE / COIL_TURNS targeting.
        if (isResistor || isCoil)
            return;

        int requiredDuration = currentRequiredDuration();

        if (amountWinded >= requiredDuration) {
            // Stamp the freshly-crafted output with the scroll-value target
            // if it's a resistor / coil. Otherwise the recipe's static
            // default (RESISTANCE = 10, COIL_TURNS = 100) would be applied
            // and the dedicated branch above would then drain extra spool
            // climbing back up to the player's target — the '+40 leak' the
            // testers reported. Bake the target into the result.
            ItemStack result = recipe.rollResults(level.random).get(0);
            if (result.is(TFMGBlocks.RESISTOR.asItem()))
                result.set(TFMGDataComponents.RESISTANCE, target);
            else if (result.is(TFMGItems.ELECTROMAGNETIC_COIL.get())
                    || result.is(TFMGBlocks.LARGE_COIL.get().asItem()))
                result.set(TFMGDataComponents.COIL_TURNS, target);
            inventory.setStackInSlot(0, result);
            recipe = null;
            amountWinded = 0;
            sendData();
            setChanged();
            return;
        }
        if (spool.isEmpty() || spool.is(TFMGItems.EMPTY_SPOOL.get()))
            return;
        int spoolAmount = spool.getOrDefault(TFMGDataComponents.SPOOL_AMOUNT, 0);
        if (spoolAmount > 0) {
            if (recipe.getSpool().test(spool)) {
                spool.set(TFMGDataComponents.SPOOL_AMOUNT, spoolAmount - 1);
                amountWinded++;
                convertEmptyIfDrained();
                // Without this the client never receives the per-tick
                // SPOOL_AMOUNT / amountWinded updates, so the goggle tooltip
                // and the spool durability bar stay frozen until the recipe
                // finishes. The dedicated resistor/coil branches already
                // send data per drain — the generic branch was the outlier.
                setChanged();
                sendData();
            }
        } else {
            inventory.setStackInSlot(0, recipe.rollResults(level.random).get(0));
            sendData();
            setChanged();
        }
    }

    /** Promote a depleted SpoolItem to an empty_spool. */
    private void convertEmptyIfDrained() {
        if (!spool.has(TFMGDataComponents.SPOOL_AMOUNT))
            return;
        if (spool.getOrDefault(TFMGDataComponents.SPOOL_AMOUNT, 0) == 0
                && !spool.is(TFMGItems.EMPTY_SPOOL.get())
                && spool.getItem() instanceof SpoolItem) {
            spool = TFMGItems.EMPTY_SPOOL.asStack();
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
