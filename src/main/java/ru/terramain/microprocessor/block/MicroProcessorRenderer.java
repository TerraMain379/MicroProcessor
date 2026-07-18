package ru.terramain.microprocessor.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.Level;
import ru.terramain.microprocessor.MicroProcessorMod;
import ru.terramain.microprocessor.logic.MicroProcessorContext;
import ru.terramain.microprocessor.plate.PlateActionContext;
import ru.terramain.microprocessor.plate.PlateRenderer;
import ru.terramain.microprocessor.plate.PlateRendererContext;
import ru.terramain.microprocessor.plate.PlateState;
import ru.terramain.microprocessor.plate.plates.NullPlate;

/**
 * Рендерер для блока микропроцессора.
 * Отрисовывает базовую текстуру блока и пластины на каждой грани.
 */
public class MicroProcessorRenderer implements BlockEntityRenderer<MicroProcessorBlockEntity> {

    // Базовый материал для граней без пластин
    private static final Material BASE_MATERIAL = new Material(
            InventoryMenu.BLOCK_ATLAS,
            ResourceLocation.fromNamespaceAndPath(MicroProcessorMod.MODID, "block/micro_processor")
    );

    public MicroProcessorRenderer(BlockEntityRendererProvider.Context context) { }

    /**
     * Основной метод рендеринга блока микропроцессора.
     * Отрисовывает все 6 граней блока с соответствующими текстурами.
     */
    @Override
    public void render(MicroProcessorBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        Level level = be.getLevel();
        boolean itemRender = level == null;

        BlockPos pos = be.getBlockPos();
        VertexConsumer vertexBuilder = bufferSource.getBuffer(RenderType.entityCutout(InventoryMenu.BLOCK_ATLAS));

        // Отрисовываем каждую грань блока
        for (Direction direction : Direction.values()) {
            PlateState<?, ?> plateState = be.getPlateState(direction);
            PlateRenderer plateRenderer;
            if (plateState != null) plateRenderer = plateState.plate.renderer;
            else plateRenderer = NullPlate.renderer;

            int newPackedLight;
            if (itemRender) {
                newPackedLight = packedLight;
            }
            else {
                BlockPos relativeBlockPos = be.getBlockPos().relative(direction);
                newPackedLight = LevelRenderer.getLightColor(level, relativeBlockPos);
            }

            MicroProcessorContext context1 = new MicroProcessorContext(be);
            PlateActionContext<?> context2 = new PlateActionContext<>(plateState, direction, context1);
            PlateRendererContext rendererContext = new PlateRendererContext(
                    partialTick, poseStack, bufferSource, vertexBuilder,
                    direction, pos,
                    newPackedLight, packedOverlay
            );
            plateRenderer.renderPlate(context2, rendererContext);

//            plateRenderer.renderShaft(context2, partialTick, poseStack, bufferSource, direction, pos, newPackedLight, packedOverlay);
        }
    }

}
