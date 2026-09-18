package com.ia7md.patternconverter.api;

import net.minecraft.resources.Identifier;

public final class CustomKeyTypes {
    private static final String NAMESPACE = "pattern_converter";

    public static final Identifier FORGE_ENERGY = id("forge_energy");
    public static final Identifier ARS_SOURCE = id("ars_source");
    public static final Identifier IF_SOULS = id("if_souls");

    public static UniversalKey.Custom forgeEnergy() {
        return new UniversalKey.Custom(FORGE_ENERGY);
    }

    public static UniversalKey.Custom arsSource() {
        return new UniversalKey.Custom(ARS_SOURCE);
    }

    public static UniversalKey.Custom ifSouls() {
        return new UniversalKey.Custom(IF_SOULS);
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(NAMESPACE, path);
    }

    private CustomKeyTypes() {
    }
}
