package ru.terramain.microprocessor.logic;

import net.minecraft.core.Direction;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import ru.terramain.microprocessor.plate.AbstractJsoPlate;
import ru.terramain.microprocessor.plate.PlateState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MicroProcessorJso {
    public final MicroProcessorWorker worker;
    public HashMap<String, List<Value>> eventHandlers;

    public AbstractJsoPlate[] jsoPlates;

    public MicroProcessorJso(MicroProcessorContext context, MicroProcessorWorker worker) {
        this.worker = worker;
        this.eventHandlers = new HashMap<>();
        this.jsoPlates = new AbstractJsoPlate[6];
        this.update(context);
    }
    public void update(MicroProcessorContext context) {
        for (Direction direction : Direction.values()) {
            PlateState<?, ?> plateState = context.be.getPlateState(direction);
            int index = direction.ordinal();
            AbstractJsoPlate jso = jsoPlates[index];

            if (plateState == null) {
                jsoPlates[index] = null;
            }
            else {
                boolean updated = false;
                if (jso != null) updated = jso.checkAndUpdate(plateState);
                if (!updated) {
                    jsoPlates[index] = plateState.plate.jsoGenerator.create(this.worker, direction, plateState);
                }
            }
        }
        // TODO:
    }



    public void runEvent(String event, Object[] args) {
        if (!eventHandlers.containsKey(event)) return;
        eventHandlers.get(event).forEach(handler -> {
            try {
                handler.executeVoid(args);
            }
            catch (Exception e) {
                worker.reportFatalError(e, MicroProcessorWorker.LogMessage.ErrorSource.MP_EVENT);
            }
        });
    }
    public void runPlateEvent(Direction direction, String event, Object[] args) {
        AbstractJsoPlate jso = this.jsoPlates[direction.ordinal()];
        jso.runEvent(event, args);
    }

    @HostAccess.Export
    public void on(String event, Value handler) {
        if (!handler.canExecute()) {
            throw new IllegalArgumentException("handler must be a function");
        }
        eventHandlers.computeIfAbsent(event, key -> new ArrayList<>());
        eventHandlers.get(event).add(handler);
    }

    @HostAccess.Export
    public Object plate(String direction) {
        Direction dir = switch (direction) {
            case "UP", "up" -> Direction.UP;
            case "DOWN", "down" -> Direction.DOWN;
            case "NORTH", "north" -> Direction.NORTH;
            case "SOUTH", "south" -> Direction.SOUTH;
            case "WEST", "west" -> Direction.WEST;
            case "EAST", "east" -> Direction.EAST;
            default -> throw new IllegalArgumentException();
        };
        AbstractJsoPlate jso = this.jsoPlates[dir.ordinal()];
        if (jso == null) return null;
        return new JsoPlate(jso);
    }
    public class JsoPlate {
        public AbstractJsoPlate jso;
        public JsoPlate(AbstractJsoPlate jso) {
            this.jso = jso;
        }

        @HostAccess.Export
        public String type() {
            return this.jso.plate.type;
        }

        @HostAccess.Export
        public Object is() {
            return this.jso;
        }


        @HostAccess.Export
        public Object is(String type) {
            if (is().equals(type)) {
                return jso;
            }
            return null;
        }
    }

    @HostAccess.Export
    public void log(Value value) {
        String content = value.isString() ? value.asString() : value.toString();
        this.worker.pushLog(content, MicroProcessorWorker.LogMessage.Level.LOG);
    }
    @HostAccess.Export
    public void warn(Value value) {
        String content = value.isString() ? value.asString() : value.toString();
        this.worker.pushLog(content, MicroProcessorWorker.LogMessage.Level.WARN);
    }
}
