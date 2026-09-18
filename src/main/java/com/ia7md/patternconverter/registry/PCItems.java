package com.ia7md.patternconverter.registry;

import com.ia7md.patternconverter.PatternConverter;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Consumer;

public final class PCItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(PatternConverter.MOD_ID);

    public static final DeferredItem<BlockItem> PATTERN_CONVERTER = ITEMS.registerItem("pattern_converter",
            properties -> new BlockItem(PCBlocks.PATTERN_CONVERTER.get(), properties) {
                @Override
                public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                            Consumer<Component> tooltip, TooltipFlag flag) {
                    super.appendHoverText(stack, context, display, tooltip, flag);
                    tooltip.accept(Component.translatable("block.pattern_converter.pattern_converter.tooltip")
                            .withStyle(ChatFormatting.GRAY));
                }
            }, Item.Properties::new);

    private PCItems() {
    }
}
