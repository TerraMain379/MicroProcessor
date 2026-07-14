package ru.terramain.microprocessor.plate;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;

public class Plates {
    public static final int SLOT_COUNT = 6;

    public static final Codec<PlateState<?, ?>[]> PLATES_ARRAY_CODEC = new Codec<>() {
        @Override
        public <T> DataResult<Pair<PlateState<?, ?>[], T>> decode(DynamicOps<T> ops, T input) {
            return ops.getList(input).map(consumer -> {
                List<T> entries = new ArrayList<>();
                consumer.accept(entries::add);
                PlateState<?, ?>[] plates = new PlateState[SLOT_COUNT];
                for (int i = 0; i < SLOT_COUNT && i < entries.size(); i++) {
                    plates[i] = PlateState.NULLABLE_CODEC.decode(ops, entries.get(i))
                            .result()
                            .map(Pair::getFirst)
                            .orElse(null);
                }
                return Pair.of(plates, input);
            });
        }

        @Override
        public <T> DataResult<T> encode(PlateState<?, ?>[] input, DynamicOps<T> ops, T prefix) {
            var builder = ops.listBuilder();
            for (int i = 0; i < SLOT_COUNT; i++) {
                PlateState<?, ?> plateState = input[i];
                T encoded = PlateState.NULLABLE_CODEC.encode(plateState, ops, ops.emptyMap()).getOrThrow();
                builder.add(encoded);
            }
            return builder.build(prefix);
        }
    };

    public static Codec<Plates.Snapshot> CODEC = RecordCodecBuilder.create(i -> i.group(
            PLATES_ARRAY_CODEC.fieldOf("plates").forGetter(Snapshot::plates)
    ).apply(i, Snapshot::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, Plates.Snapshot> STREAM_CODEC = StreamCodec.of(
            (buf, snapshot) -> {
                for (int i = 0; i < SLOT_COUNT; i++) {
                    PlateState.NULLABLE_STREAM_CODEC.encode(buf, snapshot.plates()[i]);
                }
            },
            buf -> {
                PlateState<?, ?>[] plates = new PlateState[SLOT_COUNT];
                for (int i = 0; i < SLOT_COUNT; i++) {
                    plates[i] = PlateState.NULLABLE_STREAM_CODEC.decode(buf);
                }
                return new Snapshot(plates);
            }
    );

    public PlateState<?, ?>[] plates;

    public Plates(PlateState<?, ?>[] plates) {
        this.plates = plates;
    }

    public Plates() {
        this.plates = new PlateState[SLOT_COUNT];
    }

    public record Snapshot(PlateState<?, ?>[] plates) {
        public static Snapshot by(Plates plates) {
            PlateState<?, ?>[] plateStates = new PlateState[SLOT_COUNT];
            for (int i = 0; i < SLOT_COUNT; i++) {
                plateStates[i] = plates.plates[i];
                if (plateStates[i] != null) {
                    plateStates[i] = plateStates[i].copy();
                }
            }
            return new Snapshot(plateStates);
        }

        public static Snapshot by() {
            return new Snapshot(new PlateState[SLOT_COUNT]);
        }

        public Plates copy() {
            PlateState<?, ?>[] plateStates = new PlateState[SLOT_COUNT];
            for (int i = 0; i < SLOT_COUNT; i++) {
                plateStates[i] = plates[i];
                if (plateStates[i] != null) {
                    plateStates[i] = plateStates[i].copy();
                }
            }
            return new Plates(plateStates);
        }
    }
}
