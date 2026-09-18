package com.ia7md.patternconverter.api;

import net.minecraft.network.chat.Component;

public enum ConversionDirection {
    RS_TO_AE(PatternFormat.REFINED_STORAGE, PatternFormat.APPLIED_ENERGISTICS),
    AE_TO_RS(PatternFormat.APPLIED_ENERGISTICS, PatternFormat.REFINED_STORAGE);

    private final PatternFormat source;
    private final PatternFormat target;

    ConversionDirection(PatternFormat source, PatternFormat target) {
        this.source = source;
        this.target = target;
    }

    public PatternFormat source() {
        return source;
    }

    public PatternFormat target() {
        return target;
    }

    public static ConversionDirection from(PatternFormat source) {
        return source == PatternFormat.REFINED_STORAGE ? RS_TO_AE : AE_TO_RS;
    }

    public static ConversionDirection towards(PatternFormat target) {
        return target == PatternFormat.APPLIED_ENERGISTICS ? RS_TO_AE : AE_TO_RS;
    }

    public Component displayName() {
        return Component.translatable("direction.pattern_converter." + name().toLowerCase());
    }
}
