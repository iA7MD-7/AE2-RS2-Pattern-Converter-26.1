package com.ia7md.patternconverter.convert.rs2;

import com.ia7md.patternconverter.PCConfig;
import com.ia7md.patternconverter.api.ConversionText;
import com.ia7md.patternconverter.api.PatternConverterApi;
import com.ia7md.patternconverter.api.PatternEncoder;
import com.ia7md.patternconverter.api.PatternFormat;
import com.ia7md.patternconverter.api.Result;
import com.ia7md.patternconverter.api.UniversalPattern;
import com.ia7md.patternconverter.convert.KeyMapping;
import com.ia7md.patternconverter.convert.RecipeLookup;
import com.refinedmods.refinedstorage.api.resource.ResourceAmount;
import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.common.api.support.resource.PlatformResourceKey;
import com.refinedmods.refinedstorage.common.autocrafting.CraftingPatternState;
import com.refinedmods.refinedstorage.common.autocrafting.ProcessingPatternState;
import com.refinedmods.refinedstorage.common.autocrafting.SmithingTablePatternState;
import com.refinedmods.refinedstorage.common.autocrafting.StonecutterPatternState;
import com.refinedmods.refinedstorage.common.autocrafting.patterngrid.PatternType;
import com.refinedmods.refinedstorage.common.content.DataComponents;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class Rs2Encoder implements PatternEncoder {
    private static final int MAX_PROCESSING_SLOTS = 81;

    @Override
    public PatternFormat targetFormat() {
        return PatternFormat.REFINED_STORAGE;
    }

    @Override
    public boolean isBlank(ItemStack stack) {
        return Rs2Patterns.isBlank(stack);
    }

    @Override
    public ItemStack blankStack() {
        return Rs2Patterns.blank();
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
        List<Component> warnings = new ArrayList<>();
        if (c.fluidSubstitute()) {
            if (!PCConfig.DROP_FLUID_SUBSTITUTION.get()) {
                return Result.fail(ConversionText.fluidSubstitutionRejected());
            }
            warnings.add(ConversionText.fluidSubstitutionDropped());
        }
        if (RecipeLookup.crafting(level, c.grid(), c.recipeId()).isEmpty()) {
            return Result.fail(ConversionText.recipeMissing("crafting"));
        }
        CraftingInput.Positioned positioned = CraftingInput.ofPositioned(3, 3, c.grid());
        ItemStack stack = Rs2Patterns.newEncoded(PatternType.CRAFTING);
        stack.set(DataComponents.INSTANCE.getCraftingPatternState(),
                new CraftingPatternState(c.substitute(), positioned));
        return Result.ok(stack, warnings);
    }

    private Result<ItemStack> encodeProcessing(UniversalPattern.Processing p) {
        List<Component> warnings = new ArrayList<>();
        Result<List<KeyMapping.MappedInput<ResourceKey>>> inputs =
                KeyMapping.mapInputs(p.inputs(), targetFormat(), PatternConverterApi::universalToRs2);
        if (!inputs.isOk()) {
            return inputs.castFailure();
        }
        Result<List<KeyMapping.Mapped<ResourceKey>>> outputs =
                KeyMapping.mapOutputs(p.outputs(), targetFormat(), PatternConverterApi::universalToRs2);
        if (!outputs.isOk()) {
            return outputs.castFailure();
        }
        warnings.addAll(outputs.warnings());

        List<Optional<ProcessingPatternState.ProcessingIngredient>> ingredients = new ArrayList<>();
        for (KeyMapping.MappedInput<ResourceKey> input : inputs.get()) {
            int before = ingredients.size();
            for (ResourceAmount chunk : split(input.primary())) {
                ingredients.add(Optional.of(new ProcessingPatternState.ProcessingIngredient(chunk, List.of())));
            }
            if (ingredients.size() - before > 1) {
                warnings.add(ConversionText.amountSplit(input.primary().universal(), ingredients.size() - before));
            }
        }
        if (ingredients.size() > MAX_PROCESSING_SLOTS) {
            return Result.fail(ConversionText.tooManyInputs(targetFormat(), ingredients.size(), MAX_PROCESSING_SLOTS));
        }
        List<Optional<ResourceAmount>> results = new ArrayList<>();
        for (KeyMapping.Mapped<ResourceKey> output : outputs.get()) {
            int before = results.size();
            for (ResourceAmount chunk : split(output)) {
                results.add(Optional.of(chunk));
            }
            if (results.size() - before > 1) {
                warnings.add(ConversionText.amountSplit(output.universal(), results.size() - before));
            }
        }
        if (results.size() > MAX_PROCESSING_SLOTS) {
            return Result.fail(ConversionText.tooManyOutputs(targetFormat(), results.size(), MAX_PROCESSING_SLOTS));
        }
        while (ingredients.size() < MAX_PROCESSING_SLOTS) {
            ingredients.add(Optional.empty());
        }
        while (results.size() < MAX_PROCESSING_SLOTS) {
            results.add(Optional.empty());
        }
        ItemStack stack = Rs2Patterns.newEncoded(PatternType.PROCESSING);
        stack.set(DataComponents.INSTANCE.getProcessingPatternState(),
                new ProcessingPatternState(ingredients, results));
        return Result.ok(stack, warnings);
    }

    private static List<ResourceAmount> split(KeyMapping.Mapped<ResourceKey> mapped) {
        long limit = mapped.key() instanceof PlatformResourceKey platform
                ? Math.max(1, platform.getProcessingPatternLimit()) : Long.MAX_VALUE;
        List<ResourceAmount> chunks = new ArrayList<>();
        long remaining = mapped.amount();
        while (remaining > 0) {
            long take = Math.min(remaining, limit);
            chunks.add(new ResourceAmount(mapped.key(), take));
            remaining -= take;
        }
        return chunks;
    }

    private Result<ItemStack> encodeStonecutting(UniversalPattern.Stonecutting s, Level level) {
        if (RecipeLookup.stonecutting(level, s.input(), s.output(), s.recipeId()).isEmpty()) {
            return Result.fail(ConversionText.recipeMissing("stonecutting"));
        }
        ItemStack stack = Rs2Patterns.newEncoded(PatternType.STONECUTTER);
        stack.set(DataComponents.INSTANCE.getStonecutterPatternState(), new StonecutterPatternState(
                ItemResource.ofItemStack(s.input()), ItemResource.ofItemStack(s.output())));
        return Result.ok(stack);
    }

    private Result<ItemStack> encodeSmithing(UniversalPattern.Smithing s, Level level) {
        if (RecipeLookup.smithing(level, s.template(), s.base(), s.addition(), s.recipeId()).isEmpty()) {
            return Result.fail(ConversionText.recipeMissing("smithing"));
        }
        ItemStack stack = Rs2Patterns.newEncoded(PatternType.SMITHING_TABLE);
        stack.set(DataComponents.INSTANCE.getSmithingTablePatternState(), new SmithingTablePatternState(
                ItemResource.ofItemStack(s.template()), ItemResource.ofItemStack(s.base()),
                ItemResource.ofItemStack(s.addition())));
        return Result.ok(stack);
    }
}
