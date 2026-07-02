package com.drmangotea.tfmg.content.electricity.utilities.traffic_light;

import com.drmangotea.tfmg.base.lang.TFMGLang;
import com.drmangotea.tfmg.content.electricity.base.ElectricBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;

import net.createmod.catnip.animation.LerpedFloat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class TrafficLightBlockEntity extends ElectricBlockEntity {

    protected ScrollValueBehaviour timerLength;

    public LerpedFloat glow = LerpedFloat.linear();

    int light = 0;

    public int timer = 180;
    public TrafficLightBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        timerLength = new TimerScrollBehaviour(TFMGLang.translateDirect("traffic_light.timer"), this,
                new TrafficLightScrollSlot()).between(180, 60 * 20 * 60);
        timerLength.withFormatter(this::format);
        timerLength.withCallback(value-> timer = value);
        timerLength.setValue(2);
        behaviours.add(timerLength);


    }
    private String format(int value) {
        if (value < 60)
            return value + "t";
        if (value < 20 * 60)
            return (value / 20) + "s";
        return (value / 20 / 60) + "m";
    }


    @Override
    public boolean hasElectricitySlot(Direction direction) {
        return direction.getAxis().isVertical();
    }

    @Override
    public float resistance() {
        return 175;
    }

    @Override
    public void tick() {
        super.tick();

        if (level.isClientSide) {
            glow.chase(200f, 0.4, LerpedFloat.Chaser.EXP);
            return;
        }

        if (timer > 0) {
            timer--;
        }

        int halfTimer = timerLength.getValue() / 2;

        // Cycle (timer counts down from timerLength to 0, then resets):
        //   red (light 2) -> orange (light 1) -> green (light 0) -> reset to red.
        // Green now runs all the way down to 0 so it flips straight back to red
        // instead of showing a second orange between green and red.
        int newLight;
        if (timer < halfTimer - 30) {
            newLight = 0;
        } else if (timer > halfTimer + 30) {
            newLight = 2;
        } else {
            newLight = 1;
        }

        if (newLight != light) {
            light = newLight;
            sendData();
        }

        if (timer == 0) {
            timer = timerLength.getValue();
        }
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound,registries , clientPacket);

        timer = compound.getInt("Timer");

        int newLight = compound.getInt("Light");
        if (clientPacket && newLight != light)
            glow.setValue(0);
        light = newLight;

    }

    @Override
    public void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound,registries , clientPacket);

        compound.putInt("Timer", timer);
        compound.putInt("Light", light);
    }
}
