package com.drmangotea.tfmg.content.electricity.storage;

import com.drmangotea.tfmg.base.lang.TFMGTexts;
import com.drmangotea.tfmg.config.TFMGConfigs;
import com.drmangotea.tfmg.content.electricity.base.ElectricBlockEntity;
import com.drmangotea.tfmg.content.electricity.base.IElectric;
import com.drmangotea.tfmg.content.electricity.base.IVoltageSource;
import com.drmangotea.tfmg.content.electricity.utilities.converter.ConverterBlockEntity;
import com.drmangotea.tfmg.registry.TFMGBlockEntities;
import com.drmangotea.tfmg.registry.TFMGDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.energy.IEnergyStorage;

import java.util.List;

import static net.minecraft.world.level.block.DirectionalBlock.FACING;

public class AccumulatorBlockEntity extends ElectricBlockEntity implements IVoltageSource {

    public TFMGForgeEnergyStorage energy = createEnergyStorage(1);
    private IEnergyStorage energyCapability;
    public int length = 1;
    boolean refreshNextTick = true;
    public BlockPos controller = getBlockPos();
    int signal;
    boolean signalChanged;

    public AccumulatorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        energyCapability = energy;

    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                TFMGBlockEntities.ACCUMULATOR.get(),
                (be, context) -> be.energyCapability
        );
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        neighbourChanged();
    }

    public void neighbourChanged() {

        if (!hasLevel())
            return;
        if (isController()) {
            int power = level.getBestNeighborSignal(worldPosition);


            if (power != signal)
                signalChanged = true;
        }
        if (level.getBlockEntity(controller) instanceof AccumulatorBlockEntity be) {
            int power = level.getBestNeighborSignal(worldPosition);


            if (power != be.signal)
                be.signalChanged = true;
        }
    }


    @Override
    public void remove() {
        super.remove();
    }

    @Override
    public void destroy() {
        super.destroy();
        // Hand off this block's stored energy + the entire chain's energy to
        // the surviving sub-chains BEFORE the chain rebuild kicks in. The
        // previous code just called refreshController which scanned the
        // partial chain from the destroyed block's pos and never carried
        // this BE's energy anywhere — every break voided up to one slave's
        // worth of charge.
        rebuildChainAround(getBlockPos(), energy.getEnergyStored());
    }

    @Override
    public void onPlaced() {
        super.onPlaced();
        rebuildChainAround(getBlockPos(), 0);
    }

    @Override
    public boolean makeMultimeterTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        if (!isController())
            if (level.getBlockEntity(controller) instanceof AccumulatorBlockEntity be)
                return be.makeMultimeterTooltip(tooltip, isPlayerSneaking);
        super.makeMultimeterTooltip(tooltip, isPlayerSneaking);

        TFMGTexts.electricalCapacity(energy.getEnergyStored()).forGoggles(tooltip, 1);
        TFMGTexts.chargingRate(getChargingRate()).forGoggles(tooltip, 1);
        TFMGTexts.electricalMaxCapacity(getMaxCapacity()).forGoggles(tooltip, 1);

        return true;
    }

    public void refreshController() {
        rebuildChainAround(getBlockPos(), 0);
    }

    /**
     * Walks the chain in BOTH directions from a seed position, treating the
     * seed as either present (just placed) or absent (just destroyed). For
     * a destroy, the seed pos returns null from level.getBlockEntity so the
     * scan stops there — which means the chain is split in two. We rebuild
     * each side independently. donatedEnergy is added to whichever sub-chain
     * touches the seed; if both sub-chains exist (middle break), it goes
     * into the side facing.opposite (the controller side, where storage
     * lives anyway).
     */
    private void rebuildChainAround(BlockPos seed, int donatedEnergy) {
        if (level == null)
            return;
        Direction facing = getBlockState().getValue(FACING);
        // Find the tail (most-facing.opposite) and head (most-facing) of each
        // sub-chain neighbouring the seed. Scan each side starting one step
        // away from the seed so a break in the middle creates two sub-chains.
        BlockPos tailSide = seed.relative(facing.getOpposite());
        BlockPos headSide = seed.relative(facing);
        boolean rebuiltOpp = rebuildSubChainStartingFrom(tailSide, facing, donatedEnergy);
        // The opposite-side sub-chain absorbs the donated energy (controller
        // side). If it didn't exist, donate to the head side instead so the
        // energy isn't voided.
        rebuildSubChainStartingFrom(headSide, facing, rebuiltOpp ? 0 : donatedEnergy);
    }

    /**
     * Given an arbitrary accumulator pos, walk the chain in facing/opposite
     * directions, find the controller (most-facing.opposite end), and
     * promote it. Returns false if no accumulator chain was found here.
     */
    private boolean rebuildSubChainStartingFrom(BlockPos anchor, Direction facing, int extraEnergy) {
        if (!(level.getBlockEntity(anchor) instanceof AccumulatorBlockEntity start))
            return false;
        if (start.getBlockState().getValue(FACING) != facing)
            return false;
        // Find the tail (the most-facing.opposite block belonging to this chain).
        BlockPos tailPos = anchor;
        for (int i = 1; i < 15; i++) {
            BlockPos probe = anchor.relative(facing.getOpposite(), i);
            if (level.getBlockEntity(probe) instanceof AccumulatorBlockEntity probeBe
                    && probeBe.getBlockState().getValue(FACING) == facing) {
                tailPos = probe;
            } else break;
        }
        if (!(level.getBlockEntity(tailPos) instanceof AccumulatorBlockEntity tail))
            return false;
        // Sum every block's energy from tailPos toward facing direction.
        int totalEnergy = extraEnergy;
        int newLength = 0;
        java.util.List<AccumulatorBlockEntity> members = new java.util.ArrayList<>();
        for (int i = 0; i < 15; i++) {
            BlockPos pos = tailPos.relative(facing, i);
            if (level.getBlockEntity(pos) instanceof AccumulatorBlockEntity be
                    && be.getBlockState().getValue(FACING) == facing) {
                totalEnergy += be.energy.getEnergyStored();
                members.add(be);
                newLength++;
            } else break;
        }
        // Promote tail to controller, demote everyone else to slaves.
        tail.controller = tail.getBlockPos();
        tail.length = newLength;
        tail.energy = tail.createEnergyStorage(1);
        tail.energy.setEnergy(Math.min(totalEnergy, tail.energy.getMaxEnergyStored()));
        tail.refreshCapability();
        tail.updateNextTick();
        for (AccumulatorBlockEntity be : members) {
            if (be == tail)
                continue;
            be.controller = tail.getBlockPos();
            be.length = 0;
            be.energy.setEnergy(0);
            be.refreshCapability();
            be.sendStuff();
        }
        tail.sendStuff();
        return true;
    }

    public void refreshMultiblock() {
        Direction facing = getBlockState().getValue(FACING);
        refreshCapability();
        if (!(level.getBlockEntity(getBlockPos().relative(facing)) instanceof AccumulatorBlockEntity be && be.getBlockState().getValue(FACING) == facing)) {
            // Sum existing energy across the chain BEFORE clearing slave
            // tanks, so that pre-existing slave energy is rolled into the
            // controller instead of being voided. The previous code did
            // otherBe.energy.setEnergy(0) without contributing to the
            // controller's storage, so any energy that had been received
            // by a slave (e.g. before promotion, or after a chain rebuild)
            // disappeared on the next refresh.
            int totalEnergy = energy.getEnergyStored();
            int newLength = 1;
            controller = getBlockPos();
            for (int i = 1; i < 15; i++) {
                BlockPos pos = getBlockPos().relative(getBlockState().getValue(FACING).getOpposite(), i);
                if (level.getBlockEntity(pos) instanceof AccumulatorBlockEntity otherBe && otherBe.getBlockState().getValue(FACING) == getBlockState().getValue(FACING)) {
                    totalEnergy += otherBe.energy.getEnergyStored();
                    otherBe.controller = this.getBlockPos();
                    otherBe.refreshCapability();
                    otherBe.length = 0;
                    otherBe.energy.setEnergy(0);

                    newLength++;
                } else break;
            }
            length = newLength;
            // getMaxCapacity() already multiplies by length, so the
            // storage multiplier here is 1 (otherwise capacity is squared).
            energy = createEnergyStorage(1);
            energy.setEnergy(Math.min(totalEnergy, energy.getMaxEnergyStored()));
            refreshCapability();
            updateNextTick();
            for (int i = 1; i < length; i++) {
                BlockPos pos = getBlockPos().relative(getBlockState().getValue(FACING).getOpposite(), i);
                if (level.getBlockEntity(pos) instanceof AccumulatorBlockEntity be) {
                    be.refreshCapability();
                    be.sendStuff();
                }
            }
            sendStuff();
        }
    }

    public void refreshCapability() {
        IEnergyStorage oldCap = energyCapability;
        if (level.getBlockEntity(controller) instanceof AccumulatorBlockEntity be) {
            energyCapability = be.energy;
        } else energyCapability = energy;
        invalidateCapabilities();
    }

    public boolean isController() {
        return controller == null || controller.equals(getBlockPos());
    }

    public TFMGForgeEnergyStorage createEnergyStorage(int multiplier) {
        return new TFMGForgeEnergyStorage(getMaxCapacity() * multiplier, 10000) {
            @Override
            public void onEnergyChanged(int amount, int oldAmount) {

                if ((oldAmount == 0 && amount > 0) || (this.energy == 0)) {
                    updateNextTick();
                }
                sendStuff();
            }
        };
    }

    public void setCapacity(ItemStack stack) {
        if (stack.get(TFMGDataComponents.ACCUMULATOR_STORAGE) != null)
            energy.setEnergy(stack.get(TFMGDataComponents.ACCUMULATOR_STORAGE));
    }

    protected void analogSignalChanged() {

        if (!isController()) {
            signal = level.getBestNeighborSignal(controller);
            return;
        }

        int newSignal = 0;

        for (int i = 0; i < length; i++) {
            BlockPos pos = getBlockPos().relative(getBlockState().getValue(FACING).getOpposite(), i);

            newSignal = Math.max(newSignal, level.getBestNeighborSignal(pos));


        }

        updateNextTick();

        signal = newSignal;
    }

    @Override
    public void tick() {
        super.tick();
        if (signalChanged) {
            signalChanged = false;
            analogSignalChanged();
        }
        if (!isController())
            return;

        if (refreshNextTick) {
            refreshMultiblock();
            refreshNextTick = false;
        }

        if (getData().getVoltage() > TFMGConfigs.common().machines.accumulatorVoltage.get() * length) {
            energy.receiveEnergy((int) (getChargingRate() / TFMGConfigs.common().machines.FEtoWattTickConversionRate.get()), false);

            return;
        }
        if (canPower()) {

            int energyToExtract = data.networkPowerGeneration == 0 ? getNetworkPowerUsage() : (int) Math.max(0, Math.max(((float) powerGeneration() / (float) data.networkPowerGeneration) * (float) getNetworkPowerUsage(), 0));
            energyToExtract /= TFMGConfigs.common().machines.FEtoWattTickConversionRate.get();
            energy.extractEnergy(Math.max(energyToExtract, 1), false);
            if (energy.getEnergyStored() == 0)
                updateNextTick();
        }

    }

    @Override
    protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);

        compound.putInt("ForgeEnergy", energy.getEnergyStored());
        compound.putInt("Length", length);

    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        length = compound.getInt("Length");
        //energy = createEnergyStorage(length);
        energy.setEnergy(compound.getInt("ForgeEnergy"));


    }

    @Override
    public float resistance() {
        if (voltageGeneration() > 0)
            return 0;
        if (!isController())
            return 0;

        int power = 0;
        for (IElectric member : getOrCreateElectricNetwork().members)
            if (!(member instanceof ConverterBlockEntity) && !(member instanceof AccumulatorBlockEntity))
                power += member.getPowerUsage();
        if (energy.getEnergyStored() == getMaxCapacity() || getData().getVoltage() <= getOutputVoltage() || canPower())
            return 0;
        if (Math.min(Math.max((data.networkPowerGeneration - power), 0), getMaxChargingRate()) == 0) {
            return 0;
        }

        return (float) Math.min((Math.pow(data.voltage, 2)) / Math.min(Math.max((data.networkPowerGeneration - power), 0), getMaxChargingRate()), 750);
    }

    public boolean canPower() {
        return getData().networkResistance > 0 && (getData().getVoltage() <= getOutputVoltage()) && energy.getEnergyStored() > 0 && signal == 0;
    }

    /**
     * Returns true if the accumulator should be exposing its voltage to the
     * network. canPower also requires a non-zero networkResistance, but on
     * the very first updateNetwork tick the network resistance is still 0,
     * which made canPower false, which made voltageGeneration return 0,
     * which made maxVoltage stay 0, which kept setNetworkResistance at 0.
     * The chicken-and-egg loop meant a fresh accumulator + load chain never
     * energised. Loosen the gate to 'energy stored AND no redstone signal'
     * so the first tick already publishes a voltage and the network can
     * compute its resistance from there.
     */
    public boolean canExposeVoltage() {
        return energy.getEnergyStored() > 0 && signal == 0 && isController();
    }


    public int getChargingRate() {
        //
        // int chargingRate = Math.max((data.networkPowerGeneration - getNetworkPowerUsage()), 0);
        if (energy.getEnergyStored() >= getMaxCapacity() || getData().getVoltage() < getOutputVoltage() || canPower() || data.notEnoughPower)
            return 0;

        //return Math.min(chargingRate, getMaxChargingRate());
        return getMaxChargingRate();
    }

    @Override
    public int powerGeneration() {
        return canExposeVoltage() ? maxPowerOutput() : 0;
    }

    public int maxPowerOutput() {
        return getOutputVoltage() * TFMGConfigs.common().machines.accumulatorMaxAmpOutput.get();
    }

    @Override
    public int getMaxPowerOutput() {
        return maxPowerOutput();
    }

    public int getMaxCapacity() {
        return TFMGConfigs.common().machines.accumulatorStorage.get() * length;
    }

    //in FE per tick
    public int getMaxChargingRate() {
        return TFMGConfigs.common().machines.accumulatorChargingRate.get();
    }


    public int getOutputVoltage() {


        return TFMGConfigs.common().machines.accumulatorVoltage.get() * length;
    }

    @Override
    public int voltageGeneration() {
        return canExposeVoltage() ? getOutputVoltage() : 0;
    }


    @Override
    public boolean hasElectricitySlot(Direction direction) {
        return direction.getAxis() == getBlockState().getValue(FACING).getAxis();
    }
}
