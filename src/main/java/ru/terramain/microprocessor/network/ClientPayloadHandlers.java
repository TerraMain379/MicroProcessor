package ru.terramain.microprocessor.network;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import ru.terramain.microprocessor.block.MicroProcessorBlockEntity;
import ru.terramain.microprocessor.network.payload.scriptscreen.CloseScriptScreenPayload;
import ru.terramain.microprocessor.scriptscreen.MicroProcessorScriptScreen;
import ru.terramain.microprocessor.network.payload.scriptscreen.OpenScriptScreenPayload;
import ru.terramain.microprocessor.network.payload.PlatesUpdatePayload;
import ru.terramain.microprocessor.network.payload.scriptscreen.UpdateScriptScreenPayload;

public final class ClientPayloadHandlers {

    public static void handleOpenScriptScreen(OpenScriptScreenPayload payload, IPayloadContext context) {
        MicroProcessorScriptScreen.instance = new MicroProcessorScriptScreen(payload);
        Minecraft.getInstance().setScreen(MicroProcessorScriptScreen.instance);
    }

    public static void handleUpdateScriptScreen(UpdateScriptScreenPayload payload, IPayloadContext context) {
        if (
                MicroProcessorScriptScreen.instance != null &&
                MicroProcessorScriptScreen.instance.blockPos.equals(payload.pos())
        ) {
            MicroProcessorScriptScreen.instance.handleUpdatePacket(payload);
        }
        else {
            // отправляем пакет о закрытии меню без изменений, чтобы сервер не думал, что мы смотрим в какое-то меню
            PacketDistributor.sendToServer(new CloseScriptScreenPayload(payload.pos()));
        }
    }

    public static void handleCloseScriptScreen(CloseScriptScreenPayload payload, IPayloadContext context) {
        if (MicroProcessorScriptScreen.instance != null) {
            MicroProcessorScriptScreen.instance.complete();
        }
    }


    public static void handlePlatesUpdate(PlatesUpdatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            BlockEntity blockEntity = context.player().level().getBlockEntity(payload.pos());
            if (blockEntity instanceof MicroProcessorBlockEntity be) {
                be.plates = payload.plates().copy();
            }
        });
    }
}
