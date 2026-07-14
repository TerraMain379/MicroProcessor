package ru.terramain.microprocessor.plate;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import ru.terramain.microprocessor.MicroProcessorMod;

public interface PlateRenderer {
    public static final ResourceLocation BASE_TEXTURE = ResourceLocation.fromNamespaceAndPath(MicroProcessorMod.MODID, "block/microprocessor");

    void renderPlate(PlateActionContext<?> context, PoseStack poseStack, VertexConsumer vertexBuilder,
                                Direction face, BlockPos pos, int packedLight, int packedOverlay);

    default void renderBase(PoseStack poseStack, VertexConsumer vertexBuilder,
                                  Direction face, BlockPos pos, int packedLight, int packedOverlay) {
        TexturePlateRenderer.renderPlateTexture(
                new Material(InventoryMenu.BLOCK_ATLAS, BASE_TEXTURE).sprite(),
                poseStack, vertexBuilder, face, pos, packedLight, packedOverlay);
    }
}
