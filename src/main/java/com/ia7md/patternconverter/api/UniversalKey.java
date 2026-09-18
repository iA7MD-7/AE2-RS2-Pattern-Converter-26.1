package com.ia7md.patternconverter.api;

import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

import java.util.Objects;

public sealed interface UniversalKey permits UniversalKey.Item, UniversalKey.Fluid, UniversalKey.Custom {
    Component displayName();

    default boolean isStandard() {
        return this instanceof Item || this instanceof Fluid;
    }

    final class Item implements UniversalKey {
        private final ItemStack stack;

        public Item(ItemStack stack) {
            this.stack = stack.copyWithCount(1);
        }

        public ItemStack stack() {
            return stack;
        }

        public ItemStack stack(int count) {
            return stack.copyWithCount(count);
        }

        @Override
        public Component displayName() {
            return stack.getHoverName();
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Item other && ItemStack.isSameItemSameComponents(stack, other.stack);
        }

        @Override
        public int hashCode() {
            return ItemStack.hashItemAndComponents(stack);
        }

        @Override
        public String toString() {
            return "Item[" + stack + "]";
        }
    }

    record Fluid(net.minecraft.world.level.material.Fluid fluid, DataComponentPatch components) implements UniversalKey {
        public Fluid {
            Objects.requireNonNull(fluid, "fluid");
            components = components == null ? DataComponentPatch.EMPTY : components;
        }

        public static Fluid of(net.minecraft.world.level.material.Fluid fluid) {
            return new Fluid(fluid, DataComponentPatch.EMPTY);
        }

        @Override
        public Component displayName() {
            return fluid.getFluidType().getDescription();
        }
    }

    record Custom(Identifier type) implements UniversalKey {
        @Override
        public Component displayName() {
            return Component.translatable("key_type." + type.getNamespace() + "." + type.getPath());
        }
    }
}
