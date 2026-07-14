package ru.terramain.microprocessor.network.payload;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import ru.terramain.microprocessor.MicroProcessorMod;
import ru.terramain.microprocessor.plate.Plates;

public record PlatesUpdatePayload(
        BlockPos pos,
        Plates.Snapshot plates
) implements CustomPacketPayload {
    public static final Type<PlatesUpdatePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MicroProcessorMod.MODID, "plates_update"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PlatesUpdatePayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                BlockPos.STREAM_CODEC.encode(buf, payload.pos());
                Plates.STREAM_CODEC.encode(buf, payload.plates());
            },
            buf -> new PlatesUpdatePayload(
                    BlockPos.STREAM_CODEC.decode(buf),
                    Plates.STREAM_CODEC.decode(buf)
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
