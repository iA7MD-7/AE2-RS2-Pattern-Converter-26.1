package com.ia7md.patternconverter.registry;

import com.ia7md.patternconverter.PatternConverter;
import com.ia7md.patternconverter.block.PatternConverterBlockEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class PCBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, PatternConverter.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PatternConverterBlockEntity>> PATTERN_CONVERTER =
            BLOCK_ENTITY_TYPES.register("pattern_converter",
                    () -> new BlockEntityType<>(PatternConverterBlockEntity::new, PCBlocks.PATTERN_CONVERTER.get()));

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Item.BLOCK, PATTERN_CONVERTER.get(),
                (be, side) -> be.getAutomationHandler());
    }

    private PCBlockEntities() {
    }
}
