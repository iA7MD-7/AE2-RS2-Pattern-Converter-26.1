package com.ia7md.patternconverter.compat.jei;

import com.ia7md.patternconverter.PatternConverter;
import com.ia7md.patternconverter.api.ConversionDirection;
import com.ia7md.patternconverter.compat.AddonCompat;
import com.ia7md.patternconverter.convert.ae2.Ae2Patterns;
import com.ia7md.patternconverter.convert.rs2.Rs2Patterns;
import com.ia7md.patternconverter.registry.PCItems;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;

import com.ia7md.patternconverter.client.PCSprites;
import com.ia7md.patternconverter.client.PatternConverterScreen;

import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

@JeiPlugin
public class PCJeiPlugin implements IModPlugin {
    public static final RecipeType<ConversionDisplay> CONVERSION =
            RecipeType.create(PatternConverter.MOD_ID, "conversion", ConversionDisplay.class);

    @Override
    public Identifier getPluginUid() {
        return PatternConverter.asResource("jei");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new ConversionJeiCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        ItemStack in = new ItemStack(Items.COBBLESTONE);
        ItemStack out = new ItemStack(Items.STONE);
        ItemStack rsPattern = Rs2Patterns.exampleProcessingPattern(in, out);
        ItemStack aePattern = Ae2Patterns.exampleProcessingPattern(in, out);

        registration.addRecipes(CONVERSION, List.of(
                new ConversionDisplay(ConversionDirection.RS_TO_AE, List.of(rsPattern), Ae2Patterns.blank(),
                        List.of(aePattern), Rs2Patterns.blank()),
                new ConversionDisplay(ConversionDirection.AE_TO_RS, List.of(aePattern), Rs2Patterns.blank(),
                        List.of(rsPattern), Ae2Patterns.blank())));

        String addons = AddonCompat.active().isEmpty()
                ? Component.translatable("jei.pattern_converter.info.no_addons").getString()
                : String.join(", ", AddonCompat.active());
        registration.addIngredientInfo(PCItems.PATTERN_CONVERTER.get(),
                Component.translatable("jei.pattern_converter.info.1"),
                Component.translatable("jei.pattern_converter.info.2"),
                Component.translatable("jei.pattern_converter.info.3"),
                Component.translatable("jei.pattern_converter.info.addons", addons));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(PCItems.PATTERN_CONVERTER.get(), CONVERSION);
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGuiContainerHandler(PatternConverterScreen.class, new IGuiContainerHandler<>() {
            @Override
            public List<Rect2i> getGuiExtraAreas(PatternConverterScreen screen) {
                return List.of(new Rect2i(screen.getGuiLeft() + PCSprites.SIDE_BUTTON_X,
                        screen.getGuiTop() + PCSprites.SIDE_BUTTON_Y, PCSprites.ICON_SIZE, PCSprites.ICON_SIZE));
            }
        });
    }
}
