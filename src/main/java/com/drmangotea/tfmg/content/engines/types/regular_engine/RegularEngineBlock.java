package com.drmangotea.tfmg.content.engines.types.regular_engine;

import com.drmangotea.tfmg.content.engines.base.EngineBlock;
import com.drmangotea.tfmg.content.engines.types.AbstractSmallEngineBlockEntity;
import com.drmangotea.tfmg.registry.TFMGBlockEntities;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

public class RegularEngineBlock extends EngineBlock implements IBE<RegularEngineBlockEntity> {

    public static final BooleanProperty EXTENDED = BooleanProperty.create("extended");

    public RegularEngineBlock(Properties properties) {
        super(properties);
    }


    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(EXTENDED);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        ItemStack itemStack = player.getItemInHand(hand);

        if (level.getBlockEntity(pos) instanceof RegularEngineBlockEntity be && !be.isController() && level.getBlockEntity(be.controller) instanceof AbstractSmallEngineBlockEntity controller) {
            // Components live on each block now; the master routes the item
            // into the first incomplete block of the chain.
            if (controller.nextComponent().test(itemStack))
                if (controller.insertItem(itemStack, player.isShiftKeyDown(), player, hand))
                    return ItemInteractionResult.SUCCESS;
        }

        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }
    @Override
    public void onPlace(BlockState pState, Level level, BlockPos pos, BlockState pOldState, boolean pIsMoving) {
        // KineticBlock.onPlace handles rotation propagation bookkeeping; the
        // electric network hookup runs via the constructor's connectNextTick
        // flag on the first tick, so no manual IElectric::onPlaced here (it
        // ran the full reconnect twice, on both sides).
        super.onPlace(pState, level, pos, pOldState, pIsMoving);
    }
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        IBE.onRemove(state, level, pos, newState);
    }
    @Override
    public Class<RegularEngineBlockEntity> getBlockEntityClass() {
        return RegularEngineBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends RegularEngineBlockEntity> getBlockEntityType() {
        return TFMGBlockEntities.REGULAR_ENGINE.get();
    }
}
