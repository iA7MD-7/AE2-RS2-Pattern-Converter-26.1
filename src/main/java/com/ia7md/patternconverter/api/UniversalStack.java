package com.ia7md.patternconverter.api;

public record UniversalStack(UniversalKey key, long amount) {
    public UniversalStack {
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be positive: " + amount);
        }
    }

    public UniversalStack withAmount(long newAmount) {
        return new UniversalStack(key, newAmount);
    }
}
