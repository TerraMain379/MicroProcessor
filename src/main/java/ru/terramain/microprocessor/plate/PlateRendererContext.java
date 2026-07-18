package ru.terramain.microprocessor.plate;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import ru.terramain.microprocessor.block.MicroProcessorBlockEntity;

public class PlateRendererContext {
    public float partialTick;
    public PoseStack poseStack;
    public MultiBufferSource bufferSource;
    public VertexConsumer vertexBuilder;
    public Direction direction;
    public BlockPos pos;
    public int packedLight;
    public int packedOverlay;

    public PlateRendererContext(float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, VertexConsumer vertexBuilder, Direction direction, BlockPos pos, int packedLight, int packedOverlay) {
        this.partialTick = partialTick;
        this.poseStack = poseStack;
        this.bufferSource = bufferSource;
        this.vertexBuilder = vertexBuilder;
        this.direction = direction;
        this.pos = pos;
        this.packedLight = packedLight;
        this.packedOverlay = packedOverlay;
    }
}
