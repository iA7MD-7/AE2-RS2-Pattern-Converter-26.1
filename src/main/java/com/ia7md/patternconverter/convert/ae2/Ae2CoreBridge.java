package com.ia7md.patternconverter.convert.ae2;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import com.ia7md.patternconverter.api.KeyBridge;
import com.ia7md.patternconverter.api.UniversalKey;

import net.neoforged.neoforge.fluids.FluidStack;

import java.util.Optional;

final class Ae2CoreBridge implements KeyBridge<AEKey> {
    @Override
    public Optional<UniversalKey> toUniversal(AEKey key) {
        if (key instanceof AEItemKey item) {
            return Optional.of(new UniversalKey.Item(item.toStack()));
        }
        if (key instanceof AEFluidKey fluid) {
            FluidStack stack = fluid.toStack(1);
            return Optional.of(new UniversalKey.Fluid(fluid.getFluid(), stack.getComponentsPatch()));
        }
        return Optional.empty();
    }

    @Override
    public Optional<AEKey> fromUniversal(UniversalKey key) {
        if (key instanceof UniversalKey.Item item) {
            return Optional.of(AEItemKey.of(item.stack()));
        }
        if (key instanceof UniversalKey.Fluid fluid) {
            FluidStack stack = new FluidStack(fluid.fluid().builtInRegistryHolder(), 1, fluid.components());
            return Optional.of(AEFluidKey.of(stack));
        }
        return Optional.empty();
    }
}
