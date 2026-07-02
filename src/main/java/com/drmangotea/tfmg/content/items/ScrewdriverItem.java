package com.drmangotea.tfmg.content.items;

import com.drmangotea.tfmg.content.decoration.pipes.TFMGPipeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;


public class ScrewdriverItem extends Item {
    public ScrewdriverItem(Properties p_40566_) {
        super( p_40566_);

    }
    @Override
    public InteractionResult useOn(UseOnContext pContext) {
        Player player = pContext.getPlayer();

        BlockPos positionClicked = pContext.getClickedPos();

        Level level = pContext.getLevel();

        if (level.getBlockEntity(positionClicked) instanceof TFMGPipeBlockEntity pipeBlockEntity) {
            // Lock state and tool damage are server-authoritative; the client
            // gets both through normal BE/stack sync.
            if (!level.isClientSide) {
                pipeBlockEntity.toggleLock(player);
                if (player != null)
                    pContext.getItemInHand().hurtAndBreak(1, player,
                            LivingEntity.getSlotForHand(pContext.getHand()));
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        } else {
            return super.useOn(pContext);
        }
    }
}