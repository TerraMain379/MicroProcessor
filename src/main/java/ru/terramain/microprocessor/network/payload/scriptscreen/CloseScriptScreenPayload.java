package ru.terramain.microprocessor.network.payload.scriptscreen;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

// s2c = You close screen!!!
// c2s = I close screen with this data!!!
public record CloseScriptScreenPayload(
        BlockPos pos
) implements CustomPacketPayload {
    public static final Type<CloseScriptScreenPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("microprocessor", "close_script_screen"));

    public static final StreamCodec<FriendlyByteBuf, CloseScriptScreenPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, CloseScriptScreenPayload::pos,
            CloseScriptScreenPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
