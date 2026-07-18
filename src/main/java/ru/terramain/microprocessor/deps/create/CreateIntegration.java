package ru.terramain.microprocessor.deps.create;

import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import ru.terramain.microprocessor.block.MicroProcessorBlock;
import ru.terramain.microprocessor.deps.create.plates.PlateKinetic;

public class CreateIntegration {
    public static void registerEvent(IEventBus modEventBus) {
        PlateKinetic.instance();
    }

    public static void setupEvent() {
        MovementBehaviour.REGISTRY.register(
                MicroProcessorBlock.instance().get(),
                new MicroProcessorMovementBehaviour()
        );
    }
}
