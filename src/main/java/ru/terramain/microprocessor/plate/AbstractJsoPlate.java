package ru.terramain.microprocessor.plate;

import net.minecraft.core.Direction;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import ru.terramain.microprocessor.logic.MicroProcessorWorker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public abstract class AbstractJsoPlate {
    public final Plate<?> plate;
    public final Direction direction;
    public final MicroProcessorWorker worker;
    public HashMap<String, List<Value>> eventHandlers;

    public AbstractJsoPlate(MicroProcessorWorker worker, Plate<?> plate, Direction direction, PlateState<?, ?> plateState) {
        this.worker = worker;
        this.plate = plate;
        this.direction = direction;
        this.eventHandlers = new HashMap<>();
    }

    public abstract void update(PlateState<?, ?> plateState);
    public boolean checkAndUpdate(PlateState<?, ?> plateState) {
        if (plateState.plate == this.plate) {
            update(plateState);
            return true;
        }
        return false;
    }


    public void runEvent(String event, Object[] args) { // worker thread
        if (!eventHandlers.containsKey(event)) return;
        eventHandlers.get(event).forEach(handler -> {
            try {
                handler.executeVoid(args);
            }
            catch (Exception e) {
                this.worker.reportFatalError(e, MicroProcessorWorker.LogMessage.ErrorSource.PLATE_EVENT);
            }
        });
    }

    @HostAccess.Export
    public void on(String event, Value handler) { // universal thread
        if (!handler.canExecute()) {
            throw new IllegalArgumentException("handler must be a function");
        }
        eventHandlers.computeIfAbsent(event, key -> new ArrayList<>());
        eventHandlers.get(event).add(handler);
    }
}
