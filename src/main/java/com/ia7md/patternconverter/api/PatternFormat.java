package com.ia7md.patternconverter.api;

import net.minecraft.network.chat.Component;

public enum PatternFormat {
    REFINED_STORAGE("rs2", "Refined Storage"),
    APPLIED_ENERGISTICS("ae2", "Applied Energistics");

    private final String id;
    private final String englishName;

    PatternFormat(String id, String englishName) {
        this.id = id;
        this.englishName = englishName;
    }

    public String id() {
        return id;
    }

    public String englishName() {
        return englishName;
    }

    public PatternFormat opposite() {
        return this == REFINED_STORAGE ? APPLIED_ENERGISTICS : REFINED_STORAGE;
    }

    public Component shortName() {
        return Component.translatable("format.pattern_converter." + id + ".short");
    }

    public Component displayName() {
        return Component.translatable("format.pattern_converter." + id);
    }
}
