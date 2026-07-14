package ru.terramain.microprocessor.block;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.registries.DeferredItem;
import ru.terramain.microprocessor.MicroProcessorMod;
import ru.terramain.microprocessor.MicroProcessorDataComponents;
import ru.terramain.microprocessor.plate.Plates;

import java.util.function.Consumer;

public class MicroProcessorItem extends BlockItem {
    public MicroProcessorItem(Properties properties) {
        super(MicroProcessorBlock.instance().get(), properties);
    }
    public MicroProcessorItem() {
        this(
                new Item.Properties()
                        .stacksTo(16)
                        .component(MicroProcessorDataComponents.PLATES.get(), Plates.Snapshot.by())
        );
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private MicroProcessorItemRenderer renderer;
            @Override public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.renderer == null) {
                    this.renderer = new MicroProcessorItemRenderer();
                }
                return this.renderer;
            }
        });
    }

    private static DeferredItem<MicroProcessorItem> inst = null;
    public static DeferredItem<MicroProcessorItem> instance() {
        if (inst == null) {
            inst = MicroProcessorMod.ITEMS.register("micro_processor", () -> new MicroProcessorItem());
        }
        return inst;
    }
}

