package ru.terramain.microprocessor.plate;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public interface PlateDataCodec<D extends PlateData> {
    D defaultData();
    Codec<D> getCodec();
    StreamCodec<RegistryFriendlyByteBuf, D> getStreamCodec();
}
