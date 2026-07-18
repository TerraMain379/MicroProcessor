package ru.terramain.microprocessor.plate;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public class PlateState<D extends PlateData, P extends Plate<D>> {
    public Codec<PlateState<D, P>> UNSTATIC_CODEC = new Codec<>() {
        @Override
        public <T> DataResult<Pair<PlateState<D, P>, T>> decode(DynamicOps<T> ops, T input) {
            return ops.getMap(input).flatMap(map -> {
                T typeVal = map.get("type");
                T dataVal = map.get("data");
                DataResult<Pair<String, T>> typeResult = Codec.STRING.decode(ops, typeVal);
                String type = typeResult.getOrThrow().getFirst();
                P plate = (P) PlateRegister.getPlateByType(type);
                Codec<D> codec = plate.dataCodec.getCodec();
                DataResult<Pair<D, T>> dataResult = codec.decode(ops, dataVal);
                D data = dataResult.getOrThrow().getFirst();
                PlateState<D, P> plateState = new PlateState<>(data, plate);
                return DataResult.success(Pair.of(plateState, input));
            });
        }

        @Override
        public <T> DataResult<T> encode(PlateState<D, P> input, DynamicOps<T> ops, T prefix) {
            String type = input.plate.type;
            D data = input.data;
            PlateDataCodec<D> codec = input.plate.dataCodec;
            return ops.mapBuilder()
                    .add("type", type, Codec.STRING)
                    .add("data", data, codec.getCodec())
                    .build(prefix);
        }
    };

    public static final Codec<PlateState<?, ?>> NULLABLE_CODEC = new Codec<>() {
        @Override
        public <T> DataResult<Pair<PlateState<?, ?>, T>> decode(DynamicOps<T> ops, T input) {
            if (isEmptyValue(ops, input)) {
                return DataResult.success(Pair.of(null, input));
            }
            return CODEC.decode(ops, input);
        }

        @Override
        public <T> DataResult<T> encode(PlateState<?, ?> input, DynamicOps<T> ops, T prefix) {
            if (input == null) {
                return DataResult.success(ops.emptyMap());
            }
            return CODEC.encode(input, ops, prefix);
        }
    };
    public static final StreamCodec<RegistryFriendlyByteBuf, PlateState<?, ?>> NULLABLE_STREAM_CODEC = new StreamCodec<>() {
        @Override
        public PlateState<?, ?> decode(RegistryFriendlyByteBuf buf) {
            if (!ByteBufCodecs.BOOL.decode(buf)) {
                return null;
            }
            return STREAM_CODEC.decode(buf);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, PlateState<?, ?> plateState) {
            ByteBufCodecs.BOOL.encode(buf, plateState != null);
            if (plateState != null) {
                STREAM_CODEC.encode(buf, plateState);
            }
        }
    };

    public static Codec<PlateState<?, ?>> CODEC = new Codec<PlateState<?, ?>>() {
        @Override
        public <T> DataResult<Pair<PlateState<?, ?>, T>> decode(DynamicOps<T> ops, T input) {
            return ops.getMap(input).flatMap(map -> {
                T typeVal = map.get("type");
                T dataVal = map.get("data");
                DataResult<Pair<String, T>> typeResult = Codec.STRING.decode(ops, typeVal);
                String type = typeResult.getOrThrow().getFirst();
                Plate<?> plate = PlateRegister.getPlateByType(type);
                Codec<? extends PlateData> codec = plate.dataCodec.getCodec();
                DataResult<? extends Pair<?, T>> dataResult = codec.decode(ops, dataVal);
                PlateData data;
                try {
                    data = (PlateData) dataResult.getOrThrow().getFirst();
                }
                catch (Exception e) {
                    e.printStackTrace();
                    data = plate.dataCodec.defaultData();
                }
                return decode1(ops, input, (Plate<PlateData>) plate, data);
            });
        }
        public <T, D extends PlateData> DataResult<Pair<PlateState<?, ?>, T>> decode1(DynamicOps<T> ops, T input, Plate<D> plate, D data) {
            PlateState<D, Plate<D>> plateState = new PlateState<>(data, plate);
            return DataResult.success(Pair.of(plateState, input));
        }

        @Override
        public <T> DataResult<T> encode(PlateState<?, ?> input, DynamicOps<T> ops, T prefix) {
            String type = input.plate.type;
            PlateData data = input.data;
            PlateDataCodec<PlateData> codec = (PlateDataCodec<PlateData>) input.plate.dataCodec;
            return ops.mapBuilder()
                    .add("type", type, Codec.STRING)
                    .add("data", data, codec.getCodec())
                    .build(prefix);
        }
    };
    public static StreamCodec<RegistryFriendlyByteBuf, PlateState<?, ?>> STREAM_CODEC = new StreamCodec<RegistryFriendlyByteBuf, PlateState<?, ?>>() {
        @Override
        public PlateState<?, ?> decode(RegistryFriendlyByteBuf buf) {
            String type = ByteBufCodecs.STRING_UTF8.decode(buf);
            Plate<?> plate = PlateRegister.getPlateByType(type);
            return decode1(plate, buf);
        }
        public <D extends PlateData> PlateState<D, Plate<D>> decode1(Plate<D> plate, RegistryFriendlyByteBuf buf) {
            StreamCodec<RegistryFriendlyByteBuf, D> dataCodec = plate.dataCodec.getStreamCodec();
            D plateData = dataCodec.decode(buf);
            return new PlateState<>(plateData, plate);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, PlateState<?, ?> plateState) {
            ByteBufCodecs.STRING_UTF8.encode(buf, plateState.plate.type);
            StreamCodec<RegistryFriendlyByteBuf, ? extends PlateData> dataCodec = plateState.plate.dataCodec.getStreamCodec();
            encode1(dataCodec, buf, plateState);
        }
        public <D extends PlateData> void encode1(StreamCodec<RegistryFriendlyByteBuf, D> dataCodec, RegistryFriendlyByteBuf buf, PlateState<?, ?> plateState) {
            dataCodec.encode(buf, (D) plateState.data);
        }
    };

    public D data;
    public P plate;

    public PlateState(D data, P plate) {
        this.data = data;
        this.plate = plate;
    }


    public static <D extends PlateData, P extends Plate<D>> PlateState<D, P> of(P plate) {
        return new PlateState<>(plate.dataCodec.defaultData(), plate);
    }

    public PlateState<D, P> copy() {
        return new PlateState<>(data.copy(), plate);
    }

    private static <T> boolean isEmptyValue(DynamicOps<T> ops, T input) {
        return ops.getMap(input).result().map(map -> !map.entries().iterator().hasNext()).orElse(false);
    }
}
