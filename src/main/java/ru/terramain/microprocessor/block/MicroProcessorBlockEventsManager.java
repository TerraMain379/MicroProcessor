package ru.terramain.microprocessor.block;

import ru.terramain.microprocessor.plate.plates.AbstractPlatePiston;

import java.util.ArrayList;
import java.util.function.BiFunction;
import java.util.function.Function;

public class MicroProcessorBlockEventsManager {
    private static final ArrayList<BlockEventHandler<?>> handlers = new ArrayList<>();

    public static <T> int registerBlockEvent(BiFunction<MicroProcessorBlockEntity, T, Boolean> handler, Function<T, Integer> encoder, Function<Integer, T> decoder) {
        handlers.add(new BlockEventHandler<>(
                handler,
                encoder,
                decoder
        ));
        return handlers.size() - 1;
    }
    public static boolean handleBlockEvent(int id, MicroProcessorBlockEntity be, int arg) {
        return handlers.get(id).apply(be, arg);
    }
    public static <T> void triggerBlockEvent(int id, MicroProcessorBlockEntity be, T arg) {
        BlockEventHandler<T> blockEventHandler = (BlockEventHandler<T>) handlers.get(id);
        be.getLevel().blockEvent(
                be.getBlockPos(),
                be.getBlockState().getBlock(),
                id,
                blockEventHandler.encoder.apply(arg)
        );
    }


    private record BlockEventHandler<T> (
            BiFunction<MicroProcessorBlockEntity, T, Boolean> handler,
            Function<T, Integer> encoder,
            Function<Integer, T> decoder
    ) {
        public boolean apply(MicroProcessorBlockEntity be, int arg) {
            return handler.apply(be, decoder.apply(arg));
        }
    }


    public static void registerDefault() {
        int x;
        x = AbstractPlatePiston.BLOCK_EVENT_POWER_ON;
        x = AbstractPlatePiston.BLOCK_EVENT_POWER_OFF;
    }
}
