package com.ia7md.patternconverter.api;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public interface PatternTranslator {
    Identifier id();

    PatternFormat sourceFormat();

    default int priority() {
        return CORE_PRIORITY;
    }

    int CORE_PRIORITY = 0;
    int ADDON_PRIORITY = 100;
    int FALLBACK_PRIORITY = -100;

    boolean matches(ItemStack stack);

    Result<UniversalPattern> read(ItemStack stack, Level level);

    ItemStack blankOf(ItemStack encoded);
}
