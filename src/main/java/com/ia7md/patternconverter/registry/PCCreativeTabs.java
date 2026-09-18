package com.ia7md.patternconverter.registry;

import com.ia7md.patternconverter.PatternConverter;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class PCCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, PatternConverter.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.pattern_converter.main"))
                    .icon(() -> PCItems.PATTERN_CONVERTER.get().getDefaultInstance())
                    .displayItems((parameters, output) -> output.accept(PCItems.PATTERN_CONVERTER.get()))
                    .build());

    private PCCreativeTabs() {
    }
}
