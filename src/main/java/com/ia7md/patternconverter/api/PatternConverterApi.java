package com.ia7md.patternconverter.api;

import appeng.api.stacks.AEKey;
import com.refinedmods.refinedstorage.api.resource.ResourceKey;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

public final class PatternConverterApi {
    private static final List<PatternTranslator> TRANSLATORS = new CopyOnWriteArrayList<>();
    private static final Map<PatternFormat, PatternEncoder> ENCODERS =
            Collections.synchronizedMap(new EnumMap<>(PatternFormat.class));
    private static final List<KeyBridge<ResourceKey>> RS2_BRIDGES = new CopyOnWriteArrayList<>();
    private static final List<KeyBridge<AEKey>> AE2_BRIDGES = new CopyOnWriteArrayList<>();

    private PatternConverterApi() {
    }

    public static void registerTranslator(PatternTranslator translator) {
        TRANSLATORS.removeIf(t -> t.id().equals(translator.id()));
        TRANSLATORS.add(translator);
        List<PatternTranslator> sorted = new ArrayList<>(TRANSLATORS);
        sorted.sort(Comparator.comparingInt(PatternTranslator::priority).reversed());
        TRANSLATORS.clear();
        TRANSLATORS.addAll(sorted);
    }

    public static void registerEncoder(PatternEncoder encoder) {
        ENCODERS.put(encoder.targetFormat(), encoder);
    }

    public static void registerRs2KeyBridge(KeyBridge<ResourceKey> bridge) {
        RS2_BRIDGES.add(bridge);
    }

    public static void registerAe2KeyBridge(KeyBridge<AEKey> bridge) {
        AE2_BRIDGES.add(bridge);
    }

    public static List<PatternTranslator> translators() {
        return Collections.unmodifiableList(TRANSLATORS);
    }

    public static Optional<PatternTranslator> findTranslator(ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        for (PatternTranslator t : TRANSLATORS) {
            if (t.matches(stack)) {
                return Optional.of(t);
            }
        }
        return Optional.empty();
    }

    public static Optional<PatternEncoder> encoder(PatternFormat format) {
        return Optional.ofNullable(ENCODERS.get(format));
    }

    public static boolean isEncodedPattern(ItemStack stack) {
        return findTranslator(stack).isPresent();
    }

    public static Optional<PatternFormat> blankFormat(ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        for (PatternEncoder e : ENCODERS.values()) {
            if (e.isBlank(stack)) {
                return Optional.of(e.targetFormat());
            }
        }
        return Optional.empty();
    }

    public static boolean isBlankPattern(ItemStack stack) {
        return blankFormat(stack).isPresent();
    }

    public static Optional<UniversalKey> rs2ToUniversal(ResourceKey key) {
        for (KeyBridge<ResourceKey> b : RS2_BRIDGES) {
            Optional<UniversalKey> r = b.toUniversal(key);
            if (r.isPresent()) {
                return r;
            }
        }
        return Optional.empty();
    }

    public static Optional<ResourceKey> universalToRs2(UniversalKey key) {
        for (KeyBridge<ResourceKey> b : RS2_BRIDGES) {
            Optional<ResourceKey> r = b.fromUniversal(key);
            if (r.isPresent()) {
                return r;
            }
        }
        return Optional.empty();
    }

    public static Optional<UniversalKey> ae2ToUniversal(AEKey key) {
        for (KeyBridge<AEKey> b : AE2_BRIDGES) {
            Optional<UniversalKey> r = b.toUniversal(key);
            if (r.isPresent()) {
                return r;
            }
        }
        return Optional.empty();
    }

    public static Optional<AEKey> universalToAe2(UniversalKey key) {
        for (KeyBridge<AEKey> b : AE2_BRIDGES) {
            Optional<AEKey> r = b.fromUniversal(key);
            if (r.isPresent()) {
                return r;
            }
        }
        return Optional.empty();
    }
}
