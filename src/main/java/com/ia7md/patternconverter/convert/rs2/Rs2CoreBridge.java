package com.ia7md.patternconverter.convert.rs2;

import com.ia7md.patternconverter.api.KeyBridge;
import com.ia7md.patternconverter.api.UniversalKey;
import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.common.support.resource.FluidResource;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;

import java.util.Optional;

final class Rs2CoreBridge implements KeyBridge<ResourceKey> {
    @Override
    public Optional<UniversalKey> toUniversal(ResourceKey key) {
        if (key instanceof ItemResource item) {
            return Optional.of(new UniversalKey.Item(item.toItemStack()));
        }
        if (key instanceof FluidResource fluid) {
            return Optional.of(new UniversalKey.Fluid(fluid.fluid(), fluid.components()));
        }
        return Optional.empty();
    }

    @Override
    public Optional<ResourceKey> fromUniversal(UniversalKey key) {
        if (key instanceof UniversalKey.Item item) {
            return Optional.of(ItemResource.ofItemStack(item.stack()));
        }
        if (key instanceof UniversalKey.Fluid fluid) {
            return Optional.of(new FluidResource(fluid.fluid(), fluid.components()));
        }
        return Optional.empty();
    }
}
