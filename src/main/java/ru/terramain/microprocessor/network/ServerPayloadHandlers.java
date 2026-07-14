package ru.terramain.microprocessor.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import ru.terramain.microprocessor.block.MicroProcessorBlockEntity;
import ru.terramain.microprocessor.network.payload.scriptscreen.CloseScriptScreenPayload;
import ru.terramain.microprocessor.network.payload.scriptscreen.UpdateScriptScreenPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import ru.terramain.microprocessor.scriptscreen.ScriptScreenSessions;

public final class ServerPayloadHandlers {
    private ServerPayloadHandlers() {}

    public static void handleUpdateScriptScreen(UpdateScriptScreenPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            Level level = player.level();
            BlockEntity blockEntity = level.getBlockEntity(payload.pos());
            if (!(blockEntity instanceof MicroProcessorBlockEntity be)) return;
            if (!ScriptScreenSessions.isViewing(player, payload.pos())) {
                CloseScriptScreenPayload closePayload = new CloseScriptScreenPayload(payload.pos());
                PacketDistributor.sendToPlayer(player, closePayload);
                return;
            }

            be.handleUpdatePacket(payload, player);
        });
    }

    public static void handleCloseScriptScreen(CloseScriptScreenPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            ScriptScreenSessions.close(player);
        });
    }
}
