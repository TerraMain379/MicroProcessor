package ru.terramain.microprocessor.deps;

import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

public class Integration {
    public static boolean IS_CREATE_LOADED = ModList.get().isLoaded("create");

    public static void integrate(FMLCommonSetupEvent event) {
        if (IS_CREATE_LOADED) event.enqueueWork(ru.terramain.microprocessor.deps.create.CreateIntegration::integrate);
    }
}
