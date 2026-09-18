package com.ia7md.patternconverter.convert.ae2;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import com.ia7md.patternconverter.PCConfig;
import com.ia7md.patternconverter.PatternConverter;
import com.ia7md.patternconverter.api.ConversionText;
import com.ia7md.patternconverter.api.PatternFormat;
import com.ia7md.patternconverter.api.PatternTranslator;
import com.ia7md.patternconverter.api.Result;
import com.ia7md.patternconverter.api.UniversalPattern;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

final class Ae2GenericTranslator implements PatternTranslator {
    private static final Identifier ID = PatternConverter.asResource("ae2_generic");

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public PatternFormat sourceFormat() {
        return PatternFormat.APPLIED_ENERGISTICS;
    }

    @Override
    public int priority() {
        return FALLBACK_PRIORITY;
    }

    @Override
    public boolean matches(ItemStack stack) {
        return PatternDetailsHelper.isEncodedPattern(stack);
    }

    @Override
    public ItemStack blankOf(ItemStack encoded) {
        return Ae2Patterns.blank();
    }

    @Override
    public Result<UniversalPattern> read(ItemStack stack, Level level) {
        if (!PCConfig.GENERIC_FALLBACK.get()) {
            return Result.fail(ConversionText.fallbackDisabled());
        }
        IPatternDetails details = Ae2Patterns.decode(stack, level);
        if (details == null) {
            return Result.fail(ConversionText.undecodable());
        }
        return Ae2Patterns.readAsProcessing(details, List.of(ConversionText.genericFallback(stack.getHoverName())));
    }
}
