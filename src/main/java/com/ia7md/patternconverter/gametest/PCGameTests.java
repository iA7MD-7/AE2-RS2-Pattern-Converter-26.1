package com.ia7md.patternconverter.gametest;

import com.ia7md.patternconverter.PatternConverter;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@EventBusSubscriber(modid = PatternConverter.MOD_ID)
public final class PCGameTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.ofEntries(
            Map.entry("ae2_crafting_to_rs2", ConversionGameTests::ae2CraftingToRs2),
            Map.entry("rs2_crafting_to_ae2", ConversionGameTests::rs2CraftingToAe2),
            Map.entry("ae2_processing_with_fluid_to_rs2", ConversionGameTests::ae2ProcessingWithFluidToRs2),
            Map.entry("rs2_large_processing_to_ae2_condenses", ConversionGameTests::rs2LargeProcessingToAe2Condenses),
            Map.entry("ae2_large_processing_to_rs2_splits", ConversionGameTests::ae2LargeProcessingToRs2Splits),
            Map.entry("rs2_too_many_outputs_for_ae2_is_refused", ConversionGameTests::rs2TooManyOutputsForAe2IsRefused),
            Map.entry("stonecutting_round_trip", ConversionGameTests::stonecuttingRoundTrip),
            Map.entry("smithing_round_trip", ConversionGameTests::smithingRoundTrip),
            Map.entry("locked_direction_refuses_other_kind", ConversionGameTests::lockedDirectionRefusesOtherKind),
            Map.entry("waits_for_blank_of_target_format", ConversionGameTests::waitsForBlankOfTargetFormat),
            Map.entry("batch_converts_whole_grid_and_pulses", ConversionGameTests::batchConvertsWholeGridAndPulses),
            Map.entry("refined_types_fe_bridges_to_applied_flux", ConversionGameTests::refinedTypesFeBridgesToAppliedFlux));

    private PCGameTests() {
    }

    private static boolean enabled() {
        String namespaces = System.getProperty("neoforge.enabledGameTestNamespaces", "");
        for (String ns : namespaces.split(",")) {
            if (ns.trim().equals(PatternConverter.MOD_ID)) {
                return true;
            }
        }
        return false;
    }

    @SubscribeEvent
    public static void registerFunctions(RegisterEvent event) {
        if (!enabled()) {
            return;
        }
        event.register(Registries.TEST_FUNCTION, registry ->
                TESTS.forEach((name, body) -> registry.register(PatternConverter.asResource(name), body)));
    }

    @SubscribeEvent
    public static void registerTests(RegisterGameTestsEvent event) {
        if (!enabled()) {
            return;
        }
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(
                PatternConverter.asResource("default"), new TestEnvironmentDefinition.AllOf(List.of()));
        TESTS.keySet().forEach(name -> {
            ResourceKey<Consumer<GameTestHelper>> function = ResourceKey.create(Registries.TEST_FUNCTION, PatternConverter.asResource(name));
            event.registerTest(PatternConverter.asResource(name), new FunctionGameTestInstance(function,
                    new TestData<>(environment, PatternConverter.asResource("empty"), 200, 0, true)));
        });
    }
}
