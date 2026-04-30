package com.drmangotea.tfmg.content.electricity.base;

/**
 * Marker interface for IElectric blocks that act as voltage sources for
 * adjacent network members WITHOUT extending VoltageAlteringBlockEntity.
 *
 * Accumulator + Converter cannot extend VoltageAlteringBlockEntity because
 * VoltageAlteringBlockEntity.getPowerUsage delegates to the neighbour's
 * getNetworkPowerUsage which iterates members and recurses through every
 * VoltageAlteringBlockEntity on the network — chaining accumulators or
 * converters used to blow the stack instantly.
 *
 * IElectric.voltageGeneration / powerGeneration test both this interface and
 * VoltageAlteringBlockEntity, so a block can become a voltage source by
 * implementing this without inheriting the recursive power accounting.
 */
public interface IVoltageSource extends IElectric {

    /** Output voltage exposed to neighbour members (0 = idle). */
    int getOutputVoltage();

    /** Maximum power this source can deliver, in Watts/tick. */
    int getMaxPowerOutput();
}
