package com.drmangotea.tfmg.content.machinery.vat.industrial_mixer;

import com.drmangotea.tfmg.content.machinery.vat.base.VatBlock;
import com.drmangotea.tfmg.registry.TFMGBlockEntities;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import static com.drmangotea.tfmg.content.machinery.vat.industrial_mixer.IndustrialMixerBlockEntity.MixerMode;

public class IndustrialMixerBlock extends KineticBlock implements IBE<IndustrialMixerBlockEntity> {



    public IndustrialMixerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return Direction.Axis.Y;
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face == Direction.UP;
    }

    @Override
    public void onRemove(BlockState state, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
        VatBlock.updateVatState(state, pLevel, pPos.relative(Direction.DOWN));
        super.onRemove(state, pLevel, pPos, pNewState, pIsMoving);

    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
     if(hand == InteractionHand.OFF_HAND)
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if(level.getBlockEntity(pos) instanceof IndustrialMixerBlockEntity be){
            MixerMode mixerMode = be.mixerMode;
            ItemStack stackInside = mixerMode.item;
            if(stack.is(stackInside.getItem()))
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

            if(be.setMixerMode(stack, true)) {

                // Resolve the new mode from the item being inserted, captured
                // before the stack is decremented. Passing the leftover stack
                // meant that inserting the last blade of a stack handed
                // setMixerMode an empty one, and an empty stack matches
                // MixerMode.NONE: the blade was consumed and the mixer stayed
                // empty unless the player happened to hold several.
                ItemStack inserted = stack.copyWithCount(1);
                stack.shrink(1);
                if (stack.isEmpty())
                    player.setItemInHand(hand, mixerMode.item.copy());
                else if (!mixerMode.item.isEmpty() && !player.getInventory().add(mixerMode.item.copy()))
                    player.drop(mixerMode.item.copy(), false);
                be.setMixerMode(inserted, false);
                return ItemInteractionResult.SUCCESS;
            }
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public void onPlace(BlockState state, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving) {
        VatBlock.updateVatState(state, pLevel, pPos.relative(Direction.DOWN));
        super.onPlace(state, pLevel, pPos, pOldState, pIsMoving);

    }

    @Override
    public Class<IndustrialMixerBlockEntity> getBlockEntityClass() {
        return IndustrialMixerBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends IndustrialMixerBlockEntity> getBlockEntityType() {
        return TFMGBlockEntities.INDUSTRIAL_MIXER.get();
    }
}
