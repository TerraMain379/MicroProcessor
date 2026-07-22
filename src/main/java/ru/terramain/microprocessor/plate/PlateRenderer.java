package ru.terramain.microprocessor.plate;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import ru.terramain.microprocessor.MicroProcessorMod;

public interface PlateRenderer {
    ResourceLocation BASE_TEXTURE = ResourceLocation.fromNamespaceAndPath(MicroProcessorMod.MODID, "block/microprocessor");

    void renderPlate(PlateActionContext<?> plateContext, PlateRendererContext rendererContext);


    public default TextureAtlasSprite base() {
        return new Material(InventoryMenu.BLOCK_ATLAS, BASE_TEXTURE).sprite();
    }
    default void renderBase(PlateActionContext<?> plateContext, PlateRendererContext rendererContext) {
        TexturePlateRenderer.renderPlateTexture(
                new Material(InventoryMenu.BLOCK_ATLAS, BASE_TEXTURE).sprite(),
                rendererContext);
    }

    default void renderBase(PoseStack poseStack, VertexConsumer vertexBuilder, Direction face, BlockPos pos,
                            int packedLight, int packedOverlay) {
        renderBase(null, new PlateRendererContext(0f, poseStack, null, vertexBuilder, face, pos, packedLight, packedOverlay));
    }
}
