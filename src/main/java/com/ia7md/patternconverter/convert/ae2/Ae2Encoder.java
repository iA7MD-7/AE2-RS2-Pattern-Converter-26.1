package com.ia7md.patternconverter.convert.ae2;

import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.crafting.pattern.AEProcessingPattern;
import com.ia7md.patternconverter.PCConfig;
import com.ia7md.patternconverter.api.ConversionText;
import com.ia7md.patternconverter.api.PatternConverterApi;
import com.ia7md.patternconverter.api.PatternEncoder;
import com.ia7md.patternconverter.api.PatternFormat;
import com.ia7md.patternconverter.api.Result;
import com.ia7md.patternconverter.api.UniversalPattern;
import com.ia7md.patternconverter.convert.KeyMapping;
import com.ia7md.patternconverter.convert.RecipeLookup;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class Ae2Encoder implements PatternEncoder {
    @Override
    public PatternFormat targetFormat() {
        return PatternFormat.APPLIED_ENERGISTICS;
    }

    @Override
    public boolean isBlank(ItemStack stack) {
        return Ae2Patterns.isBlank(stack);
    }

    @Override
    public ItemStack blankStack() {
        return Ae2Patterns.blank();
    }

    @Override
    public Result<ItemStack> encode(UniversalPattern pattern, Level level) {
        return switch (pattern) {
            case UniversalPattern.Crafting c -> encodeCrafting(c, level);
            case UniversalPattern.Processing p -> encodeProcessing(p);
            case UniversalPattern.Stonecutting s -> encodeStonecutting(s, level);
            case UniversalPattern.Smithing s -> encodeSmithing(s, level);
        };
    }

    private Result<ItemStack> encodeCrafting(UniversalPattern.Crafting c, Level level) {
        Optional<RecipeHolder<CraftingRecipe>> recipe = RecipeLookup.crafting(level, c.grid(), c.recipeId());
        if (recipe.isEmpty()) {
            return Result.fail(ConversionText.recipeMissing("crafting"));
        }
        ItemStack result = RecipeLookup.craftingResult(level, recipe.get(), c.grid());
        if (result.isEmpty()) {
            return Result.fail(ConversionText.recipeMissing("crafting"));
        }
        ItemStack[] grid = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            grid[i] = c.grid().get(i).copy();
        }
        return Result.ok(PatternDetailsHelper.encodeCraftingPattern(recipe.get(), grid, result,
                c.substitute(), c.fluidSubstitute()));
    }

    private Result<ItemStack> encodeProcessing(UniversalPattern.Processing p) {
        List<Component> warnings = new ArrayList<>();
        Result<List<KeyMapping.MappedInput<AEKey>>> inputs =
                KeyMapping.mapInputs(p.inputs(), targetFormat(), PatternConverterApi::universalToAe2);
        if (!inputs.isOk()) {
            return inputs.castFailure();
        }
        Result<List<KeyMapping.Mapped<AEKey>>> outputs =
                KeyMapping.mapOutputs(p.outputs(), targetFormat(), PatternConverterApi::universalToAe2);
        if (!outputs.isOk()) {
            return outputs.castFailure();
        }
        warnings.addAll(outputs.warnings());

        List<KeyMapping.Mapped<AEKey>> primaries = new ArrayList<>();
        for (KeyMapping.MappedInput<AEKey> input : inputs.get()) {
            if (!input.alternatives().isEmpty() || input.droppedAlternatives()) {
                if (!PCConfig.DROP_ALTERNATIVE_INPUTS.get()) {
                    return Result.fail(ConversionText.alternativesRejected());
                }
                warnings.add(ConversionText.alternativesDropped(input.primary().universal()));
            }
            primaries.add(input.primary());
        }
        List<KeyMapping.Mapped<AEKey>> condensedIn = KeyMapping.condense(primaries, new ArrayList<>());
        List<KeyMapping.Mapped<AEKey>> condensedOut = KeyMapping.condense(outputs.get(), new ArrayList<>());
        if (condensedIn.size() > AEProcessingPattern.MAX_INPUT_SLOTS) {
            return Result.fail(ConversionText.tooManyInputs(targetFormat(), condensedIn.size(),
                    AEProcessingPattern.MAX_INPUT_SLOTS));
        }
        if (condensedOut.size() > AEProcessingPattern.MAX_OUTPUT_SLOTS) {
            return Result.fail(ConversionText.tooManyOutputs(targetFormat(), condensedOut.size(),
                    AEProcessingPattern.MAX_OUTPUT_SLOTS));
        }
        List<GenericStack> in = condensedIn.stream().map(m -> new GenericStack(m.key(), m.amount())).toList();
        List<GenericStack> out = condensedOut.stream().map(m -> new GenericStack(m.key(), m.amount())).toList();
        return Result.ok(PatternDetailsHelper.encodeProcessingPattern(in, out), warnings);
    }

    private Result<ItemStack> encodeStonecutting(UniversalPattern.Stonecutting s, Level level) {
        Optional<RecipeHolder<StonecutterRecipe>> recipe =
                RecipeLookup.stonecutting(level, s.input(), s.output(), s.recipeId());
        if (recipe.isEmpty()) {
            return Result.fail(ConversionText.recipeMissing("stonecutting"));
        }
        ItemStack result = RecipeLookup.stonecuttingResult(recipe.get(), s.input());
        return Result.ok(PatternDetailsHelper.encodeStonecuttingPattern(recipe.get(),
                AEItemKey.of(s.input()), AEItemKey.of(result), s.substitute()));
    }

    private Result<ItemStack> encodeSmithing(UniversalPattern.Smithing s, Level level) {
        Optional<RecipeHolder<SmithingRecipe>> recipe =
                RecipeLookup.smithing(level, s.template(), s.base(), s.addition(), s.recipeId());
        if (recipe.isEmpty()) {
            return Result.fail(ConversionText.recipeMissing("smithing"));
        }
        ItemStack result = RecipeLookup.smithingResult(recipe.get(), s.template(), s.base(), s.addition());
        if (result.isEmpty()) {
            return Result.fail(ConversionText.recipeMissing("smithing"));
        }
        return Result.ok(PatternDetailsHelper.encodeSmithingTablePattern(recipe.get(),
                AEItemKey.of(s.template()), AEItemKey.of(s.base()), AEItemKey.of(s.addition()),
                AEItemKey.of(result), s.substitute()));
    }
}
