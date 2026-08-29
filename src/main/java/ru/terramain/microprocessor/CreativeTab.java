package ru.terramain.microprocessor;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import ru.terramain.microprocessor.block.MicroProcessorBlock;
import ru.terramain.microprocessor.deps.create.plates.PlateKinetic;
import ru.terramain.microprocessor.plate.plates.PlateDistributor;
import ru.terramain.microprocessor.plate.plates.PlateObserver;
import ru.terramain.microprocessor.plate.plates.PlatePiston;
import ru.terramain.microprocessor.plate.plates.PlateStickyPiston;

public class CreativeTab {
    public static void register() {
        MicroProcessorMod.CREATIVE_MODE_TABS.register("microprocessor_tab", () -> CreativeModeTab.builder()
                .icon(() -> new ItemStack(MicroProcessorBlock.instance()))
                .title(Component.translatable("creativetab.microprocessor"))
                .displayItems((itemDisplayParameters, output) -> {
                    output.accept(MicroProcessorBlock.instance());
                    output.accept(PlateObserver.instance().item);
                    output.accept(PlateDistributor.instance().item);
                    output.accept(PlatePiston.instance().item);
                    output.accept(PlateStickyPiston.instance().item);
                    output.accept(PlateKinetic.instance().item);
                })
                .build());
    }
}
