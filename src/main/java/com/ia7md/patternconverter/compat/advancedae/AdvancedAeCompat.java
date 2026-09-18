package com.ia7md.patternconverter.compat.advancedae;

import com.ia7md.patternconverter.api.PatternConverterApi;

public final class AdvancedAeCompat {
    private AdvancedAeCompat() {
    }

    public static void register() {
        PatternConverterApi.registerTranslator(new AdvancedAeTranslator());
    }
}
