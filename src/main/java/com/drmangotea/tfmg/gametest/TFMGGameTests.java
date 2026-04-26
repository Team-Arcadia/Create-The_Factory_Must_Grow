package com.drmangotea.tfmg.gametest;

import com.drmangotea.tfmg.content.electricity.base.ElectricBlockValues;
import com.drmangotea.tfmg.content.electricity.base.IElectric;
import com.drmangotea.tfmg.registry.TFMGBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

/**
 * Regression tests for fixes shipped on the fixes/issues-332-360 branch.
 * Each test maps 1:1 with a commit hash documented in the test procedure.
 *
 * Run via: ./gradlew runGameTestServer
 *      or: /test runall  (in the dev client)
 */
public final class TFMGGameTests {

    private TFMGGameTests() {}

    public static void register(RegisterGameTestsEvent event) {
        event.register(TFMGGameTests.class);
    }

    /**
     * commit 01d80abf — IElectric.setNetworkResistance must round 0 < r < 1 up to 1
     * instead of truncating to 0. Two parallel resistors at 1 ohm each yield 0.5 ohm,
     * which used to disable accumulator/converter discharge paths.
     */
    @GameTest(templateNamespace = "tfmg", template = "empty", timeoutTicks = 20)
    public static void networkResistance_roundsSubOhmUpToOne(GameTestHelper helper) {
        ElectricBlockValues data = new ElectricBlockValues(0L);
        IElectric stub = stubWithData(data);

        stub.setNetworkResistance(0.5f);
        helper.assertValueEqual(data.networkResistance, 1, "0.5 ohm rounded up");

        stub.setNetworkResistance(0.0001f);
        helper.assertValueEqual(data.networkResistance, 1, "0.0001 ohm rounded up");

        stub.setNetworkResistance(0f);
        helper.assertValueEqual(data.networkResistance, 0, "exactly 0 stays 0");

        stub.setNetworkResistance(2.7f);
        helper.assertValueEqual(data.networkResistance, 2, ">=1 truncated as before");

        stub.setNetworkResistance(-0.4f);
        helper.assertValueEqual(data.networkResistance, 0, "negative goes to 0");

        helper.succeed();
    }

    /**
     * commit f867118c — aluminium and cast iron tanks must be reachable through
     * Create's FluidTankBlock.isTank() check, otherwise the multiblock formation
     * silently fails. We assert the block class hierarchy here so any future
     * refactor that breaks the parent-class link fails the build.
     */
    @GameTest(templateNamespace = "tfmg", template = "empty", timeoutTicks = 20)
    public static void aluminumTank_extendsFluidTankBlock(GameTestHelper helper) {
        Class<?> alu = TFMGBlocks.ALUMINUM_FLUID_TANK.get().getClass();
        helper.assertValueEqual(
                com.simibubi.create.content.fluids.tank.FluidTankBlock.class.isAssignableFrom(alu),
                true,
                "AluminumTankBlock must extend FluidTankBlock for Create's isTank() to recognise it");

        Class<?> ci = TFMGBlocks.CAST_IRON_FLUID_TANK.get().getClass();
        helper.assertValueEqual(
                com.simibubi.create.content.fluids.tank.FluidTankBlock.class.isAssignableFrom(ci),
                true,
                "CastIronTankBlock must extend FluidTankBlock");
        helper.succeed();
    }

    /**
     * commit ec9ba240 — wrench shift+click pickup needs IWrenchable on every
     * pickup-able TFMG block. Sample a handful of blocks that previously didn't
     * have it.
     */
    @GameTest(templateNamespace = "tfmg", template = "empty", timeoutTicks = 20)
    public static void wrenchSupport_onCommonBlocks(GameTestHelper helper) {
        assertImplementsWrenchable(helper, TFMGBlocks.CREATIVE_GENERATOR.get(), "creative_generator");
        assertImplementsWrenchable(helper, TFMGBlocks.STATOR.get(), "stator");
        assertImplementsWrenchable(helper, TFMGBlocks.RESISTOR.get(), "resistor");
        assertImplementsWrenchable(helper, TFMGBlocks.POTENTIOMETER.get(), "potentiometer");
        assertImplementsWrenchable(helper, TFMGBlocks.VOLTMETER.get(), "voltmeter");
        helper.succeed();
    }

    /**
     * commit 31e55201 — placing a creative generator at a known position should
     * spawn a ticking BE without throwing on the server side. Smoke test that
     * exercises the registry + BE lifecycle.
     */
    @GameTest(templateNamespace = "tfmg", template = "empty", timeoutTicks = 40)
    public static void creativeGenerator_placesAndTicks(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 2, 1);
        BlockState state = TFMGBlocks.CREATIVE_GENERATOR.get().defaultBlockState();
        helper.setBlock(pos, state);
        helper.runAfterDelay(5, () -> {
            BlockEntity be = helper.getBlockEntity(pos);
            if (be == null) {
                helper.fail("creative generator did not spawn a BlockEntity");
                return;
            }
            helper.succeed();
        });
    }

    private static void assertImplementsWrenchable(GameTestHelper helper, Object block, String name) {
        boolean ok = block instanceof com.simibubi.create.content.equipment.wrench.IWrenchable;
        helper.assertValueEqual(ok, true, name + " must implement IWrenchable");
    }

    /** Tiny IElectric stub that only exposes the data backing field. Other methods throw. */
    private static IElectric stubWithData(ElectricBlockValues data) {
        return new IElectric() {
            @Override public long getPos() { return 0L; }
            @Override public net.minecraft.world.level.LevelAccessor getLevelAccessor() { return null; }
            @Override public ElectricBlockValues getData() { return data; }
            @Override public void sendStuff() {}
        };
    }
}
