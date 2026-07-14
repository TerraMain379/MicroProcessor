package ru.terramain.microprocessor.plate;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public abstract class AbstractPlateItem<D extends PlateData, P extends Plate<D>> extends Item {
    public AbstractPlateItem(Properties properties) {
        super(properties);
    }

    public abstract P getPlate();

    public PlateState<D, P> createPlateState(ItemStack itemStack) {
        return new PlateState<>(getPlate().dataCodec.defaultData(), getPlate());
    }
    public ItemStack fromPlateState(PlateState<?, ?> plateState) {
        return new ItemStack(plateState.plate.item.get());
    }
}
