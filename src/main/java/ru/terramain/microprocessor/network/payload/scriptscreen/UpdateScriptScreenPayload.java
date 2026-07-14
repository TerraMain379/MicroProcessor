package ru.terramain.microprocessor.network.payload.scriptscreen;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;

// s2c = I update this mp, You render this
// c2s = I update this mp, You save this
public record UpdateScriptScreenPayload(
        BlockPos pos,
        boolean codeChanged,
        String code,
        boolean logsPushed,
        boolean logsCleared,
        ArrayList<String> logs,
        boolean isRunningChanged,
        boolean isRunning
) implements CustomPacketPayload {
    public static final Type<UpdateScriptScreenPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("microprocessor", "update_script_screen"));

    public static final StreamCodec<FriendlyByteBuf, UpdateScriptScreenPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, value) -> {
                BlockPos.STREAM_CODEC.encode(buffer, value.pos());
                ByteBufCodecs.BOOL.encode(buffer, value.codeChanged());
                ByteBufCodecs.STRING_UTF8.encode(buffer, value.code());
                ByteBufCodecs.BOOL.encode(buffer, value.logsPushed());
                ByteBufCodecs.BOOL.encode(buffer, value.logsCleared());
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).encode(buffer, value.logs());
                ByteBufCodecs.BOOL.encode(buffer, value.isRunningChanged());
                ByteBufCodecs.BOOL.encode(buffer, value.isRunning());
            },
            (buffer) -> new UpdateScriptScreenPayload(
                    BlockPos.STREAM_CODEC.decode(buffer),
                    ByteBufCodecs.BOOL.decode(buffer),
                    ByteBufCodecs.STRING_UTF8.decode(buffer),
                    ByteBufCodecs.BOOL.decode(buffer),
                    ByteBufCodecs.BOOL.decode(buffer),
                    new ArrayList<>(ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).decode(buffer)),
                    ByteBufCodecs.BOOL.decode(buffer),
                    ByteBufCodecs.BOOL.decode(buffer)
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static UpdateScriptScreenPayload createNoChangesPayload(BlockPos pos) {
        return new UpdateScriptScreenPayload(
                pos,
                false, "",
                false, false, new ArrayList<>(),
                false, false
        );
    }
}
