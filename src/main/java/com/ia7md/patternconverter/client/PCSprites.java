package com.ia7md.patternconverter.client;

import com.ia7md.patternconverter.PatternConverter;
import com.ia7md.patternconverter.api.ConversionDirection;

import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

public final class PCSprites {
    public static final Identifier SHEET = PatternConverter.asResource("textures/gui/pattern_converter.png");

    public static final int PANEL_WIDTH = 176;
    public static final int PANEL_HEIGHT = 168;

    public static final int ARROW_U = 176;
    public static final int ARROW_W = 24;
    public static final int ARROW_H = 12;
    public static final int ARROW_AE_TO_RS_V = 0;
    public static final int ARROW_RS_TO_AE_V = 12;
    public static final int ARROW_IDLE_V = 24;
    public static final int ARROW_INK_X = 4;
    public static final int ARROW_INK_W = 19;

    public static final int GHOST_V = 48;
    public static final int GHOST_RS_U = 177;
    public static final int GHOST_AE_U = 193;

    public static final int ICON_SIZE = 18;
    public static final int ICON_V = 80;
    public static final int ICON_HOVER_V = 98;
    public static final int ICON_ISSUES_U = 226;
    public static final int BUTTON_BG_U = 208;
    public static final int BUTTON_BG_V = 128;
    public static final int BUTTON_BG_HOVER_V = 146;
    public static final int SIDE_BUTTON_X = -20;
    public static final int SIDE_BUTTON_Y = 6;

    private PCSprites() {
    }

    public static int arrowV(@Nullable ConversionDirection direction) {
        if (direction == null) {
            return ARROW_IDLE_V;
        }
        return direction == ConversionDirection.RS_TO_AE ? ARROW_RS_TO_AE_V : ARROW_AE_TO_RS_V;
    }
}
