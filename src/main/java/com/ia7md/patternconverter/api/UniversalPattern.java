package com.ia7md.patternconverter.api;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public sealed interface UniversalPattern
        permits UniversalPattern.Crafting, UniversalPattern.Processing,
        UniversalPattern.Stonecutting, UniversalPattern.Smithing {
    default String shapeKey() {
        return switch (this) {
            case Crafting c -> "crafting";
            case Processing p -> "processing";
            case Stonecutting s -> "stonecutting";
            case Smithing s -> "smithing";
        };
    }

    record Crafting(List<ItemStack> grid, ItemStack result, @Nullable ResourceKey<Recipe<?>> recipeId,
                    boolean substitute, boolean fluidSubstitute) implements UniversalPattern {
        public Crafting {
            if (grid.size() != 9) {
                throw new IllegalArgumentException("crafting grid must have 9 cells, got " + grid.size());
            }
            grid = List.copyOf(grid);
        }
    }

    record Processing(List<Input> inputs, List<UniversalStack> outputs) implements UniversalPattern {
        public Processing {
            inputs = List.copyOf(inputs);
            outputs = List.copyOf(outputs);
        }

        public record Input(UniversalStack primary, List<UniversalKey> alternatives) {
            public Input {
                alternatives = List.copyOf(alternatives);
            }

            public static Input of(UniversalStack stack) {
                return new Input(stack, List.of());
            }
        }
    }

    record Stonecutting(ItemStack input, ItemStack output, @Nullable ResourceKey<Recipe<?>> recipeId,
                        boolean substitute) implements UniversalPattern {
    }

    record Smithing(ItemStack template, ItemStack base, ItemStack addition, ItemStack result,
                    @Nullable ResourceKey<Recipe<?>> recipeId, boolean substitute) implements UniversalPattern {
    }
}
