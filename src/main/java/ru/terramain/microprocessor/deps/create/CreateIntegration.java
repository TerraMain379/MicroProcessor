package ru.terramain.microprocessor.deps.create;

import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import ru.terramain.microprocessor.block.MicroProcessorBlock;

public class CreateIntegration {
    public static void integrate() {
        MovementBehaviour.REGISTRY.register(
                MicroProcessorBlock.instance().get(),
                new MicroProcessorMovementBehaviour()
        );
    }
}
