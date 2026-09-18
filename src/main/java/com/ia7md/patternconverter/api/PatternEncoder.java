package com.ia7md.patternconverter.api;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public interface PatternEncoder {
    PatternFormat targetFormat();

    boolean isBlank(ItemStack stack);

    ItemStack blankStack();

    Result<ItemStack> encode(UniversalPattern pattern, Level level);
}
