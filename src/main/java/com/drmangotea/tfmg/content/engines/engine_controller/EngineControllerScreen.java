package com.drmangotea.tfmg.content.engines.engine_controller;

import com.drmangotea.tfmg.base.lang.TFMGLang;
import com.drmangotea.tfmg.registry.TFMGGuiTextures;
import com.drmangotea.tfmg.registry.TFMGKeys;
import net.minecraft.client.KeyMapping;
import com.google.common.collect.ImmutableList;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import com.simibubi.create.foundation.gui.widget.IconButton;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static com.simibubi.create.foundation.gui.AllGuiTextures.PLAYER_INVENTORY;

public class EngineControllerScreen extends AbstractSimiContainerScreen<EngineControllerMenu> {

	protected TFMGGuiTextures background;
	private List<Rect2i> extraAreas = Collections.emptyList();

	private IconButton resetButton;
	private IconButton confirmButton;

	public EngineControllerScreen(EngineControllerMenu menu, Inventory inv, Component title) {
		super(menu, inv, title);
		this.background = TFMGGuiTextures.ENGINE_CONTROLLER;
	}

	@Override
	protected void init() {
		setWindowSize(background.width, background.height + 4 + PLAYER_INVENTORY.getHeight());
		setWindowOffset(50, 0);
		super.init();

		int x = leftPos;
		int y = topPos;

		resetButton = new IconButton(x + background.width - 170, y + background.height - 24, AllIcons.I_TRASH);
		resetButton.withCallback(() -> {
			menu.clearContents();
			menu.sendClearPacket();
		});
		confirmButton = new IconButton(x + background.width -117, y + background.height - 24, AllIcons.I_CONFIRM);
		confirmButton.withCallback(() -> {
			minecraft.player.closeContainer();
		});

		addRenderableWidget(resetButton);
		addRenderableWidget(confirmButton);

		extraAreas = ImmutableList.of(new Rect2i(x + background.width + 4, y + background.height - 44, 64, 56));
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {


		int invX = getLeftOfCentered(PLAYER_INVENTORY.getWidth());
		int invY = topPos + background.height + 4;
		renderPlayerInventory(graphics, invX, invY);

		int x = leftPos;
		int y = topPos;

		background.render(graphics, x, y);
		graphics.drawString(font, title, x + 15, y + 4, 0x592424, false);
		GuiGameElement.of(menu.contentHolder).<GuiGameElement
			.GuiRenderBuilder>at(x + background.width - 4, y + background.height - 56, -200)
			.scale(5)
			.render(graphics);
	}

	@Override
	protected void containerTick() {


		super.containerTick();
	}

	@Override
	protected void renderTooltip(GuiGraphics graphics, int x, int y) {
		if (!menu.getCarried()
			.isEmpty() || this.hoveredSlot == null || hoveredSlot.container == menu.playerInventory) {
			super.renderTooltip(graphics, x, y);
			return;
		}

		List<Component> list = new LinkedList<>();
		if (hoveredSlot.hasItem())
			list = getTooltipFromContainerItem(hoveredSlot.getItem());

		graphics.renderComponentTooltip(font, addToTooltip(list, hoveredSlot.getSlotIndex()), x, y);
	}

	private List<Component> addToTooltip(List<Component> list, int slot) {
		if (slot < 0 || slot >= 6)
			return list;
		// The server maps pair 0 to the steer-left key, pair 1 to steer-right
		// and pair 2 to the custom button; the old code labelled them with
		// Create's movement keys through a lang key that never existed.
		KeyMapping key = switch (slot / 2) {
			case 0 -> minecraft.options.keyLeft;
			case 1 -> minecraft.options.keyRight;
			default -> TFMGKeys.ENGINE_CONTROLLER_CUSTOM_BUTTON.getKeybind();
		};
		list.add(TFMGLang.translateDirect("engine_controller.frequency_slot_" + ((slot % 2) + 1), key
			.getTranslatedKeyMessage()
			.getString())
			.withStyle(ChatFormatting.GOLD));
		return list;
	}

	@Override
	public List<Rect2i> getExtraAreas() {
		return extraAreas;
	}

}