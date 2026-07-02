package com.drmangotea.tfmg.content.engines.fuels;

import com.drmangotea.tfmg.TFMG;
import com.drmangotea.tfmg.registry.TFMGTags;

public class BaseFuelTypes {

    public static final FuelType

            FALLBACK = create("fallback")
            .speed(1)
            .efficiency(1)
            .stress(1)
            .register(),

    GASOLINE = create("gasoline")
            .speed(1)
            .efficiency(1)
            .stress(1)
            .registerAndAssign(TFMGTags.TFMGFluidTags.GASOLINE.tag),

    DIESEL = create("diesel")
            .speed(0.8f)
            .efficiency(0.8f)
            .stress(1.4f)
            .registerAndAssign(TFMGTags.TFMGFluidTags.DIESEL.tag),

    LPG = create("lpg")
            .speed(1.2f)
            .efficiency(0.7f)
            .stress(0.7f)
            .registerAndAssign(TFMGTags.TFMGFluidTags.LPG.tag),

    KEROSENE = create("kerosene")
            .speed(0.7f)
            .efficiency(1f)
            .stress(1.4f)
            .registerAndAssign(TFMGTags.TFMGFluidTags.KEROSENE.tag),

    NAPHTHA = create("naphtha")
            .speed(1f)
            .efficiency(0.7f)
            .stress(1.3f)
            .registerAndAssign(TFMGTags.TFMGFluidTags.NAPHTHA.tag),

    //MAZUT = create("mazut")
    //        .speed(0)
    //        .efficiency(0)
    //        .stress(0)
    //        .registerAndAssign(TFMGTags.TFMGFluidTags.GAS.tag),

    CREOSOTE = create("creosote")
            .speed(0.7f)
            .efficiency(0.4f)
            .stress(0.5f)
            .registerAndAssign(TFMGTags.TFMGFluidTags.CREOSOTE.tag),

    // Specialized variants of LPG (1.2 / 0.7 / 0.7): butane trades
    // efficiency for speed, propane trades speed for efficiency, and
    // hydrogen (vat electrolysis of water) is the fastest, cleanest and
    // weakest of the gas fuels.
    BUTANE = create("butane")
            .speed(1.3f)
            .efficiency(0.6f)
            .stress(0.6f)
            .registerAndAssign(TFMGTags.TFMGFluidTags.BUTANE.tag),

    PROPANE = create("propane")
            .speed(1.1f)
            .efficiency(0.9f)
            .stress(0.8f)
            .registerAndAssign(TFMGTags.TFMGFluidTags.PROPANE.tag),

    HYDROGEN = create("hydrogen")
            .speed(1.4f)
            .efficiency(0.5f)
            .stress(0.6f)
            .registerAndAssign(TFMGTags.TFMGFluidTags.HYDROGEN.tag),


    FURNACE_GAS = create("furnace_gas")
            .speed(0.5f)
            .efficiency(0.3f)
            .stress(0.3f)
            .registerAndAssign(TFMGTags.TFMGFluidTags.FURNACE_GAS.tag);


    private static FuelType.Builder create(String name) {
        return new FuelType.Builder(TFMG.asResource(name));
    }

    public static void register() {
    }
}