package com.ia7md.patternconverter;

import com.ia7md.patternconverter.compat.AddonCompat;
import com.ia7md.patternconverter.convert.ae2.Ae2Patterns;
import com.ia7md.patternconverter.convert.rs2.Rs2Patterns;
import com.ia7md.patternconverter.registry.PCBlockEntities;
import com.ia7md.patternconverter.registry.PCBlocks;
import com.ia7md.patternconverter.registry.PCCreativeTabs;
import com.ia7md.patternconverter.registry.PCItems;
import com.ia7md.patternconverter.registry.PCMenus;

import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(PatternConverter.MOD_ID)
public class PatternConverter {
    public static final String MOD_ID = "pattern_converter";
    public static final String NAME = "AE2 & RS2 Pattern Converter";
    public static final Logger LOGGER = LoggerFactory.getLogger(NAME);

    public PatternConverter(IEventBus modBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, PCConfig.SPEC);
        PCBlocks.BLOCKS.register(modBus);
        PCItems.ITEMS.register(modBus);
        PCBlockEntities.BLOCK_ENTITY_TYPES.register(modBus);
        PCMenus.MENU_TYPES.register(modBus);
        PCCreativeTabs.TABS.register(modBus);
        modBus.addListener(this::commonSetup);
        modBus.addListener(PCBlockEntities::registerCapabilities);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            Rs2Patterns.register();
            Ae2Patterns.register();
            AddonCompat.register();
        });
    }

    public static Identifier asResource(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
