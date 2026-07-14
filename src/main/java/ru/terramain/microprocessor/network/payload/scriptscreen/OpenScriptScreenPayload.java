package ru.terramain.microprocessor.network.payload.scriptscreen;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

// s2c = You open screen with this data!!!
public record OpenScriptScreenPayload(
        UpdateScriptScreenPayload updateData
) implements CustomPacketPayload {
    public static final Type<OpenScriptScreenPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("microprocessor", "open_script_screen"));

    public static final StreamCodec<FriendlyByteBuf, OpenScriptScreenPayload> STREAM_CODEC = StreamCodec.composite(
            UpdateScriptScreenPayload.STREAM_CODEC, OpenScriptScreenPayload::updateData,
            OpenScriptScreenPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
