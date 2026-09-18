package com.ia7md.patternconverter.api;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class ConversionText {
    private static final String FAIL = "message.pattern_converter.fail.";
    private static final String WARN = "message.pattern_converter.warn.";
    private static final String STATUS = "message.pattern_converter.status.";

    private ConversionText() {
    }

    public static Component notAPattern() {
        return fail("not_a_pattern");
    }

    public static Component blankInInput() {
        return fail("blank_in_input");
    }

    public static Component undecodable() {
        return fail("undecodable");
    }

    public static Component directionLocked(ConversionDirection locked) {
        return fail("direction_locked", locked.displayName());
    }

    public static Component translatorDisabled(String addonName) {
        return fail("translator_disabled", addonName);
    }

    public static Component noEncoder(PatternFormat format) {
        return fail("no_encoder", format.displayName());
    }

    public static Component unmappableInput(UniversalKey key, PatternFormat target) {
        return fail("unmappable_input", key.displayName(), target.displayName());
    }

    public static Component unmappableOutput(UniversalKey key, PatternFormat target) {
        return fail("unmappable_output", key.displayName(), target.displayName());
    }

    public static Component customRejected(UniversalKey key) {
        return fail("custom_rejected", key.displayName());
    }

    public static Component noOutputsLeft() {
        return fail("no_outputs_left");
    }

    public static Component tooManyInputs(PatternFormat target, int needed, int max) {
        return fail("too_many_inputs", target.displayName(), needed, max);
    }

    public static Component tooManyOutputs(PatternFormat target, int needed, int max) {
        return fail("too_many_outputs", target.displayName(), needed, max);
    }

    public static Component recipeMissing(String shapeKey) {
        return fail("recipe_missing", Component.translatable("shape.pattern_converter." + shapeKey));
    }

    public static Component alternativesRejected() {
        return fail("alternatives_rejected");
    }

    public static Component fluidSubstitutionRejected() {
        return fail("fluid_substitution_rejected");
    }

    public static Component emptyPattern() {
        return fail("empty_pattern");
    }

    public static Component fallbackDisabled() {
        return fail("fallback_disabled");
    }

    public static Component unknownResource(String typeName, PatternFormat side) {
        return fail("unknown_resource", typeName, side.displayName());
    }

    public static Component alternativesDropped(UniversalKey key) {
        return warn("alternatives_dropped", key.displayName());
    }

    public static Component fluidSubstitutionDropped() {
        return warn("fluid_substitution_dropped");
    }

    public static Component genericFallback(Component itemName) {
        return warn("generic_fallback", itemName);
    }

    public static Component directionsDropped() {
        return warn("directions_dropped");
    }

    public static Component outputDropped(UniversalKey key, PatternFormat target) {
        return warn("output_dropped", key.displayName(), target.displayName());
    }

    public static Component amountSplit(UniversalKey key, int slots) {
        return warn("amount_split", key.displayName(), slots);
    }

    public static Component condensed(UniversalKey key) {
        return warn("condensed", key.displayName());
    }

    public static Component idle() {
        return Component.translatable(STATUS + "idle").withStyle(ChatFormatting.GRAY);
    }

    public static Component waitingForBlank(PatternFormat target) {
        return Component.translatable(STATUS + "waiting_blank", target.shortName()).withStyle(ChatFormatting.YELLOW);
    }

    public static Component outputFull() {
        return Component.translatable(STATUS + "output_full").withStyle(ChatFormatting.YELLOW);
    }

    public static Component returnFull(PatternFormat source) {
        return Component.translatable(STATUS + "return_full", source.shortName()).withStyle(ChatFormatting.YELLOW);
    }

    public static Component queued() {
        return Component.translatable(STATUS + "queued").withStyle(ChatFormatting.GRAY);
    }

    public static Component converted(ConversionDirection direction, int count) {
        return Component.translatable(STATUS + "converted", count, direction.displayName()).withStyle(ChatFormatting.GREEN);
    }

    private static MutableComponent fail(String key, Object... args) {
        return Component.translatable(FAIL + key, args).withStyle(ChatFormatting.RED);
    }

    private static MutableComponent warn(String key, Object... args) {
        return Component.translatable(WARN + key, args).withStyle(ChatFormatting.GOLD);
    }
}
