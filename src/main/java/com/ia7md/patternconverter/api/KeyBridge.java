package com.ia7md.patternconverter.api;

import java.util.Optional;

public interface KeyBridge<K> {
    Optional<UniversalKey> toUniversal(K key);

    Optional<K> fromUniversal(UniversalKey key);
}
