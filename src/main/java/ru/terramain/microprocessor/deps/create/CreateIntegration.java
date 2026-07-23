package ru.terramain.microprocessor.deps.create;

import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.IEventBus;
import org.jetbrains.annotations.Nullable;
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


    public static @Nullable SubLevel getContaining(BlockEntity blockEntity) {
        return Sable.HELPER.getContaining(blockEntity);
    }
}
