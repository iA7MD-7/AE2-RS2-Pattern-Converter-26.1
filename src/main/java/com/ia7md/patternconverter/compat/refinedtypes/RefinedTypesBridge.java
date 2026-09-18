package com.ia7md.patternconverter.compat.refinedtypes;

import com.ia7md.patternconverter.PCConfig;
import com.ia7md.patternconverter.api.CustomKeyTypes;
import com.ia7md.patternconverter.api.KeyBridge;
import com.ia7md.patternconverter.api.UniversalKey;
import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.ultramega.refinedtypes.type.energy.EnergyResource;

import java.util.Optional;

final class RefinedTypesBridge implements KeyBridge<ResourceKey> {
    @Override
    public Optional<UniversalKey> toUniversal(ResourceKey key) {
        if (!PCConfig.ADDON_REFINED_TYPES.get()) {
            return Optional.empty();
        }
        if (key instanceof EnergyResource) {
            return Optional.of(CustomKeyTypes.forgeEnergy());
        }
        return Optional.empty();
    }

    @Override
    public Optional<ResourceKey> fromUniversal(UniversalKey key) {
        if (!PCConfig.ADDON_REFINED_TYPES.get() || !(key instanceof UniversalKey.Custom custom)) {
            return Optional.empty();
        }
        if (custom.type().equals(CustomKeyTypes.FORGE_ENERGY)) {
            return Optional.of(EnergyResource.ENERGY_RESOURCE);
        }
        return Optional.empty();
    }
}
