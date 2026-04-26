package com.drmangotea.tfmg.content.electricity.base;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.HashMap;
import java.util.Map;

public class ElectricNetworkManager {

    public static Map<LevelAccessor, Map<Long, ElectricalNetwork>> networks = new HashMap<>();

    public void onLoadWorld(LevelAccessor world) {
        networks.put(world, new HashMap<>());
    }
    public void onUnloadWorld(LevelAccessor world) {
        networks.remove(world);
    }
    public ElectricalNetwork getOrCreateNetworkFor(IElectric be) {
        Long id = be.getData().getId();
        Map<Long, ElectricalNetwork> map = networks.computeIfAbsent(be.getLevelAccessor(), $ -> new HashMap<>());

        ElectricalNetwork network = map.get(id);
        if (network != null) {
            network.members.removeIf(ElectricNetworkManager::isStale);
            if (network.members.isEmpty()) {
                map.remove(id);
                network = null;
            }
        }

        if (network == null) {
            network = new ElectricalNetwork(id);
            network.add(be);
            be.setNetwork(be.getData().getId());
            map.put(id, network);
        }
        return network;
    }

    public static boolean isStale(IElectric member) {
        if (member == null)
            return true;
        if (member.destroyed())
            return true;
        if (member instanceof BlockEntity be) {
            return be.isRemoved() || be.getLevel() == null;
        }
        return false;
    }
}
