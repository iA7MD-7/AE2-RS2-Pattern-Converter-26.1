package com.ia7md.patternconverter.convert.ae2;

import appeng.api.crafting.IPatternDetails;
import appeng.api.ids.AEComponents;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.core.definitions.AEItems;
import appeng.crafting.pattern.AECraftingPattern;
import appeng.crafting.pattern.AEProcessingPattern;
import appeng.crafting.pattern.AESmithingTablePattern;
import appeng.crafting.pattern.AEStonecuttingPattern;
import appeng.crafting.pattern.EncodedCraftingPattern;
import com.ia7md.patternconverter.PatternConverter;
import com.ia7md.patternconverter.api.ConversionText;
import com.ia7md.patternconverter.api.PatternFormat;
import com.ia7md.patternconverter.api.PatternTranslator;
import com.ia7md.patternconverter.api.Result;
import com.ia7md.patternconverter.api.UniversalPattern;

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

final class Ae2Translator implements PatternTranslator {
    private static final Identifier ID = PatternConverter.asResource("ae2");

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public PatternFormat sourceFormat() {
        return PatternFormat.APPLIED_ENERGISTICS;
    }

    @Override
    public boolean matches(ItemStack stack) {
        return AEItems.CRAFTING_PATTERN.is(stack)
                || AEItems.PROCESSING_PATTERN.is(stack)
                || AEItems.STONECUTTING_PATTERN.is(stack)
                || AEItems.SMITHING_TABLE_PATTERN.is(stack);
    }

    @Override
    public ItemStack blankOf(ItemStack encoded) {
        return Ae2Patterns.blank();
    }

    @Override
    public Result<UniversalPattern> read(ItemStack stack, Level level) {
        IPatternDetails details = Ae2Patterns.decode(stack, level);
        if (details == null) {
            return Result.fail(ConversionText.undecodable());
        }
        if (details instanceof AECraftingPattern crafting) {
            return readCrafting(stack, crafting);
        }
        if (details instanceof AEStonecuttingPattern stonecutting) {
            GenericStack output = stonecutting.getPrimaryOutput();
            if (!(output.what() instanceof AEItemKey outKey)) {
                return Result.fail(ConversionText.undecodable());
            }
            return Result.ok(new UniversalPattern.Stonecutting(stonecutting.getInput().toStack(),
                    outKey.toStack((int) output.amount()), stonecutting.getRecipeId(), stonecutting.canSubstitute));
        }
        if (details instanceof AESmithingTablePattern smithing) {
            GenericStack output = smithing.getPrimaryOutput();
            if (!(output.what() instanceof AEItemKey outKey)) {
                return Result.fail(ConversionText.undecodable());
            }
            return Result.ok(new UniversalPattern.Smithing(smithing.getTemplate().toStack(),
                    smithing.getBase().toStack(), smithing.getAddition().toStack(),
                    outKey.toStack((int) output.amount()), smithing.getRecipeId(), smithing.canSubstitute));
        }
        if (details instanceof AEProcessingPattern processing) {
            return Ae2Patterns.readSparse(processing.getSparseInputs(), processing.getSparseOutputs(), List.of());
        }
        return Ae2Patterns.readAsProcessing(details, List.of(ConversionText.genericFallback(stack.getHoverName())));
    }

    private Result<UniversalPattern> readCrafting(ItemStack stack, AECraftingPattern crafting) {
        List<GenericStack> sparse = crafting.getSparseInputs();
        List<ItemStack> grid = new ArrayList<>(9);
        for (int i = 0; i < 9; i++) {
            GenericStack cell = i < sparse.size() ? sparse.get(i) : null;
            if (cell == null) {
                grid.add(ItemStack.EMPTY);
            } else if (cell.what() instanceof AEItemKey itemKey) {
                grid.add(itemKey.toStack(1));
            } else {
                return Result.fail(ConversionText.undecodable());
            }
        }
        GenericStack output = crafting.getPrimaryOutput();
        if (!(output.what() instanceof AEItemKey outKey)) {
            return Result.fail(ConversionText.undecodable());
        }
        EncodedCraftingPattern encoded = stack.get(AEComponents.ENCODED_CRAFTING_PATTERN);
        ResourceKey<Recipe<?>> recipeId = encoded != null ? encoded.recipeId() : null;
        return Result.ok(new UniversalPattern.Crafting(grid, outKey.toStack((int) output.amount()), recipeId,
                crafting.canSubstitute, crafting.canSubstituteFluids));
    }
}
