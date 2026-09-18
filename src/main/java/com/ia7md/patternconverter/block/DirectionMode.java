package com.ia7md.patternconverter.block;

import com.ia7md.patternconverter.api.ConversionDirection;

import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.Nullable;

public enum DirectionMode implements StringRepresentable {
    AUTO("auto", null),
    FORCE_RS_TO_AE("force_rs_to_ae", ConversionDirection.RS_TO_AE),
    FORCE_AE_TO_RS("force_ae_to_rs", ConversionDirection.AE_TO_RS);

    private final String name;
    @Nullable
    private final ConversionDirection forced;

    DirectionMode(String name, @Nullable ConversionDirection forced) {
        this.name = name;
        this.forced = forced;
    }

    @Nullable
    public ConversionDirection forced() {
        return forced;
    }

    public DirectionMode next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public static DirectionMode byOrdinal(int ordinal) {
        DirectionMode[] values = values();
        return values[Math.floorMod(ordinal, values.length)];
    }

    public static DirectionMode byName(String name) {
        for (DirectionMode mode : values()) {
            if (mode.name.equals(name)) {
                return mode;
            }
        }
        return AUTO;
    }

    public Component displayName() {
        return Component.translatable("mode.pattern_converter." + name);
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
