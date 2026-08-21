package com.drmangotea.tfmg.content.engines.engine_controller.packets;

import com.drmangotea.tfmg.base.lang.TFMGLang;
import com.drmangotea.tfmg.content.engines.engine_controller.EngineControllerBlockEntity;
import com.drmangotea.tfmg.registry.TFMGPackets;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class EngineStartPacket extends EngineControllerPacketBase {


    public static final StreamCodec<ByteBuf, EngineStartPacket> STREAM_CODEC = BlockPos.STREAM_CODEC.map(
            EngineStartPacket::new, EngineStartPacket::getControllerPos
    );


    public EngineStartPacket(BlockPos controllerPos) {
        super(controllerPos);
    }

    @Override
    protected void handleItem(ServerPlayer player, ItemStack heldItem) {

    }

    @Override
    protected void handleLectern(ServerPlayer player, EngineControllerBlockEntity controller) {
        // Starting an unlinked controller was a silent no-op; say why nothing
        // happens instead of leaving the key looking broken.
        if (controller.engine == null) {
            player.displayClientMessage(TFMGLang.translateDirect("engine_controller.no_engine"), true);
            return;
        }
        controller.toggleEngine();
        player.displayClientMessage(TFMGLang.translateDirect(
                controller.engineStarted ? "engine_controller.engine_started" : "engine_controller.engine_stopped"), true);
    }



    @Override
    public PacketTypeProvider getTypeProvider() {
        return TFMGPackets.ENGINE_START;
    }
}
