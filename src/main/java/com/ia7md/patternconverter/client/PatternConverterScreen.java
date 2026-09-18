package com.ia7md.patternconverter.client;

import com.ia7md.patternconverter.PatternConverter;
import com.ia7md.patternconverter.api.ConversionDirection;
import com.ia7md.patternconverter.api.PatternFormat;
import com.ia7md.patternconverter.block.DirectionMode;
import com.ia7md.patternconverter.block.PatternConverterBlockEntity;
import com.ia7md.patternconverter.block.SlotStatus;
import com.ia7md.patternconverter.menu.PatternConverterMenu;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.ia7md.patternconverter.client.PCSprites.ARROW_H;
import static com.ia7md.patternconverter.client.PCSprites.ARROW_INK_W;
import static com.ia7md.patternconverter.client.PCSprites.ARROW_INK_X;
import static com.ia7md.patternconverter.client.PCSprites.ARROW_U;
import static com.ia7md.patternconverter.client.PCSprites.ARROW_W;
import static com.ia7md.patternconverter.client.PCSprites.BUTTON_BG_HOVER_V;
import static com.ia7md.patternconverter.client.PCSprites.BUTTON_BG_U;
import static com.ia7md.patternconverter.client.PCSprites.BUTTON_BG_V;
import static com.ia7md.patternconverter.client.PCSprites.GHOST_AE_U;
import static com.ia7md.patternconverter.client.PCSprites.GHOST_RS_U;
import static com.ia7md.patternconverter.client.PCSprites.GHOST_V;
import static com.ia7md.patternconverter.client.PCSprites.ICON_HOVER_V;
import static com.ia7md.patternconverter.client.PCSprites.ICON_ISSUES_U;
import static com.ia7md.patternconverter.client.PCSprites.ICON_SIZE;
import static com.ia7md.patternconverter.client.PCSprites.ICON_V;
import static com.ia7md.patternconverter.client.PCSprites.PANEL_HEIGHT;
import static com.ia7md.patternconverter.client.PCSprites.PANEL_WIDTH;
import static com.ia7md.patternconverter.client.PCSprites.SHEET;
import static com.ia7md.patternconverter.client.PCSprites.SIDE_BUTTON_X;
import static com.ia7md.patternconverter.client.PCSprites.SIDE_BUTTON_Y;
import static com.ia7md.patternconverter.client.PCSprites.arrowV;

public class PatternConverterScreen extends AbstractContainerScreen<PatternConverterMenu> {
    private static final String FALLBACK_ISSUES_URL = "https://github.com/iA7MD-7/AE2-RS2-Pattern-Converter-26.1/issues";
    private static final int SHEET_SIZE = 256;

    private static final int GAP_X = 61;
    private static final int GAP_W = 54;
    private static final int ARROW_X = GAP_X + (GAP_W - ARROW_INK_W) / 2;
    private static final int ARROW_Y = 38;
    private static final int ISSUES_X = SIDE_BUTTON_X;
    private static final int ISSUES_Y = SIDE_BUTTON_Y;

    private static final int TINT_ERROR = 0x50FF3030;
    private static final int TINT_WAITING = 0x40FFD040;
    private static final int TINT_QUEUED = 0x2080C0FF;
    private static final int COLOR_AUTO = 0xFF404040;
    private static final int COLOR_LOCKED = 0xFF8A4A00;

    private static final long GHOST_CYCLE_MS = 2000L;

    private ArrowButton arrowButton;
    private String arrowTooltipKey;

