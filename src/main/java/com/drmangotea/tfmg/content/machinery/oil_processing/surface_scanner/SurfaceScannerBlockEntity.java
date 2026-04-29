package com.drmangotea.tfmg.content.machinery.oil_processing.surface_scanner;

import com.drmangotea.tfmg.base.lang.TFMGTexts;
import com.drmangotea.tfmg.config.TFMGConfigs;
import com.drmangotea.tfmg.content.machinery.misc.machine_input.MachineInputBlockEntity;
import com.drmangotea.tfmg.registry.TFMGTags;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.util.List;

public class SurfaceScannerBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {


    public Boolean[][] grid = new Boolean[5][5];

    public SurfaceScannerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setLazyTickRate(20);

    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

    public void findDeposits(){

        if(level.isClientSide)
            return;

        for(int x = 0;x<5;x++){
            for(int z = 0;z<5;z++){
                grid[x][z] = hasOil(new BlockPos(getBlockPos().getX() + (x-2)*16, TFMGConfigs.common().machines.surfaceScannerScanDepth.get(),getBlockPos().getZ() + (z-2)*16));
            }
        }
        sendData();
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        TFMGTexts.header("surface_scanner")
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip);
        if(level.getBlockEntity(getBlockPos().below()) instanceof MachineInputBlockEntity be&&Math.abs(be.getSpeed())>=64) {
            int depositsFound = 0;
            for(Boolean[] row : grid){
                for(Boolean light : row){
                    if(light!=null&&light)
                        depositsFound++;
                }
            }

            if(depositsFound>0){
                TFMGTexts.SurfaceScanner.deposits(depositsFound).forGoggles(tooltip);
            }else
                TFMGTexts.SurfaceScanner.noDeposit().forGoggles(tooltip);
        }else
            TFMGTexts.SurfaceScanner.noRotation().forGoggles(tooltip);

        return true;
    }

    @Override
    public void lazyTick() {
        super.lazyTick();

        if(level.getBlockEntity(getBlockPos().below()) instanceof MachineInputBlockEntity be&&Math.abs(be.getSpeed())>=64) {
            findDeposits();
        }else {
            grid = new Boolean[5][5];
        }
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        // grid was never serialised: server computed it, sendData wrote the
        // BE NBT, but without a write/read entry the client kept seeing an
        // empty Boolean[5][5] forever. Encode as 25 bytes: 0 = unset,
        // 1 = false (scanned, no deposit), 2 = true (scanned, deposit).
        byte[] flat = new byte[25];
        for (int x = 0; x < 5; x++) {
            for (int z = 0; z < 5; z++) {
                Boolean cell = grid[x][z];
                flat[x * 5 + z] = (byte) (cell == null ? 0 : (cell ? 2 : 1));
            }
        }
        tag.putByteArray("Grid", flat);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        if (!tag.contains("Grid")) {
            grid = new Boolean[5][5];
            return;
        }
        byte[] flat = tag.getByteArray("Grid");
        if (flat.length != 25) {
            grid = new Boolean[5][5];
            return;
        }
        Boolean[][] next = new Boolean[5][5];
        for (int x = 0; x < 5; x++) {
            for (int z = 0; z < 5; z++) {
                byte v = flat[x * 5 + z];
                next[x][z] = v == 0 ? null : v == 2;
            }
        }
        grid = next;
    }

    public boolean hasOil(BlockPos pos){
        ChunkAccess chunk = level.getChunk(pos);
        // Walk every non-empty 16x16x16 section in the chunk and check each
        // block via the section's PalettedContainer. Deposits sit at worldgen
        // Y = -64 by default but may exist at any Y; the previous bounding-box
        // path returned nothing in practice because the box straddled the
        // chunk boundary and chunk.getBlockStates filtered every block out.
        int minY = level.getMinBuildHeight();
        int maxY = TFMGConfigs.common().machines.surfaceScannerScanDepth.get();
        if (maxY <= minY)
            return false;
        LevelChunkSection[] sections = chunk.getSections();
        int minSectionIndex = chunk.getSectionIndex(minY);
        int maxSectionIndex = chunk.getSectionIndex(maxY);
        for (int s = minSectionIndex; s <= maxSectionIndex && s < sections.length; s++) {
            LevelChunkSection section = sections[s];
            if (section == null || section.hasOnlyAir())
                continue;
            int sectionMinY = SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(s));
            int yStart = Math.max(0, minY - sectionMinY);
            int yEnd = Math.min(15, maxY - sectionMinY);
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = yStart; y <= yEnd; y++) {
                        BlockState state = section.getBlockState(x, y, z);
                        if (state.is(TFMGTags.TFMGBlockTags.SURFACE_SCANNER_FINDABLE.tag))
                            return true;
                    }
                }
            }
        }
        return false;
    }





}
