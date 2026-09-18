package com.ia7md.patternconverter.registry;

import com.ia7md.patternconverter.PatternConverter;
import com.ia7md.patternconverter.menu.PatternConverterMenu;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class PCMenus {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, PatternConverter.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<PatternConverterMenu>> PATTERN_CONVERTER =
            MENU_TYPES.register("pattern_converter",
                    () -> IMenuTypeExtension.create(PatternConverterMenu::fromNetwork));

    private PCMenus() {
    }
}
