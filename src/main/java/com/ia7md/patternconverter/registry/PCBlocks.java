package com.ia7md.patternconverter.registry;

import com.ia7md.patternconverter.PatternConverter;
import com.ia7md.patternconverter.block.PatternConverterBlock;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class PCBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(PatternConverter.MOD_ID);

    public static final DeferredBlock<PatternConverterBlock> PATTERN_CONVERTER = BLOCKS.registerBlock("pattern_converter",
            PatternConverterBlock::new,
            () -> BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(1.8F, 6.0F)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> state.getValue(PatternConverterBlock.POWERED) ? 7 : 0));

    private PCBlocks() {
    }
}
