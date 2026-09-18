package com.ia7md.patternconverter.compat.appflux;

import appeng.api.stacks.AEKey;
import com.glodblock.github.appflux.common.me.key.FluxKey;
import com.glodblock.github.appflux.common.me.key.type.EnergyType;
import com.ia7md.patternconverter.PCConfig;
import com.ia7md.patternconverter.api.CustomKeyTypes;
import com.ia7md.patternconverter.api.KeyBridge;
import com.ia7md.patternconverter.api.PatternConverterApi;
import com.ia7md.patternconverter.api.UniversalKey;

import java.util.Optional;

public final class AppliedFluxCompat {
    private AppliedFluxCompat() {
    }

    public static void register() {
        PatternConverterApi.registerAe2KeyBridge(new Bridge());
    }

    private static final class Bridge implements KeyBridge<AEKey> {
        @Override
        public Optional<UniversalKey> toUniversal(AEKey key) {
            if (PCConfig.ADDON_APPLIED_FLUX.get() && key instanceof FluxKey flux && flux.getEnergyType() == EnergyType.FE) {
                return Optional.of(CustomKeyTypes.forgeEnergy());
            }
            return Optional.empty();
        }

        @Override
        public Optional<AEKey> fromUniversal(UniversalKey key) {
            if (PCConfig.ADDON_APPLIED_FLUX.get() && key instanceof UniversalKey.Custom custom
                    && custom.type().equals(CustomKeyTypes.FORGE_ENERGY)) {
                return Optional.of(FluxKey.of(EnergyType.FE));
            }
            return Optional.empty();
        }
    }
}
