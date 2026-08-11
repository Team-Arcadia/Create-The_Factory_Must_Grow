package com.drmangotea.tfmg.content.machinery.misc.winding_machine;

import com.drmangotea.tfmg.TFMG;
import com.drmangotea.tfmg.TFMGRegistries;
import com.drmangotea.tfmg.base.TFMGUtils;
import com.drmangotea.tfmg.base.lang.TFMGLang;
import com.drmangotea.tfmg.content.electricity.connection.cable_type.CableType;
import com.drmangotea.tfmg.content.electricity.connection.cables.CableConnection;
import com.drmangotea.tfmg.content.electricity.connection.cables.CableConnectorBlockEntity;
import com.drmangotea.tfmg.registry.TFMGDataComponents;
import com.drmangotea.tfmg.registry.TFMGItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;
import java.util.Objects;

import static com.drmangotea.tfmg.base.blocks.WallMountBlock.FACING;

public class SpoolItem extends Item {

    public final int barColor;
    public final ResourceLocation cableTypeKey;

    public SpoolItem(Properties properties, int barColor, ResourceLocation cableTypeKey) {
        super(properties);
        this.barColor = barColor;
        this.cableTypeKey = cableTypeKey;
    }


    @Override
    public void onCraftedBy(ItemStack stack, Level p_41448_, Player p_41449_) {
        stack.set(TFMGDataComponents.SPOOL_AMOUNT, 1000);
        super.onCraftedBy(stack, p_41448_, p_41449_);

    }


    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isCrouching() && stack.getOrDefault(TFMGDataComponents.POSITION, 0f).longValue() != 0f) {
            if (level.getBlockEntity(BlockPos.of(stack.getOrDefault(TFMGDataComponents.POSITION, 0f).longValue())) instanceof CableConnectorBlockEntity be)
                be.player = null;
            stack.remove(TFMGDataComponents.POSITION);
            stack.remove(TFMGDataComponents.X_POS);
            stack.remove(TFMGDataComponents.Y_POS);
            stack.remove(TFMGDataComponents.Z_POS);
            if (level.isClientSide)
                player.displayClientMessage(TFMGLang.translateDirect("wires.removed_data")
                        .withStyle(ChatFormatting.YELLOW), true);
            return InteractionResultHolder.success(stack);

        }

        return super.use(level, player, hand);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {

        String text = TFMGLang.translateDirect("tooltip.coils").getString();

        tooltip.add(TFMGLang.text(text + stack.getOrDefault(TFMGDataComponents.SPOOL_AMOUNT, 0)).component().withStyle(ChatFormatting.GREEN)

        );

        if (stack.get(TFMGDataComponents.POSITION) == null)
            return;
        BlockPos pos = BlockPos.of(stack.get(TFMGDataComponents.POSITION));
        if (pos.asLong() != 0)
            tooltip.add(TFMGLang.text(pos.getX() + " " + pos.getY() + " " + pos.getZ()).component()
                    .withStyle(ChatFormatting.AQUA)
            );
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {

        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        if (level.getBlockEntity(pos) instanceof WindingMachineBlockEntity be) {
            // Only real spools may be slotted; ignore anything else so a
            // deployer / dispenser can't shove an arbitrary item into the
            // spool field.
            if (!(stack.getItem() instanceof SpoolItem))
                return InteractionResult.PASS;
            // The swap mutates authoritative state — server only.
            if (level.isClientSide)
                return InteractionResult.SUCCESS;

            ItemStack oldSpool = be.spool.isEmpty() ? ItemStack.EMPTY : be.spool;
            // Store a COPY (count 1), never the live stack held by the player or
            // the deployer: performRecipe mutates be.spool every tick, and
            // aliasing the deployer's own stack corrupted its state and could
            // hang the interaction.
            be.spool = stack.copyWithCount(1);
            stack.shrink(1);

            // Hand the previously held spool back. context.getPlayer() is null
            // for some automation (e.g. dispensers), so guard it instead of
            // NPEing — drop the old spool in the world when there is no player
            // to receive it.
            if (!oldSpool.isEmpty()) {
                if (player == null)
                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), oldSpool);
                else if (stack.isEmpty())
                    player.setItemInHand(context.getHand(), oldSpool);
                else if (!player.getInventory().add(oldSpool))
                    player.drop(oldSpool, false);
            }

            be.onSpoolChanged();
            return InteractionResult.SUCCESS;
        }

        if (stack.get(TFMGDataComponents.SPOOL_AMOUNT) == null)
            return InteractionResult.PASS;
        if (level.isClientSide)
            return InteractionResult.SUCCESS;

        if (Objects.equals(cableTypeKey, TFMG.asResource("empty")))
            return InteractionResult.PASS;
        // Nothing above guarantees the clicked block is a cable connector, and
        // BlockState#getValue throws when the property is absent. FACING here is
        // BlockStateProperties.FACING, which most blocks do not carry (a chest
        // uses HORIZONTAL_FACING, a different property), so right-clicking an
        // ordinary block with a wound spool threw straight out of useOn — on the
        // server, since the client already returned above.
        BlockState clickedState = level.getBlockState(pos);
        if (!clickedState.hasProperty(FACING))
            return InteractionResult.PASS;
        Direction direction = clickedState.getValue(FACING);
        for (int i = 0; i < 64; i++) {
            if (level.getBlockEntity(pos.relative(direction)) instanceof CableConnectorBlockEntity) {
                pos = pos.relative(direction);

            } else break;

        }

        if (level.getBlockEntity(pos) instanceof CableConnectorBlockEntity be) {


            if (stack.get(TFMGDataComponents.POSITION) != null) {
                BlockPos posToConnect = BlockPos.of(stack.get(TFMGDataComponents.POSITION));
                if (posToConnect.equals(pos)) {
                    // REMOVE the stored endpoint, do not set it to 0. The check
                    // above only asks whether the component is present, so a
                    // stored 0 left the spool believing it had an endpoint at
                    // (0,0,0): every later click tried to wire the connector to
                    // the world origin and the spool linked nothing ever again.
                    stack.remove(TFMGDataComponents.POSITION);
                    // No isClientSide guard: the method already returned on the
                    // client further up, so this branch is server-only and the
                    // guard silently swallowed the message.
                    if (player != null)
                        player.displayClientMessage(TFMGLang.translateDirect("wires.cant_connect_itself")
                                .withStyle(ChatFormatting.YELLOW), true);
                    be.player = null;
                    be.sendData();
                    be.setChanged();
                    return InteractionResult.SUCCESS;
                }
                // Walk the stored endpoint along ITS OWN facing. This used to
                // reuse "direction", the facing of the block just clicked, so
                // the first endpoint was walked in a direction belonging to an
                // unrelated connector and could resolve to the wrong block or
                // wander off the run entirely.
                BlockState otherState = level.getBlockState(posToConnect);
                if (otherState.hasProperty(FACING)) {
                    Direction otherDirection = otherState.getValue(FACING);
                    for (int i = 0; i < 64; i++) {
                        if (level.getBlockEntity(posToConnect.relative(otherDirection)) instanceof CableConnectorBlockEntity) {
                            posToConnect = posToConnect.relative(otherDirection);

                        } else break;

                    }
                }
                if (level.getBlockEntity(posToConnect) instanceof CableConnectorBlockEntity otherBE) {
                    //CableConnectorBlockEntity connectedBe1 = pos.asLong()>posToConnect.asLong() ? otherBE : be;
                    //CableConnectorBlockEntity connectedBe2= pos.asLong()>posToConnect.asLong() ? be : otherBE;
                    CableType cableType = TFMGUtils.getCableType(cableTypeKey);

                    CableConnection connection1 = new CableConnection(be.getCablePosition(), otherBE.getCablePosition(), otherBE.getBlockPos(), cableType, true);
                    CableConnection connection2 = new CableConnection(otherBE.getCablePosition(), be.getCablePosition(), be.getBlockPos(), cableType, false);

                    float wireCost = (connection1.getLength() / 8);


                    if (stack.get(TFMGDataComponents.SPOOL_AMOUNT) < wireCost * 125) {
                        // Returning PASS here left the first endpoint armed on
                        // the spool with no message, so the spool looked dead:
                        // every later click retried the same over-long run and
                        // failed the same way, even with turns left for a
                        // shorter one. Say why and disarm.
                        if (player != null)
                            player.displayClientMessage(TFMGLang.translateDirect("wires.not_enough_wire")
                                    .withStyle(ChatFormatting.RED), true);
                        stack.remove(TFMGDataComponents.POSITION);
                        stack.remove(TFMGDataComponents.X_POS);
                        stack.remove(TFMGDataComponents.Y_POS);
                        stack.remove(TFMGDataComponents.Z_POS);
                        be.player = null;
                        be.sendData();
                        be.setChanged();
                        return InteractionResult.SUCCESS;
                    }
                    // Endpoint-only test: a second cable of a DIFFERENT material
                    // between the same pair is still a duplicate. Comparing full
                    // connections let a player stack copper, aluminum and
                    // constantan wires on one pair of connectors.
                    if (be.connections.stream().anyMatch(connection1::linksSameEndpoints)
                            || otherBE.connections.stream().anyMatch(connection1::linksSameEndpoints)) {
                        // Same as above: this branch only ever runs server-side,
                        // so the old isClientSide guard meant the player was
                        // never told, and the spool looked like it silently ate
                        // wire for nothing.
                        if (player != null)
                            player.displayClientMessage(TFMGLang.translateDirect("wires.connection_already_created")
                                    .withStyle(ChatFormatting.YELLOW), true);
                        // Clear the stored endpoint too, otherwise the refused
                        // selection stays armed and the next click retries it.
                        stack.remove(TFMGDataComponents.POSITION);
                        be.player = null;
                        be.sendData();
                        be.setChanged();
                        return InteractionResult.SUCCESS;
                    }
                    //  if(!level.isClientSide) {
                    be.connections.add(connection1);
                    otherBE.connections.add(connection2);


                    //   otherBE.onPlaced();
                    //}

                    //  connectedBe1.wiresUpdated();
                    stack.set(TFMGDataComponents.SPOOL_AMOUNT, (int) (stack.get(TFMGDataComponents.SPOOL_AMOUNT) - (wireCost * 125)));
                    be.player = null;
                    otherBE.player = null;
                    be.setChanged();
                    otherBE.setChanged();
                    be.sendData();
                    otherBE.sendData();
                    stack.remove(TFMGDataComponents.POSITION);
                    stack.remove(TFMGDataComponents.X_POS);
                    stack.remove(TFMGDataComponents.Y_POS);
                    stack.remove(TFMGDataComponents.Z_POS);
                }
                //
                be.player = null;
                if (!level.isClientSide()) {

                    be.data.connectNextTick = true;

                }
                return InteractionResult.SUCCESS;
            } else {
                stack.set(TFMGDataComponents.POSITION, be.getBlockPos().asLong());
                stack.set(TFMGDataComponents.X_POS, (int) be.getCablePosition().x());
                stack.set(TFMGDataComponents.Y_POS, (int) be.getCablePosition().y());
                stack.set(TFMGDataComponents.Z_POS, (int) be.getCablePosition().z());
                be.player = player;
                be.color = barColor;
                be.sendData();
                be.setChanged();
                if (!level.isClientSide())
                    be.onPlaced();
                return InteractionResult.SUCCESS;
            }
        }
//
//

        return InteractionResult.PASS;
    }


    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean p_41408_) {


        if (stack.get(TFMGDataComponents.SPOOL_AMOUNT) == null)
            return;

        if (stack.get(TFMGDataComponents.SPOOL_AMOUNT) == 0 && entity instanceof Player player && !stack.is(TFMGItems.EMPTY_SPOOL.get())) {
            player.getInventory().setItem(slot, TFMGItems.EMPTY_SPOOL.asStack());
        }
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return !Objects.equals(cableTypeKey, TFMG.asResource("empty")) && TFMGRegistries.CABLE_TYPE_REGISTRY.containsKey(cableTypeKey);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return barColor;
    }

    @Override
    public int getBarWidth(ItemStack stack) {

        if (stack.get(TFMGDataComponents.SPOOL_AMOUNT) == null)
            return 13;

        return (int) (13f * ((float) stack.get(TFMGDataComponents.SPOOL_AMOUNT) / 1000));
    }

}
