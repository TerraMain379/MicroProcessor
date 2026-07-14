package ru.terramain.microprocessor.scriptscreen;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import ru.terramain.microprocessor.MicroProcessorMod;

@EventBusSubscriber(modid = MicroProcessorMod.MODID)
public final class ServerGameEvents {
    private ServerGameEvents() {
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ScriptScreenSessions.close(player);
        }
    }
}
