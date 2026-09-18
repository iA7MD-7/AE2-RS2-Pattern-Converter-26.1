package com.ia7md.patternconverter.gametest;

import appeng.api.stacks.AEKey;
import com.glodblock.github.appflux.common.me.key.FluxKey;
import com.glodblock.github.appflux.common.me.key.type.EnergyType;
import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.ultramega.refinedtypes.type.energy.EnergyResource;

final class AddonKeys {
    private AddonKeys() {
    }

    static ResourceKey refinedTypesFe() {
        return EnergyResource.ENERGY_RESOURCE;
    }

    static boolean isRefinedTypesFe(ResourceKey key) {
        return key instanceof EnergyResource;
    }

    static boolean isAppliedFluxFe(AEKey key) {
        return key instanceof FluxKey flux && flux.getEnergyType() == EnergyType.FE;
    }
}
