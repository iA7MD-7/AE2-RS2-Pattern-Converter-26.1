package com.ia7md.patternconverter.convert;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public final class RecipeLookup {
    private RecipeLookup() {
    }

    @Nullable
    private static RecipeManager manager(Level level) {
        return level instanceof ServerLevel server ? server.recipeAccess() : null;
    }

    public static Optional<RecipeHolder<CraftingRecipe>> crafting(Level level, List<ItemStack> grid,
                                                                  @Nullable ResourceKey<Recipe<?>> preferredId) {
        RecipeManager recipes = manager(level);
        CraftingInput input = CraftingInput.of(3, 3, grid);
        if (recipes == null || input.isEmpty()) {
            return Optional.empty();
        }
        if (preferredId != null) {
            Optional<RecipeHolder<?>> byId = recipes.byKey(preferredId);
            if (byId.isPresent() && byId.get().value() instanceof CraftingRecipe cr && cr.matches(input, level)) {
                @SuppressWarnings("unchecked")
                RecipeHolder<CraftingRecipe> holder = (RecipeHolder<CraftingRecipe>) byId.get();
                return Optional.of(holder);
            }
        }
        return recipes.getRecipeFor(RecipeType.CRAFTING, input, level);
    }

    public static ItemStack craftingResult(Level level, RecipeHolder<CraftingRecipe> holder, List<ItemStack> grid) {
        return holder.value().assemble(CraftingInput.of(3, 3, grid));
    }

    public static Optional<RecipeHolder<StonecutterRecipe>> stonecutting(Level level, ItemStack input, ItemStack output,
                                                                         @Nullable ResourceKey<Recipe<?>> preferredId) {
        RecipeManager recipes = manager(level);
        if (recipes == null || input.isEmpty() || output.isEmpty()) {
            return Optional.empty();
        }
        SingleRecipeInput recipeInput = new SingleRecipeInput(input);
        RecipeHolder<StonecutterRecipe> fallback = null;
        for (RecipeHolder<StonecutterRecipe> holder : recipes.recipeMap()
                .getRecipesFor(RecipeType.STONECUTTING, recipeInput, level).toList()) {
            ItemStack result = holder.value().assemble(recipeInput);
            if (!ItemStack.isSameItemSameComponents(result, output)) {
                continue;
            }
            if (preferredId != null && holder.id().equals(preferredId)) {
                return Optional.of(holder);
            }
            if (fallback == null) {
                fallback = holder;
            }
        }
        return Optional.ofNullable(fallback);
    }

    public static ItemStack stonecuttingResult(RecipeHolder<StonecutterRecipe> holder, ItemStack input) {
        return holder.value().assemble(new SingleRecipeInput(input));
    }

    public static Optional<RecipeHolder<SmithingRecipe>> smithing(Level level, ItemStack template, ItemStack base,
                                                                  ItemStack addition, @Nullable ResourceKey<Recipe<?>> preferredId) {
        RecipeManager recipes = manager(level);
        if (recipes == null) {
            return Optional.empty();
        }
        SmithingRecipeInput input = new SmithingRecipeInput(template, base, addition);
        if (preferredId != null) {
            Optional<RecipeHolder<?>> byId = recipes.byKey(preferredId);
            if (byId.isPresent() && byId.get().value() instanceof SmithingRecipe sr && sr.matches(input, level)) {
                @SuppressWarnings("unchecked")
                RecipeHolder<SmithingRecipe> holder = (RecipeHolder<SmithingRecipe>) byId.get();
                return Optional.of(holder);
            }
        }
        return recipes.getRecipeFor(RecipeType.SMITHING, input, level);
    }

    public static ItemStack smithingResult(RecipeHolder<SmithingRecipe> holder, ItemStack template,
                                           ItemStack base, ItemStack addition) {
        return holder.value().assemble(new SmithingRecipeInput(template, base, addition));
    }
}
