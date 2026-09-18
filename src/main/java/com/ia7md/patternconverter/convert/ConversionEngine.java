package com.ia7md.patternconverter.convert;

import com.ia7md.patternconverter.PatternConverter;
import com.ia7md.patternconverter.api.ConversionDirection;
import com.ia7md.patternconverter.api.ConversionText;
import com.ia7md.patternconverter.api.PatternConverterApi;
import com.ia7md.patternconverter.api.PatternEncoder;
import com.ia7md.patternconverter.api.PatternTranslator;
import com.ia7md.patternconverter.api.Result;
import com.ia7md.patternconverter.api.UniversalPattern;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public final class ConversionEngine {
    private ConversionEngine() {
    }

    public record Conversion(ItemStack converted, ItemStack emptiedOriginal, ConversionDirection direction,
                             List<Component> warnings) {
    }

    public static Result<Conversion> convert(ItemStack source, Level level, @Nullable ConversionDirection forced) {
        Optional<PatternTranslator> translatorOpt = PatternConverterApi.findTranslator(source);
        if (translatorOpt.isEmpty()) {
            return Result.fail(PatternConverterApi.isBlankPattern(source)
                    ? ConversionText.blankInInput() : ConversionText.notAPattern());
        }
        PatternTranslator translator = translatorOpt.get();
        ConversionDirection direction = ConversionDirection.from(translator.sourceFormat());
        if (forced != null && forced != direction) {
            return Result.fail(ConversionText.directionLocked(forced));
        }
        Optional<PatternEncoder> encoderOpt = PatternConverterApi.encoder(direction.target());
        if (encoderOpt.isEmpty()) {
            return Result.fail(ConversionText.noEncoder(direction.target()));
        }

        ItemStack single = source.copyWithCount(1);
        Result<UniversalPattern> read;
        try {
            read = translator.read(single, level);
        } catch (RuntimeException e) {
            PatternConverter.LOGGER.warn("Translator {} threw while reading {}", translator.id(), single, e);
            return Result.fail(ConversionText.undecodable());
        }
        if (!read.isOk()) {
            return read.castFailure();
        }

        Result<ItemStack> encoded;
        try {
            encoded = encoderOpt.get().encode(read.get(), level);
        } catch (RuntimeException e) {
            PatternConverter.LOGGER.warn("Encoder for {} threw while encoding {}", direction.target(), read.get(), e);
            return Result.fail(ConversionText.undecodable());
        }
        if (!encoded.isOk()) {
            return encoded.castFailure();
        }
        ItemStack blank = translator.blankOf(single);
        Result<ItemStack> merged = encoded.withWarnings(read.warnings());
        return Result.ok(new Conversion(merged.get(), blank, direction, merged.warnings()), merged.warnings());
    }

    public static Optional<ConversionDirection> directionOf(ItemStack stack) {
        return PatternConverterApi.findTranslator(stack).map(t -> ConversionDirection.from(t.sourceFormat()));
    }
}
