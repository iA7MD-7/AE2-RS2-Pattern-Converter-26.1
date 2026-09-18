package com.ia7md.patternconverter.convert;

import com.ia7md.patternconverter.PCConfig;
import com.ia7md.patternconverter.api.ConversionText;
import com.ia7md.patternconverter.api.PatternFormat;
import com.ia7md.patternconverter.api.Result;
import com.ia7md.patternconverter.api.UniversalKey;
import com.ia7md.patternconverter.api.UniversalPattern;
import com.ia7md.patternconverter.api.UniversalStack;
import com.ia7md.patternconverter.api.UnmappableResourceMode;

import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public final class KeyMapping {
    private KeyMapping() {
    }

    public record Mapped<K>(K key, long amount, UniversalKey universal) {
    }

    public record MappedInput<K>(Mapped<K> primary, List<K> alternatives, boolean droppedAlternatives) {
    }

    public static <K> Result<List<MappedInput<K>>> mapInputs(List<UniversalPattern.Processing.Input> inputs,
                                                             PatternFormat target,
                                                             Function<UniversalKey, Optional<K>> mapper) {
        UnmappableResourceMode mode = PCConfig.UNMAPPABLE_RESOURCES.get();
        List<MappedInput<K>> out = new ArrayList<>(inputs.size());
        for (UniversalPattern.Processing.Input input : inputs) {
            UniversalKey key = input.primary().key();
            if (mode == UnmappableResourceMode.ALWAYS_REJECT && !key.isStandard()) {
                return Result.fail(ConversionText.customRejected(key));
            }
            Optional<K> mapped = mapper.apply(key);
            if (mapped.isEmpty()) {
                return Result.fail(ConversionText.unmappableInput(key, target));
            }
            List<K> alternatives = new ArrayList<>();
            boolean dropped = false;
            for (UniversalKey alt : input.alternatives()) {
                Optional<K> m = mapper.apply(alt);
                if (m.isPresent()) {
                    alternatives.add(m.get());
                } else {
                    dropped = true;
                }
            }
            out.add(new MappedInput<>(new Mapped<>(mapped.get(), input.primary().amount(), key), alternatives, dropped));
        }
        return Result.ok(out);
    }

    public static <K> Result<List<Mapped<K>>> mapOutputs(List<UniversalStack> outputs, PatternFormat target,
                                                         Function<UniversalKey, Optional<K>> mapper) {
        UnmappableResourceMode mode = PCConfig.UNMAPPABLE_RESOURCES.get();
        List<Mapped<K>> out = new ArrayList<>(outputs.size());
        List<Component> warnings = new ArrayList<>();
        for (UniversalStack stack : outputs) {
            UniversalKey key = stack.key();
            if (mode == UnmappableResourceMode.ALWAYS_REJECT && !key.isStandard()) {
                return Result.fail(ConversionText.customRejected(key));
            }
            Optional<K> mapped = mapper.apply(key);
            if (mapped.isPresent()) {
                out.add(new Mapped<>(mapped.get(), stack.amount(), key));
            } else if (mode == UnmappableResourceMode.DROP_UNMAPPABLE_OUTPUTS) {
                warnings.add(ConversionText.outputDropped(key, target));
            } else {
                return Result.fail(ConversionText.unmappableOutput(key, target));
            }
        }
        if (out.isEmpty()) {
            return Result.fail(ConversionText.noOutputsLeft());
        }
        return Result.ok(out, warnings);
    }

    public static <K> List<Mapped<K>> condense(List<Mapped<K>> stacks, List<Component> warnings) {
        List<Mapped<K>> out = new ArrayList<>();
        for (Mapped<K> s : stacks) {
            boolean merged = false;
            for (int i = 0; i < out.size(); i++) {
                Mapped<K> existing = out.get(i);
                if (existing.key().equals(s.key())) {
                    out.set(i, new Mapped<>(existing.key(), existing.amount() + s.amount(), existing.universal()));
                    warnings.add(ConversionText.condensed(s.universal()));
                    merged = true;
                    break;
                }
            }
            if (!merged) {
                out.add(s);
            }
        }
        return out;
    }
}
