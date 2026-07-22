package ru.terramain.microprocessor;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

import org.slf4j.Logger;

import ru.terramain.microprocessor.bcbui.EditorFonts;
import ru.terramain.microprocessor.block.*;
import ru.terramain.microprocessor.plate.plates.piston.MicroProcessorPistonHeadRenderer;
import ru.terramain.microprocessor.deps.Integration;
import ru.terramain.microprocessor.plate.plates.*;
import ru.terramain.microprocessor.plate.plates.piston.MicroProcessorPistonHeadBlock;
import ru.terramain.microprocessor.plate.plates.PlatePiston;
import ru.terramain.microprocessor.plate.plates.PlateStickyPiston;

@Mod(MicroProcessorMod.MODID)
public class MicroProcessorMod {
    public static final String MODID = "microprocessor";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, MicroProcessorMod.MODID);


    public MicroProcessorMod(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        NeoForge.EVENT_BUS.register(this);
        registerAll(modEventBus);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }
    public void registerAll(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        DATA_COMPONENT_TYPES.register(modEventBus);
        MicroProcessorDataComponents.register();
        MicroProcessorBlockEventsManager.registerDefault();

        MicroProcessorBlock.instance();
        MicroProcessorBlockEntity.instance();
        MicroProcessorItem.instance();
        MicroProcessorPistonHeadBlock.instance();

        NullPlate.instance();
        PlateObserver.instance();
        PlateDistributor.instance();
        PlatePiston.instance();
        PlateStickyPiston.instance();

        Integration.registerEvent(modEventBus);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        Integration.setupEvent(event);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("server starting");
    }

    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("client starting");
            LOGGER.info("name >> {}", Minecraft.getInstance().getUser().getName());
        }

        @SubscribeEvent
        public static void onRegisterClientReloadListeners(RegisterClientReloadListenersEvent event) {
            event.registerReloadListener(new PreparableReloadListener() {
                @Override
                public java.util.concurrent.CompletableFuture<Void> reload(
                        PreparationBarrier barrier,
                        ResourceManager resourceManager,
                        ProfilerFiller preparationsProfiler,
                        ProfilerFiller reloadProfiler,
                        java.util.concurrent.Executor backgroundExecutor,
                        java.util.concurrent.Executor gameExecutor
                ) {
                    return barrier.wait(null).thenRunAsync(EditorFonts::invalidate, gameExecutor);
                }
            });
        }

        @SubscribeEvent
        public static void registerBER(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(MicroProcessorBlockEntity.instance().get(), MicroProcessorRenderer::new);
            event.registerBlockEntityRenderer(BlockEntityType.PISTON, MicroProcessorPistonHeadRenderer::new);
        }
    }
}
