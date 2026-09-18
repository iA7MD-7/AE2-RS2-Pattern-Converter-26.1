package com.ia7md.patternconverter.convert.ae2;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.core.definitions.AEItems;
import com.ia7md.patternconverter.PatternConverter;
import com.ia7md.patternconverter.api.ConversionText;
import com.ia7md.patternconverter.api.PatternConverterApi;
import com.ia7md.patternconverter.api.PatternFormat;
import com.ia7md.patternconverter.api.Result;
import com.ia7md.patternconverter.api.UniversalKey;
import com.ia7md.patternconverter.api.UniversalPattern;
import com.ia7md.patternconverter.api.UniversalStack;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class Ae2Patterns {
    private Ae2Patterns() {
    }

    public static void register() {
        PatternConverterApi.registerAe2KeyBridge(new Ae2CoreBridge());
        PatternConverterApi.registerTranslator(new Ae2Translator());
        PatternConverterApi.registerTranslator(new Ae2GenericTranslator());
        PatternConverterApi.registerEncoder(new Ae2Encoder());
    }

    public static boolean isBlank(ItemStack stack) {
        return AEItems.BLANK_PATTERN.is(stack);
    }

    public static ItemStack blank() {
        return AEItems.BLANK_PATTERN.stack();
    }

    public static ItemStack exampleProcessingPattern(ItemStack input, ItemStack output) {
        return PatternDetailsHelper.encodeProcessingPattern(
                List.of(GenericStack.fromItemStack(input)), List.of(GenericStack.fromItemStack(output)));
    }

    @Nullable
    public static IPatternDetails decode(ItemStack stack, Level level) {
        try {
            return PatternDetailsHelper.decodePattern(stack, level);
        } catch (RuntimeException e) {
            PatternConverter.LOGGER.debug("AE2 refused to decode {}", stack, e);
            return null;
        }
    }

    public static Result<UniversalPattern> readAsProcessing(IPatternDetails details, List<Component> warnings) {
        List<UniversalPattern.Processing.Input> inputs = new ArrayList<>();
        for (IPatternDetails.IInput input : details.getInputs()) {
            GenericStack[] possible = input.getPossibleInputs();
            if (possible.length == 0) {
                continue;
            }
            GenericStack primary = possible[0];
            Optional<UniversalKey> key = PatternConverterApi.ae2ToUniversal(primary.what());
            if (key.isEmpty()) {
                return Result.fail(unknown(primary.what()));
            }
            List<UniversalKey> alternatives = new ArrayList<>();
            for (int i = 1; i < possible.length; i++) {
                PatternConverterApi.ae2ToUniversal(possible[i].what()).ifPresent(alternatives::add);
            }
            long amount = Math.max(1, primary.amount()) * Math.max(1, input.getMultiplier());
            inputs.add(new UniversalPattern.Processing.Input(new UniversalStack(key.get(), amount), alternatives));
        }
        List<UniversalStack> outputs = new ArrayList<>();
        for (GenericStack output : details.getOutputs()) {
            if (output == null) {
                continue;
            }
            Optional<UniversalKey> key = PatternConverterApi.ae2ToUniversal(output.what());
            if (key.isEmpty()) {
                return Result.fail(unknown(output.what()));
            }
            outputs.add(new UniversalStack(key.get(), output.amount()));
        }
        if (inputs.isEmpty() || outputs.isEmpty()) {
            return Result.fail(ConversionText.emptyPattern());
        }
        return Result.ok(new UniversalPattern.Processing(inputs, outputs), warnings);
    }

    public static Result<UniversalPattern> readSparse(List<GenericStack> sparseInputs, List<GenericStack> sparseOutputs,
                                                      List<Component> warnings) {
        List<UniversalPattern.Processing.Input> inputs = new ArrayList<>();
        for (GenericStack input : sparseInputs) {
            if (input == null) {
                continue;
            }
            Optional<UniversalKey> key = PatternConverterApi.ae2ToUniversal(input.what());
            if (key.isEmpty()) {
                return Result.fail(unknown(input.what()));
            }
            inputs.add(UniversalPattern.Processing.Input.of(new UniversalStack(key.get(), input.amount())));
        }
        List<UniversalStack> outputs = new ArrayList<>();
        for (GenericStack output : sparseOutputs) {
            if (output == null) {
                continue;
            }
            Optional<UniversalKey> key = PatternConverterApi.ae2ToUniversal(output.what());
            if (key.isEmpty()) {
                return Result.fail(unknown(output.what()));
            }
            outputs.add(new UniversalStack(key.get(), output.amount()));
        }
        if (inputs.isEmpty() || outputs.isEmpty()) {
            return Result.fail(ConversionText.emptyPattern());
        }
        return Result.ok(new UniversalPattern.Processing(inputs, outputs), warnings);
    }

    static Component unknown(AEKey key) {
        return ConversionText.unknownResource(key.getType().getId().toString(), PatternFormat.APPLIED_ENERGISTICS);
    }
}
