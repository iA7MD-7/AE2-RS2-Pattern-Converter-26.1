package com.ia7md.patternconverter.compat.jei;

import com.ia7md.patternconverter.api.ConversionDirection;
import com.ia7md.patternconverter.client.PCSprites;
import com.ia7md.patternconverter.registry.PCItems;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class ConversionJeiCategory implements IRecipeCategory<ConversionDisplay> {
    private static final Identifier SHEET = PCSprites.SHEET;
    private static final int WIDTH = 150;
    private static final int HEIGHT = 58;

    private final IDrawable icon;
    private final IDrawable arrowRsToAe;
    private final IDrawable arrowAeToRs;

    public ConversionJeiCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemLike(PCItems.PATTERN_CONVERTER.get());
        this.arrowRsToAe = guiHelper.createDrawable(SHEET, PCSprites.ARROW_U, PCSprites.ARROW_RS_TO_AE_V,
                PCSprites.ARROW_W, PCSprites.ARROW_H);
        this.arrowAeToRs = guiHelper.createDrawable(SHEET, PCSprites.ARROW_U, PCSprites.ARROW_AE_TO_RS_V,
                PCSprites.ARROW_W, PCSprites.ARROW_H);
    }

    @Override
    public RecipeType<ConversionDisplay> getRecipeType() {
        return PCJeiPlugin.CONVERSION;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.pattern_converter.category");
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ConversionDisplay recipe, IFocusGroup focuses) {
        builder.addInputSlot(4, 10).setStandardSlotBackground().addItemStacks(recipe.sourcePatterns());
        builder.addInputSlot(34, 10).setStandardSlotBackground().addItemStack(recipe.targetBlank());
        builder.addOutputSlot(98, 10).setOutputSlotBackground().addItemStacks(recipe.targetPatterns());
        builder.addOutputSlot(128, 10).setOutputSlotBackground().addItemStack(recipe.sourceBlank());
    }

    @Override
    public void draw(ConversionDisplay recipe, IRecipeSlotsView slotsView, GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        graphics.text(font, "+", 25, 15, 0xFF404040, false);
        graphics.text(font, "+", 119, 15, 0xFF404040, false);
        (recipe.direction() == ConversionDirection.RS_TO_AE ? arrowRsToAe : arrowAeToRs).draw(graphics, 63, 13);
        Component caption = recipe.direction().displayName().copy().withStyle(ChatFormatting.DARK_GRAY);
        int captionWidth = font.width(caption);
        graphics.text(font, caption, (WIDTH - captionWidth) / 2, 36, 0xFF404040, false);
        Component note = Component.translatable("jei.pattern_converter.shapes").withStyle(ChatFormatting.GRAY);
        int noteWidth = font.width(note);
        graphics.text(font, note, (WIDTH - noteWidth) / 2, 47, 0xFF404040, false);
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, ConversionDisplay recipe, IRecipeSlotsView slotsView,
                           double mouseX, double mouseY) {
        if (mouseX >= 63 && mouseX < 87 && mouseY >= 13 && mouseY < 25) {
            tooltip.add(Component.translatable("jei.pattern_converter.arrow", recipe.source().displayName(),
                    recipe.target().displayName()));
        }
    }
}
