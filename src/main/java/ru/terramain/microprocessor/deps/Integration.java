package ru.terramain.microprocessor.deps;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

public class Integration {
    public static boolean IS_CREATE_LOADED = ModList.get().isLoaded("create");

    public static void registerEvent(IEventBus modEventBus) {
        if (IS_CREATE_LOADED) ru.terramain.microprocessor.deps.create.CreateIntegration.registerEvent(modEventBus);
    }
    public static void setupEvent(FMLCommonSetupEvent event) {
        if (IS_CREATE_LOADED) event.enqueueWork(ru.terramain.microprocessor.deps.create.CreateIntegration::setupEvent);
    }
}
