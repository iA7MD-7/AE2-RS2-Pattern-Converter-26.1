package com.ia7md.patternconverter;

import com.ia7md.patternconverter.api.UnmappableResourceMode;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class PCConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.IntValue CYCLE_TICKS;
    public static final ModConfigSpec.IntValue PATTERNS_PER_CYCLE;
    public static final ModConfigSpec.IntValue REDSTONE_PULSE_TICKS;

    public static final ModConfigSpec.EnumValue<UnmappableResourceMode> UNMAPPABLE_RESOURCES;
    public static final ModConfigSpec.BooleanValue DROP_ALTERNATIVE_INPUTS;
    public static final ModConfigSpec.BooleanValue DROP_FLUID_SUBSTITUTION;
    public static final ModConfigSpec.BooleanValue GENERIC_FALLBACK;

    public static final ModConfigSpec.BooleanValue ADDON_ADVANCED_AE;
    public static final ModConfigSpec.BooleanValue ADDON_REFINED_TYPES;
    public static final ModConfigSpec.BooleanValue ADDON_APPLIED_FLUX;
    public static final ModConfigSpec.BooleanValue ADDON_ARS_ENERGISTIQUE;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("Machine throughput.").push("machine");
        CYCLE_TICKS = builder
                .comment("Ticks between work cycles. Each cycle converts up to patternsPerCycle patterns.")
                .defineInRange("cycleTicks", 10, 1, 1200);
        PATTERNS_PER_CYCLE = builder
                .comment("Patterns converted per work cycle (9 = the whole input grid at once).")
                .defineInRange("patternsPerCycle", 9, 1, 9);
        REDSTONE_PULSE_TICKS = builder
                .comment("Length in ticks of the redstone pulse emitted when a cycle converts at least one pattern.")
                .defineInRange("redstonePulseTicks", 10, 1, 200);
        builder.pop();

        builder.comment("Conversion rules.").push("conversion");
        UNMAPPABLE_RESOURCES = builder
                .comment(
                        "What to do with a pattern that uses a resource type the destination mod cannot hold",
                        "(Refined Types FE / Ars Nouveau Source / Industrial Foregoing Souls on the RS2 side,",
                        "Applied Flux FE / Ars Energistique Source / other AEKey types on the AE2 side).",
                        "  BRIDGE_IF_AVAILABLE      - convert when a matching key type exists on the other side",
                        "                             (e.g. Refined Types FE -> Applied Flux FE); otherwise reject with a reason.",
                        "  ALWAYS_REJECT            - refuse any pattern that contains a non item/fluid resource, even if a bridge exists.",
                        "  DROP_UNMAPPABLE_OUTPUTS  - like BRIDGE_IF_AVAILABLE, but an unmappable *output* is dropped with a warning",
                        "                             instead of rejecting. Inputs are never dropped (that would create free crafts).")
                .defineEnum("unmappableResources", UnmappableResourceMode.BRIDGE_IF_AVAILABLE);
        DROP_ALTERNATIVE_INPUTS = builder
                .comment("RS2 processing patterns can list alternative (tag) inputs; AE2 processing patterns cannot.",
                        "true = keep the primary input and warn, false = reject such patterns.")
                .define("dropAlternativeInputs", true);
        DROP_FLUID_SUBSTITUTION = builder
                .comment("AE2 crafting patterns can allow fluid substitution; RS2 has no equivalent flag.",
                        "true = convert and warn, false = reject such patterns.")
                .define("dropFluidSubstitution", true);
        GENERIC_FALLBACK = builder
                .comment("When an addon pattern item has no dedicated translator, read it through the public",
                        "pattern API (AE2 IPatternDetails / RS2 PatternProviderItem) and convert it as a processing pattern.")
                .define("genericApiFallback", true);
        builder.pop();

        builder.comment("Per-addon toggles. A disabled translator makes its patterns fail with a clear reason;",
                "a disabled bridge makes that resource type count as unmappable.").push("addons");
        ADDON_ADVANCED_AE = builder
                .comment("AdvancedAE: translate Advanced Processing Patterns (directional inputs are dropped with a warning).")
                .define("advancedAe", true);
        ADDON_REFINED_TYPES = builder
                .comment("Refined Types: recognise its FE / Source / Souls resources in RS2 patterns.")
                .define("refinedTypes", true);
        ADDON_APPLIED_FLUX = builder
                .comment("Applied Flux: map FE to/from its AE2 FE key (pairs with Refined Types FE).")
                .define("appliedFlux", true);
        ADDON_ARS_ENERGISTIQUE = builder
                .comment("Ars Energistique: map Ars Nouveau Source to/from its AE2 Source key (pairs with Refined Types Source).")
                .define("arsEnergistique", true);
        builder.pop();

        SPEC = builder.build();
    }

    private PCConfig() {
    }
}
