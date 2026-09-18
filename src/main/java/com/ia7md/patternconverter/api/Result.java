package com.ia7md.patternconverter.api;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public record Result<T>(@Nullable T value, List<Component> warnings, @Nullable Component failure) {
    public Result {
        warnings = List.copyOf(warnings);
        if ((value == null) == (failure == null)) {
            throw new IllegalArgumentException("a result is either a value or a failure");
        }
    }

    public static <T> Result<T> ok(T value) {
        return new Result<>(value, List.of(), null);
    }

    public static <T> Result<T> ok(T value, List<Component> warnings) {
        return new Result<>(value, warnings, null);
    }

    public static <T> Result<T> fail(Component reason) {
        return new Result<>(null, List.of(), reason);
    }

    public boolean isOk() {
        return failure == null;
    }

    public T get() {
        if (value == null) {
            throw new IllegalStateException("result is a failure: " + failure);
        }
        return value;
    }

    public <U> Result<U> castFailure() {
        if (failure == null) {
            throw new IllegalStateException("not a failure");
        }
        return Result.fail(failure);
    }

    public <U> Result<U> map(Function<T, U> fn) {
        return isOk() ? new Result<>(fn.apply(value), warnings, null) : castFailure();
    }

    public Result<T> withWarnings(List<Component> earlier) {
        if (earlier.isEmpty()) {
            return this;
        }
        List<Component> merged = new ArrayList<>(earlier);
        merged.addAll(warnings);
        return new Result<>(value, merged, failure);
    }
}
