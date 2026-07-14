package ru.terramain.microprocessor.plate;

import net.minecraft.core.Direction;
import ru.terramain.microprocessor.logic.MicroProcessorContext;

public class PlateActionContext<S extends PlateState<?, ?>> {
    public S plateState;
    public Direction direction;
    public MicroProcessorContext context;

    public PlateActionContext(S plateState, Direction direction, MicroProcessorContext context) {
        this.plateState = plateState;
        this.direction = direction;
        this.context = context;
    }

    public void setChanged() {
        this.context.be.setChanged();
    }
}
