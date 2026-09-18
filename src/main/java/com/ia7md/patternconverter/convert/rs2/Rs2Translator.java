package com.ia7md.patternconverter.convert.rs2;

import com.ia7md.patternconverter.PatternConverter;
import com.ia7md.patternconverter.api.ConversionText;
import com.ia7md.patternconverter.api.PatternConverterApi;
import com.ia7md.patternconverter.api.PatternFormat;
import com.ia7md.patternconverter.api.PatternTranslator;
import com.ia7md.patternconverter.api.Result;
import com.ia7md.patternconverter.api.UniversalKey;
import com.ia7md.patternconverter.api.UniversalPattern;
import com.ia7md.patternconverter.api.UniversalStack;
import com.ia7md.patternconverter.convert.RecipeLookup;
import com.refinedmods.refinedstorage.api.resource.ResourceAmount;
import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.common.autocrafting.CraftingPatternState;
import com.refinedmods.refinedstorage.common.autocrafting.PatternState;
import com.refinedmods.refinedstorage.common.autocrafting.ProcessingPatternState;
import com.refinedmods.refinedstorage.common.autocrafting.SmithingTablePatternState;
import com.refinedmods.refinedstorage.common.autocrafting.StonecutterPatternState;
import com.refinedmods.refinedstorage.common.content.DataComponents;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class Rs2Translator implements PatternTranslator {
    private static final Identifier ID = PatternConverter.asResource("rs2");

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public PatternFormat sourceFormat() {
        return PatternFormat.REFINED_STORAGE;
    }

    @Override
    public boolean matches(ItemStack stack) {
        return Rs2Patterns.isRs2PatternItem(stack) && stack.has(DataComponents.INSTANCE.getPatternState());
    }

    @Override
    public ItemStack blankOf(ItemStack encoded) {
        return Rs2Patterns.blank();
    }

    @Override
    public Result<UniversalPattern> read(ItemStack stack, Level level) {
        PatternState state = stack.get(DataComponents.INSTANCE.getPatternState());
        if (state == null) {
            return Result.fail(ConversionText.blankInInput());
        }
        return switch (state.type()) {
            case CRAFTING -> readCrafting(stack, level);
            case PROCESSING -> readProcessing(stack);
            case STONECUTTER -> readStonecutter(stack, level);
            case SMITHING_TABLE -> readSmithing(stack, level);
        };
    }

    private Result<UniversalPattern> readCrafting(ItemStack stack, Level level) {
        CraftingPatternState state = stack.get(DataComponents.INSTANCE.getCraftingPatternState());
        if (state == null) {
            return Result.fail(ConversionText.undecodable());
        }
        CraftingInput.Positioned positioned = state.input();
        CraftingInput input = positioned.input();
        List<ItemStack> grid = new ArrayList<>(9);
        for (int i = 0; i < 9; i++) {
            grid.add(ItemStack.EMPTY);
        }
        for (int y = 0; y < input.height(); y++) {
            for (int x = 0; x < input.width(); x++) {
                int gx = positioned.left() + x;
                int gy = positioned.top() + y;
                if (gx < 0 || gx > 2 || gy < 0 || gy > 2) {
                    return Result.fail(ConversionText.undecodable());
                }
                grid.set(gy * 3 + gx, input.getItem(x, y).copyWithCount(1));
            }
        }
        Optional<RecipeHolder<CraftingRecipe>> recipe = RecipeLookup.crafting(level, grid, null);
        if (recipe.isEmpty()) {
            return Result.fail(ConversionText.recipeMissing("crafting"));
        }
        ItemStack result = RecipeLookup.craftingResult(level, recipe.get(), grid);
        if (result.isEmpty()) {
            return Result.fail(ConversionText.recipeMissing("crafting"));
        }
        return Result.ok(new UniversalPattern.Crafting(grid, result, recipe.get().id(), state.fuzzyMode(), false));
    }

    private Result<UniversalPattern> readProcessing(ItemStack stack) {
        ProcessingPatternState state = stack.get(DataComponents.INSTANCE.getProcessingPatternState());
        if (state == null) {
            return Result.fail(ConversionText.undecodable());
        }
        List<UniversalPattern.Processing.Input> inputs = new ArrayList<>();
        for (Optional<ProcessingPatternState.ProcessingIngredient> slot : state.ingredients()) {
            if (slot.isEmpty()) {
                continue;
            }
            ProcessingPatternState.ProcessingIngredient ingredient = slot.get();
            ResourceAmount primary = ingredient.input();
            Optional<UniversalKey> key = PatternConverterApi.rs2ToUniversal(primary.resource());
            if (key.isEmpty()) {
                return Result.fail(unknown(primary.resource()));
            }
            List<UniversalKey> alternatives = new ArrayList<>();
            if (!ingredient.allowedAlternativeIds().isEmpty()) {
                for (ResourceKey alt : ingredient.calculateInputsIncludingAlternatives()) {
                    if (alt.equals(primary.resource())) {
                        continue;
                    }
                    PatternConverterApi.rs2ToUniversal(alt).ifPresent(alternatives::add);
                }
            }
            inputs.add(new UniversalPattern.Processing.Input(
                    new UniversalStack(key.get(), primary.amount()), alternatives));
        }
        List<UniversalStack> outputs = new ArrayList<>();
        for (Optional<ResourceAmount> slot : state.outputs()) {
            if (slot.isEmpty()) {
                continue;
            }
            ResourceAmount output = slot.get();
            Optional<UniversalKey> key = PatternConverterApi.rs2ToUniversal(output.resource());
            if (key.isEmpty()) {
                return Result.fail(unknown(output.resource()));
            }
            outputs.add(new UniversalStack(key.get(), output.amount()));
        }
        if (inputs.isEmpty() || outputs.isEmpty()) {
            return Result.fail(ConversionText.emptyPattern());
        }
        return Result.ok(new UniversalPattern.Processing(inputs, outputs));
    }

    private Result<UniversalPattern> readStonecutter(ItemStack stack, Level level) {
        StonecutterPatternState state = stack.get(DataComponents.INSTANCE.getStonecutterPatternState());
        if (state == null) {
            return Result.fail(ConversionText.undecodable());
        }
        ItemStack input = state.input().toItemStack();
        ItemStack output = state.selectedOutput().toItemStack();
        Optional<RecipeHolder<StonecutterRecipe>> recipe = RecipeLookup.stonecutting(level, input, output, null);
        if (recipe.isEmpty()) {
            return Result.fail(ConversionText.recipeMissing("stonecutting"));
        }
        ItemStack result = RecipeLookup.stonecuttingResult(recipe.get(), input);
        return Result.ok(new UniversalPattern.Stonecutting(input, result, recipe.get().id(), false));
    }

    private Result<UniversalPattern> readSmithing(ItemStack stack, Level level) {
        SmithingTablePatternState state = stack.get(DataComponents.INSTANCE.getSmithingTablePatternState());
        if (state == null) {
            return Result.fail(ConversionText.undecodable());
        }
        ItemStack template = state.template().toItemStack();
        ItemStack base = state.base().toItemStack();
        ItemStack addition = state.addition().toItemStack();
        Optional<RecipeHolder<SmithingRecipe>> recipe = RecipeLookup.smithing(level, template, base, addition, null);
        if (recipe.isEmpty()) {
            return Result.fail(ConversionText.recipeMissing("smithing"));
        }
        ItemStack result = RecipeLookup.smithingResult(recipe.get(), template, base, addition);
        if (result.isEmpty()) {
            return Result.fail(ConversionText.recipeMissing("smithing"));
        }
        return Result.ok(new UniversalPattern.Smithing(template, base, addition, result, recipe.get().id(), false));
    }

    static net.minecraft.network.chat.Component unknown(ResourceKey key) {
        return ConversionText.unknownResource(key.getClass().getSimpleName(), PatternFormat.REFINED_STORAGE);
    }
}
