package com.ia7md.patternconverter.client;

import com.ia7md.patternconverter.PatternConverter;
import com.ia7md.patternconverter.registry.PCMenus;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = PatternConverter.MOD_ID, value = Dist.CLIENT)
public final class PCClient {
    private PCClient() {
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(PCMenus.PATTERN_CONVERTER.get(), PatternConverterScreen::new);
    }
}
