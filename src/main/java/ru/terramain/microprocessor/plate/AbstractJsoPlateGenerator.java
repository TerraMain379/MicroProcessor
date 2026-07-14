package ru.terramain.microprocessor.plate;

import net.minecraft.core.Direction;
import ru.terramain.microprocessor.logic.MicroProcessorWorker;

public interface AbstractJsoPlateGenerator<J extends AbstractJsoPlate> {
    J create(MicroProcessorWorker worker, Direction direction, PlateState<?, ?> plateState);
}
