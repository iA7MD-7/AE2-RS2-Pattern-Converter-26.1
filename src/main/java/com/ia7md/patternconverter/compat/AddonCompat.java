package com.ia7md.patternconverter.compat;

import com.ia7md.patternconverter.PatternConverter;
import com.ia7md.patternconverter.compat.advancedae.AdvancedAeCompat;
import com.ia7md.patternconverter.compat.appflux.AppliedFluxCompat;
import com.ia7md.patternconverter.compat.refinedtypes.RefinedTypesCompat;

import net.neoforged.fml.ModList;

import java.util.ArrayList;
import java.util.List;

public final class AddonCompat {
    public static final String ADVANCED_AE = "advanced_ae";
    public static final String EXPANDED_AE = "expandedae";
    public static final String EXTENDED_AE = "extendedae";
    public static final String REFINED_TYPES = "refinedtypes";
    public static final String APPLIED_FLUX = "appflux";
    public static final String ARS_ENERGISTIQUE = "arseng";

    private static final List<String> ACTIVE = new ArrayList<>();

    private AddonCompat() {
    }

    public static void register() {
        ModList mods = ModList.get();
        if (mods.isLoaded(ADVANCED_AE)) {
            guarded("AdvancedAE", AdvancedAeCompat::register);
        }
        if (mods.isLoaded(REFINED_TYPES)) {
            guarded("Refined Types", RefinedTypesCompat::register);
        }
        if (mods.isLoaded(APPLIED_FLUX)) {
            guarded("Applied Flux", AppliedFluxCompat::register);
        }
        if (mods.isLoaded(EXPANDED_AE)) {
            ACTIVE.add("Expanded AE (stock AE2 patterns, core translator)");
        }
        if (mods.isLoaded(EXTENDED_AE)) {
            ACTIVE.add("ExtendedAE (stock AE2 patterns, core translator)");
        }
        PatternConverter.LOGGER.info("Addon compat active: {}", ACTIVE.isEmpty() ? "none" : String.join(", ", ACTIVE));
    }

    public static List<String> active() {
        return List.copyOf(ACTIVE);
    }

    private static void guarded(String name, Runnable registration) {
        try {
            registration.run();
            ACTIVE.add(name);
        } catch (Throwable t) {
            PatternConverter.LOGGER.error("{} compat failed to initialise; that addon's patterns will not convert", name, t);
        }
    }
}
