package ru.terramain.microprocessor.network;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import ru.terramain.microprocessor.MicroProcessorMod;
import ru.terramain.microprocessor.network.payload.scriptscreen.CloseScriptScreenPayload;
import ru.terramain.microprocessor.network.payload.scriptscreen.OpenScriptScreenPayload;
import ru.terramain.microprocessor.network.payload.PlatesUpdatePayload;
import ru.terramain.microprocessor.network.payload.scriptscreen.UpdateScriptScreenPayload;

@EventBusSubscriber(modid = MicroProcessorMod.MODID, bus = EventBusSubscriber.Bus.MOD)
public class ServerNetworkHandler {
    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        if (!FMLEnvironment.dist.isDedicatedServer()) {
            return;
        }
        final PayloadRegistrar registrar = event.registrar("1");

        registrar.playToClient( // s2c only packet
                OpenScriptScreenPayload.TYPE,
                OpenScriptScreenPayload.STREAM_CODEC,
                (openScriptScreenPayload, iPayloadContext) -> {}
        );
        registrar.playBidirectional( // s2c & c2s
                UpdateScriptScreenPayload.TYPE,
                UpdateScriptScreenPayload.STREAM_CODEC,
                new DirectionalPayloadHandler<>(
                        (updateScriptScreenPayload, iPayloadContext) -> {},
                        ServerPayloadHandlers::handleUpdateScriptScreen
                )
        );
        registrar.playBidirectional( // s2c & c2s
                CloseScriptScreenPayload.TYPE,
                CloseScriptScreenPayload.STREAM_CODEC,
                new DirectionalPayloadHandler<>(
                        (closeScriptScreenPayload, iPayloadContext) -> {},
                        ServerPayloadHandlers::handleCloseScriptScreen
                )
        );


        registrar.playToClient(
                PlatesUpdatePayload.TYPE,
                PlatesUpdatePayload.STREAM_CODEC,
                (payload, context) -> {}
        );
    }
}
