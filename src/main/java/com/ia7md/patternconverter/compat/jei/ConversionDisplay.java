package com.ia7md.patternconverter.compat.jei;

import com.ia7md.patternconverter.api.ConversionDirection;
import com.ia7md.patternconverter.api.PatternFormat;

import net.minecraft.world.item.ItemStack;

import java.util.List;

public record ConversionDisplay(ConversionDirection direction, List<ItemStack> sourcePatterns,
                                ItemStack targetBlank, List<ItemStack> targetPatterns, ItemStack sourceBlank) {
    public PatternFormat source() {
        return direction.source();
    }

    public PatternFormat target() {
        return direction.target();
    }
}
