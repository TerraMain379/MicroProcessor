package ru.terramain.microprocessor.scriptscreen;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import ru.terramain.microprocessor.network.payload.scriptscreen.CloseScriptScreenPayload;
import ru.terramain.microprocessor.network.payload.scriptscreen.UpdateScriptScreenPayload;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ScriptScreenSessions {
    private static final Map<BlockPos, Set<UUID>> VIEWERS_BY_POS = new ConcurrentHashMap<>();
    private static final Map<UUID, BlockPos> POS_BY_PLAYER = new ConcurrentHashMap<>();

    private ScriptScreenSessions() { }

    public static void open(ServerPlayer player, BlockPos pos) {
        close(player);
        POS_BY_PLAYER.put(player.getUUID(), pos);
        VIEWERS_BY_POS
                .computeIfAbsent(pos, ignored -> ConcurrentHashMap.newKeySet())
                .add(player.getUUID());
    }

    public static void close(ServerPlayer player) {
        BlockPos pos = POS_BY_PLAYER.remove(player.getUUID());
        if (pos == null) {
            return;
        }
        Set<UUID> viewers = VIEWERS_BY_POS.get(pos);
        if (viewers != null) {
            viewers.remove(player.getUUID());
            if (viewers.isEmpty()) {
                VIEWERS_BY_POS.remove(pos);
            }
        }
    }
    public static void sendClose(ServerPlayer player, BlockPos pos) {
        PacketDistributor.sendToPlayer(player, new CloseScriptScreenPayload(pos));
    }

    public static void closeAll(MinecraftServer server, BlockPos pos) {
        Set<UUID> uuids = VIEWERS_BY_POS.get(pos);
        if (uuids != null) uuids.forEach(uuid -> {
            ServerPlayer viewer = server.getPlayerList().getPlayer(uuid);
            close(viewer);
            sendClose(viewer, pos);
        });
    }

    public static boolean isViewing(ServerPlayer player, BlockPos pos) {
        return pos.equals(POS_BY_PLAYER.get(player.getUUID()));
    }

    public static void sendUpdate(MinecraftServer server, BlockPos pos, UpdateScriptScreenPayload payload) {
        sendUpdate(server, pos, payload, null);
    }

    public static void sendUpdate(MinecraftServer server, BlockPos pos, UpdateScriptScreenPayload payload, ServerPlayer exclude) {
        Set<UUID> viewers = VIEWERS_BY_POS.get(pos);
        if (viewers == null || viewers.isEmpty()) {
            return;
        }
        for (UUID viewerId : viewers) {
            if (exclude != null && exclude.getUUID().equals(viewerId)) {
                continue;
            }
            ServerPlayer viewer = server.getPlayerList().getPlayer(viewerId);
            if (viewer != null) {
                PacketDistributor.sendToPlayer(viewer, payload);
            }
        }
    }
}
