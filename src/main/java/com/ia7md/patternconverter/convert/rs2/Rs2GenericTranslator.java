package com.ia7md.patternconverter.convert.rs2;

import com.ia7md.patternconverter.PCConfig;
import com.ia7md.patternconverter.PatternConverter;
import com.ia7md.patternconverter.api.ConversionText;
import com.ia7md.patternconverter.api.PatternConverterApi;
import com.ia7md.patternconverter.api.PatternFormat;
import com.ia7md.patternconverter.api.PatternTranslator;
import com.ia7md.patternconverter.api.Result;
import com.ia7md.patternconverter.api.UniversalKey;
import com.ia7md.patternconverter.api.UniversalPattern;
import com.ia7md.patternconverter.api.UniversalStack;
import com.refinedmods.refinedstorage.api.autocrafting.Ingredient;
import com.refinedmods.refinedstorage.api.autocrafting.Pattern;
import com.refinedmods.refinedstorage.api.autocrafting.PatternLayout;
import com.refinedmods.refinedstorage.api.resource.ResourceAmount;
import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import com.refinedmods.refinedstorage.common.api.autocrafting.PatternProviderItem;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class Rs2GenericTranslator implements PatternTranslator {
    private static final Identifier ID = PatternConverter.asResource("rs2_generic");

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public PatternFormat sourceFormat() {
        return PatternFormat.REFINED_STORAGE;
    }

    @Override
    public int priority() {
        return FALLBACK_PRIORITY;
    }

    @Override
    public boolean matches(ItemStack stack) {
        return stack.getItem() instanceof PatternProviderItem && !Rs2Patterns.isRs2PatternItem(stack);
    }

    @Override
    public ItemStack blankOf(ItemStack encoded) {
        return Rs2Patterns.blank();
    }

    @Override
    public Result<UniversalPattern> read(ItemStack stack, Level level) {
        if (!PCConfig.GENERIC_FALLBACK.get()) {
            return Result.fail(ConversionText.fallbackDisabled());
        }
        Optional<Pattern> pattern = RefinedStorageApi.INSTANCE.getPattern(stack, level);
        if (pattern.isEmpty()) {
            return Result.fail(ConversionText.undecodable());
        }
        PatternLayout layout = pattern.get().layout();
        List<UniversalPattern.Processing.Input> inputs = new ArrayList<>();
        for (Ingredient ingredient : layout.ingredients()) {
            if (ingredient.inputs().isEmpty()) {
                continue;
            }
            ResourceKey primary = ingredient.inputs().get(0);
            Optional<UniversalKey> key = PatternConverterApi.rs2ToUniversal(primary);
            if (key.isEmpty()) {
                return Result.fail(Rs2Translator.unknown(primary));
            }
            List<UniversalKey> alternatives = new ArrayList<>();
            for (int i = 1; i < ingredient.inputs().size(); i++) {
                PatternConverterApi.rs2ToUniversal(ingredient.inputs().get(i)).ifPresent(alternatives::add);
            }
            inputs.add(new UniversalPattern.Processing.Input(
                    new UniversalStack(key.get(), ingredient.amount()), alternatives));
        }
        List<UniversalStack> outputs = new ArrayList<>();
        for (ResourceAmount output : layout.outputs()) {
            Optional<UniversalKey> key = PatternConverterApi.rs2ToUniversal(output.resource());
            if (key.isEmpty()) {
                return Result.fail(Rs2Translator.unknown(output.resource()));
            }
            outputs.add(new UniversalStack(key.get(), output.amount()));
        }
        if (inputs.isEmpty() || outputs.isEmpty()) {
            return Result.fail(ConversionText.emptyPattern());
        }
        return Result.ok(new UniversalPattern.Processing(inputs, outputs),
                List.of(ConversionText.genericFallback(stack.getHoverName())));
    }
}
