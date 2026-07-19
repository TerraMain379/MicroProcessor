package ru.terramain.microprocessor.network;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import ru.terramain.microprocessor.MicroProcessorMod;
import ru.terramain.microprocessor.network.payload.MicroProcessorPistonActionPayload;
import ru.terramain.microprocessor.network.payload.scriptscreen.CloseScriptScreenPayload;
import ru.terramain.microprocessor.network.payload.scriptscreen.OpenScriptScreenPayload;
import ru.terramain.microprocessor.network.payload.PlatesUpdatePayload;
import ru.terramain.microprocessor.network.payload.scriptscreen.UpdateScriptScreenPayload;

@EventBusSubscriber(modid = MicroProcessorMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientNetworkHandler {
    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");


        registrar.playToClient( // s2c only packet
                OpenScriptScreenPayload.TYPE,
                OpenScriptScreenPayload.STREAM_CODEC,
                ClientPayloadHandlers::handleOpenScriptScreen
        );
        registrar.playBidirectional( // s2c & c2s
                UpdateScriptScreenPayload.TYPE,
                UpdateScriptScreenPayload.STREAM_CODEC,
                new DirectionalPayloadHandler<>(
                        ClientPayloadHandlers::handleUpdateScriptScreen,
                        ServerPayloadHandlers::handleUpdateScriptScreen
                )
        );
        registrar.playBidirectional( // s2c & c2s
                CloseScriptScreenPayload.TYPE,
                CloseScriptScreenPayload.STREAM_CODEC,
                new DirectionalPayloadHandler<>(
                        ClientPayloadHandlers::handleCloseScriptScreen,
                        ServerPayloadHandlers::handleCloseScriptScreen
                )
        );


        registrar.playToClient(
                PlatesUpdatePayload.TYPE,
                PlatesUpdatePayload.STREAM_CODEC,
                ClientPayloadHandlers::handlePlatesUpdate
        );
        registrar.playToClient(
                MicroProcessorPistonActionPayload.TYPE,
                MicroProcessorPistonActionPayload.STREAM_CODEC,
                ClientPayloadHandlers::handleMicroProcessorPistonAction
        );
    }
}