    public PatternConverterScreen(PatternConverterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, PANEL_WIDTH, PANEL_HEIGHT);
        inventoryLabelY = PatternConverterMenu.PLAYER_INV_Y - 12;
    }

    @Override
    protected void init() {
        super.init();
        arrowButton = addRenderableWidget(new ArrowButton(leftPos + ARROW_X, topPos + ARROW_Y));
        IconButton issues = addRenderableWidget(new IconButton(leftPos + ISSUES_X, topPos + ISSUES_Y, ICON_ISSUES_U,
                Component.translatable("gui.pattern_converter.issues.title"),
                () -> ConfirmLinkScreen.confirmLinkNow(this, issuesUrl())));
        issues.setTooltip(Tooltip.create(Component.translatable("gui.pattern_converter.issues.title").append("\n")
                .append(Component.translatable("gui.pattern_converter.issues.desc").withStyle(ChatFormatting.GRAY))));
        refreshArrowTooltip();
    }

    private static String issuesUrl() {
        return ModList.get().getModContainerById(PatternConverter.MOD_ID)
                .flatMap(container -> container.getModInfo().getOwningFile().getConfig()
                        .<String>getConfigElement("issueTrackerURL"))
                .orElse(FALLBACK_ISSUES_URL);
    }

    private static void blit(GuiGraphicsExtractor graphics, int x, int y, int u, int v, int w, int h) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, SHEET, x, y, u, v, w, h, SHEET_SIZE, SHEET_SIZE);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        blit(graphics, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        ConversionDirection direction = menu.getActiveDirection();

        ConversionDirection hinted = hintedDirection(direction);
        PatternFormat source = hinted.source();
        PatternFormat target = hinted.target();
        for (int i = 0; i < PatternConverterBlockEntity.INPUT_COUNT; i++) {
            Slot slot = menu.slots.get(PatternConverterBlockEntity.INPUT_START + i);
            if (!slot.hasItem()) {
                blitGhost(graphics, slot, source);
            }
        }
        for (int i = 0; i < PatternConverterBlockEntity.OUTPUT_COUNT; i++) {
            Slot slot = menu.slots.get(PatternConverterBlockEntity.OUTPUT_START + i);
            if (!slot.hasItem()) {
                blitGhost(graphics, slot, target);
            }
        }
        Slot supplySlot = menu.slots.get(PatternConverterBlockEntity.BLANK_SUPPLY_SLOT);
        if (!supplySlot.hasItem()) {
            blitGhost(graphics, supplySlot, target);
        }
        Slot returnSlot = menu.slots.get(PatternConverterBlockEntity.BLANK_RETURN_SLOT);
        if (!returnSlot.hasItem()) {
            blitGhost(graphics, returnSlot, source);
        }

        PatternConverterBlockEntity be = menu.getBlockEntity();
        for (int i = 0; i < PatternConverterBlockEntity.INPUT_COUNT; i++) {
            Slot slot = menu.slots.get(PatternConverterBlockEntity.INPUT_START + i);
            if (!slot.hasItem()) {
                continue;
            }
            SlotStatus status = be != null ? be.getStatus(i) : SlotStatus.EMPTY;
            int tint = switch (status.kind()) {
                case ERROR -> TINT_ERROR;
                case WAITING -> TINT_WAITING;
                case QUEUED -> TINT_QUEUED;
                case EMPTY -> 0;
            };
            if (tint != 0) {
                int x = leftPos + slot.x;
                int y = topPos + slot.y;
                graphics.fill(x, y, x + 16, y + 16, tint);
            }
        }
    }

    private ConversionDirection hintedDirection(@Nullable ConversionDirection active) {
        if (active != null) {
            return active;
        }
        ConversionDirection forced = menu.getMode().forced();
        if (forced != null) {
            return forced;
        }
        return (System.currentTimeMillis() / GHOST_CYCLE_MS) % 2 == 0
                ? ConversionDirection.RS_TO_AE : ConversionDirection.AE_TO_RS;
    }

    private void blitGhost(GuiGraphicsExtractor graphics, Slot slot, PatternFormat format) {
        int u = format == PatternFormat.REFINED_STORAGE ? GHOST_RS_U : GHOST_AE_U;
        blit(graphics, leftPos + slot.x, topPos + slot.y, u, GHOST_V, 16, 16);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        DirectionMode mode = menu.getMode();
        boolean auto = mode == DirectionMode.AUTO;
        Component label = Component.translatable(auto
                ? "gui.pattern_converter.direction.auto" : "gui.pattern_converter.direction.locked");
        int x = imageWidth - inventoryLabelX - font.width(label);
        graphics.text(font, label, x, inventoryLabelY, auto ? COLOR_AUTO : COLOR_LOCKED, false);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        refreshArrowTooltip();
        super.extractTooltip(graphics, mouseX, mouseY);
        if (hoveredSlot == null || hoveredSlot.hasItem()) {
            return;
        }
        int index = hoveredSlot.index;
        Component hint = null;
        if (index >= PatternConverterBlockEntity.INPUT_START
                && index < PatternConverterBlockEntity.INPUT_START + PatternConverterBlockEntity.INPUT_COUNT) {
            hint = Component.translatable("gui.pattern_converter.slot.input");
        } else if (index >= PatternConverterBlockEntity.OUTPUT_START
                && index < PatternConverterBlockEntity.OUTPUT_START + PatternConverterBlockEntity.OUTPUT_COUNT) {
            hint = Component.translatable("gui.pattern_converter.slot.output");
        } else if (index == PatternConverterBlockEntity.BLANK_SUPPLY_SLOT) {
            hint = Component.translatable("gui.pattern_converter.slot.blank_supply");
        } else if (index == PatternConverterBlockEntity.BLANK_RETURN_SLOT) {
            hint = Component.translatable("gui.pattern_converter.slot.blank_return");
        }
        if (hint != null) {
            graphics.setTooltipForNextFrame(font, hint.copy().withStyle(ChatFormatting.GRAY), mouseX, mouseY);
        }
    }

    @Override
    protected List<Component> getTooltipFromContainerItem(ItemStack stack) {
        List<Component> lines = new ArrayList<>(super.getTooltipFromContainerItem(stack));
        PatternConverterBlockEntity be = menu.getBlockEntity();
        if (hoveredSlot != null && be != null
                && hoveredSlot.index >= PatternConverterBlockEntity.INPUT_START
                && hoveredSlot.index < PatternConverterBlockEntity.INPUT_START + PatternConverterBlockEntity.INPUT_COUNT) {
            SlotStatus status = be.getStatus(hoveredSlot.index - PatternConverterBlockEntity.INPUT_START);
            status.message().ifPresent(message -> {
                lines.add(Component.empty());
                lines.add(message);
            });
        }
        return lines;
    }

    private void refreshArrowTooltip() {
        if (arrowButton == null) {
            return;
        }
        DirectionMode mode = menu.getMode();
        ConversionDirection direction = menu.getActiveDirection();
        PatternConverterBlockEntity be = menu.getBlockEntity();
        Component event = be != null ? be.getLastEvent() : Component.empty();
        List<Component> notes = be != null ? be.getNotes() : List.of();
        String key = mode + "|" + direction + "|" + event.getString() + "|" + notes.size();
        if (key.equals(arrowTooltipKey)) {
            return;
        }
        arrowTooltipKey = key;

        MutableComponent text = Component.translatable("gui.pattern_converter.arrows.title").append("\n")
                .append((direction != null ? direction.displayName().copy()
                        : Component.translatable("gui.pattern_converter.direction.none")).withStyle(ChatFormatting.GRAY))
                .append("\n")
                .append(Component.translatable("gui.pattern_converter.override.current", mode.displayName())
                        .withStyle(ChatFormatting.GRAY)).append("\n")
                .append(Component.translatable("gui.pattern_converter.override.next", mode.next().displayName())
                        .withStyle(ChatFormatting.DARK_GRAY));
        if (be != null) {
            text.append("\n").append(event);
            if (!notes.isEmpty()) {
                text.append("\n").append(Component.translatable("gui.pattern_converter.notes").withStyle(ChatFormatting.GRAY));
                for (Component note : notes) {
                    text.append("\n").append(note);
                }
            }
        }
        arrowButton.setTooltip(Tooltip.create(text));
    }

    private abstract static class SpriteButton extends AbstractWidget {
        SpriteButton(int x, int y, int width, int height, Component narration) {
            super(x, y, width, height, narration);
        }

        protected abstract void press();

        @Override
        public void onClick(MouseButtonEvent event, boolean doubleClick) {
            press();
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }

    private final class ArrowButton extends SpriteButton {
        ArrowButton(int x, int y) {
            super(x, y, ARROW_INK_W, ARROW_H, Component.translatable("gui.pattern_converter.override.title"));
        }

        @Override
        protected void press() {
            if (minecraft != null && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, PatternConverterMenu.BUTTON_CYCLE_MODE);
            }
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            blit(graphics, getX() - ARROW_INK_X, getY(), ARROW_U, arrowV(menu.getActiveDirection()), ARROW_W, ARROW_H);
            if (isHoveredOrFocused()) {
                graphics.fill(getX() - 1, getY() - 1, getX() + ARROW_INK_W + 1, getY() + ARROW_H + 1, 0x30FFFFFF);
            }
        }
    }

    private static final class IconButton extends SpriteButton {
        private final int iconU;
        private final Runnable action;

        IconButton(int x, int y, int iconU, Component narration, Runnable action) {
            super(x, y, ICON_SIZE, ICON_SIZE, narration);
            this.iconU = iconU;
            this.action = action;
        }

        @Override
        protected void press() {
            action.run();
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            boolean hover = isHoveredOrFocused();
            blit(graphics, getX(), getY(), BUTTON_BG_U, hover ? BUTTON_BG_HOVER_V : BUTTON_BG_V, ICON_SIZE, ICON_SIZE);
            blit(graphics, getX(), getY(), iconU, hover ? ICON_HOVER_V : ICON_V, ICON_SIZE, ICON_SIZE);
        }
    }
}
