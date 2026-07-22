package ru.terramain.microprocessor.plate.plates;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import ru.terramain.microprocessor.MicroProcessorMod;
import ru.terramain.microprocessor.plate.*;

public class NullPlate extends Plate<NullPlate.Data> {
    protected NullPlate() {
        super(null, null, null, renderer, null);
    }
    public static class Data implements PlateData {
        @Override
        public <D extends PlateData> D copy() {
            return (D) new Data();
        }
    }
    public static class Renderer implements TexturePlateRenderer {
        public static final ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(MicroProcessorMod.MODID, "block/microprocessor_null");

        @Override public TextureAtlasSprite sprite(PlateActionContext<?> context) {
            return new Material(InventoryMenu.BLOCK_ATLAS, texture).sprite();
        }
    };
    public static Renderer renderer = new Renderer();

    private static NullPlate inst = null;
    public static NullPlate instance() {
        if (inst == null) {
            inst = PlateRegister.register(new NullPlate());
        }
        return inst;
    }
    public static final PlateState<Data, NullPlate> PLATE_STATE = new PlateState<>(null, instance());
}
