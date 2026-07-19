package ru.terramain.microprocessor.network.payload;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import ru.terramain.microprocessor.MicroProcessorMod;

public record MicroProcessorPistonActionPayload (
        BlockPos pos,
        Direction direction,
        boolean isExtend
) implements CustomPacketPayload {
    public static final Type<MicroProcessorPistonActionPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MicroProcessorMod.MODID, "microprocessor_piston_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MicroProcessorPistonActionPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                BlockPos.STREAM_CODEC.encode(buf, payload.pos());
                Direction.STREAM_CODEC.encode(buf, payload.direction());
                ByteBufCodecs.BOOL.encode(buf, payload.isExtend());
            },
            buf -> new MicroProcessorPistonActionPayload(
                    BlockPos.STREAM_CODEC.decode(buf),
                    Direction.STREAM_CODEC.decode(buf),
                    ByteBufCodecs.BOOL.decode(buf)
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
