package com.ia7md.patternconverter.compat.advancedae;

import appeng.api.crafting.IPatternDetails;
import com.ia7md.patternconverter.PCConfig;
import com.ia7md.patternconverter.PatternConverter;
import com.ia7md.patternconverter.api.ConversionText;
import com.ia7md.patternconverter.api.PatternFormat;
import com.ia7md.patternconverter.api.PatternTranslator;
import com.ia7md.patternconverter.api.Result;
import com.ia7md.patternconverter.api.UniversalPattern;
import com.ia7md.patternconverter.convert.ae2.Ae2Patterns;
import net.pedroksl.advanced_ae.common.definitions.AAEItems;
import net.pedroksl.advanced_ae.common.patterns.AdvProcessingPattern;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

final class AdvancedAeTranslator implements PatternTranslator {
    private static final Identifier ID = PatternConverter.asResource("advanced_ae");

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
        return ADDON_PRIORITY;
    }

    @Override
    public boolean matches(ItemStack stack) {
        return AAEItems.ADV_PROCESSING_PATTERN.is(stack);
    }

    @Override
    public ItemStack blankOf(ItemStack encoded) {
        return Ae2Patterns.blank();
    }

    @Override
    public Result<UniversalPattern> read(ItemStack stack, Level level) {
        if (!PCConfig.ADDON_ADVANCED_AE.get()) {
            return Result.fail(ConversionText.translatorDisabled("AdvancedAE"));
        }
        IPatternDetails details = Ae2Patterns.decode(stack, level);
        if (details == null) {
            return Result.fail(ConversionText.undecodable());
        }
        List<Component> warnings = new ArrayList<>();
        if (details instanceof AdvProcessingPattern adv) {
            if (adv.directionalInputsSet()) {
                warnings.add(ConversionText.directionsDropped());
            }
            return Ae2Patterns.readSparse(adv.getSparseInputs(), adv.getSparseOutputs(), warnings);
        }
        warnings.add(ConversionText.genericFallback(stack.getHoverName()));
        return Ae2Patterns.readAsProcessing(details, warnings);
    }
}
