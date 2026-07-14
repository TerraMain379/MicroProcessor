package ru.terramain.microprocessor;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import ru.terramain.microprocessor.plate.Plates;

import java.util.List;
import java.util.function.UnaryOperator;

public class MicroProcessorDataComponents {

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Plates.Snapshot>> PLATES = register("plates",
            builder -> builder
                    .persistent(Plates.CODEC)
                    .networkSynchronized(Plates.STREAM_CODEC)
    );
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> IS_ON = register("is_on",
            builder -> builder
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL)
    );
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> LAST_REDSTONE_SIGNAL = register("last_redstone_signal",
            builder -> builder
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.INT)
    );
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> CODE = register("code",
            builder -> builder
                    .persistent(Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8)
    );
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<String>>> LOGS = register("logs",
            builder -> builder
                    .persistent(Codec.STRING.listOf())
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()))
    );


    public static <T> DeferredHolder<DataComponentType<?>, DataComponentType<T>> register(String name, UnaryOperator<DataComponentType.Builder<T>> builderOperator) {
        return MicroProcessorMod.DATA_COMPONENT_TYPES.register(name, () -> builderOperator.apply(DataComponentType.builder()).build());
    }

    public static void register() { }
}
