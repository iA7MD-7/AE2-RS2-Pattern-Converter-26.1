package com.ia7md.patternconverter.convert.rs2;

import com.ia7md.patternconverter.api.PatternConverterApi;
import com.refinedmods.refinedstorage.common.autocrafting.PatternItem;
import com.refinedmods.refinedstorage.common.autocrafting.PatternState;
import com.refinedmods.refinedstorage.common.autocrafting.patterngrid.PatternType;
import com.refinedmods.refinedstorage.common.content.DataComponents;
import com.refinedmods.refinedstorage.common.content.Items;

import com.refinedmods.refinedstorage.api.resource.ResourceAmount;
import com.refinedmods.refinedstorage.common.autocrafting.ProcessingPatternState;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class Rs2Patterns {
    private Rs2Patterns() {
    }

    public static void register() {
        PatternConverterApi.registerRs2KeyBridge(new Rs2CoreBridge());
        PatternConverterApi.registerTranslator(new Rs2Translator());
        PatternConverterApi.registerTranslator(new Rs2GenericTranslator());
        PatternConverterApi.registerEncoder(new Rs2Encoder());
    }

    public static boolean isRs2PatternItem(ItemStack stack) {
        return stack.getItem() instanceof PatternItem;
    }

    public static boolean isBlank(ItemStack stack) {
        return isRs2PatternItem(stack) && !stack.has(DataComponents.INSTANCE.getPatternState());
    }

    public static ItemStack blank() {
        return new ItemStack(Items.INSTANCE.getPattern());
    }

    static ItemStack newEncoded(PatternType type) {
        ItemStack stack = blank();
        stack.set(DataComponents.INSTANCE.getPatternState(), new PatternState(UUID.randomUUID(), type));
        return stack;
    }

    public static ItemStack exampleProcessingPattern(ItemStack input, ItemStack output) {
        List<Optional<ProcessingPatternState.ProcessingIngredient>> ingredients = new ArrayList<>();
        List<Optional<ResourceAmount>> outputs = new ArrayList<>();
        ingredients.add(Optional.of(new ProcessingPatternState.ProcessingIngredient(
                new ResourceAmount(ItemResource.ofItemStack(input), input.getCount()), List.of())));
        outputs.add(Optional.of(new ResourceAmount(ItemResource.ofItemStack(output), output.getCount())));
        for (int i = 1; i < 81; i++) {
            ingredients.add(Optional.empty());
            outputs.add(Optional.empty());
        }
        ItemStack stack = newEncoded(PatternType.PROCESSING);
        stack.set(DataComponents.INSTANCE.getProcessingPatternState(), new ProcessingPatternState(ingredients, outputs));
        return stack;
    }
}
