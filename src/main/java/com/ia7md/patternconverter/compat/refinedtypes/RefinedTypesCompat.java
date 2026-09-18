package com.ia7md.patternconverter.compat.refinedtypes;

import com.ia7md.patternconverter.api.PatternConverterApi;

public final class RefinedTypesCompat {
    private RefinedTypesCompat() {
    }

    public static void register() {
        PatternConverterApi.registerRs2KeyBridge(new RefinedTypesBridge());
    }
}
