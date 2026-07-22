package ru.terramain.microprocessor.plate.plates.piston;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.PistonType;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.ClientHooks;

@OnlyIn(Dist.CLIENT)
public class MicroProcessorPistonHeadRenderer implements BlockEntityRenderer<PistonMovingBlockEntity> {
    private final BlockRenderDispatcher blockRenderer;

    public MicroProcessorPistonHeadRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(PistonMovingBlockEntity piston, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        Level level = piston.getLevel();
        if (level == null) {
            return;
        }

        BlockPos renderPos = piston.getBlockPos().relative(piston.getMovementDirection().getOpposite());
        BlockState movedState = piston.getMovedState();
        if (movedState.isAir()) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(piston.getXOff(partialTick), piston.getYOff(partialTick), piston.getZOff(partialTick));

        float progress = piston.getProgress(partialTick);
        if (isAnimatedHead(movedState)) {
            movedState = movedState.setValue(BlockStateProperties.SHORT, progress <= 0.5F);
            renderBlock(renderPos, movedState, poseStack, bufferSource, level, false, packedOverlay);
        } else if (piston.isSourcePiston() && !piston.isExtending()) {
            PistonType pistonType = movedState.is(Blocks.STICKY_PISTON) ? PistonType.STICKY : PistonType.DEFAULT;
            BlockState headState = Blocks.PISTON_HEAD
                    .defaultBlockState()
                    .setValue(PistonHeadBlock.TYPE, pistonType)
                    .setValue(PistonHeadBlock.FACING, movedState.getValue(PistonBaseBlock.FACING))
                    .setValue(BlockStateProperties.SHORT, progress <= 0.5F);
            renderBlock(renderPos, headState, poseStack, bufferSource, level, false, packedOverlay);

            BlockPos basePos = renderPos.relative(piston.getMovementDirection());
            poseStack.popPose();
            poseStack.pushPose();
            movedState = movedState.setValue(PistonBaseBlock.EXTENDED, true);
            renderBlock(basePos, movedState, poseStack, bufferSource, level, true, packedOverlay);
        } else {
            renderBlock(renderPos, movedState, poseStack, bufferSource, level, false, packedOverlay);
        }

        poseStack.popPose();
    }

    private static boolean isAnimatedHead(BlockState state) {
        return state.is(Blocks.PISTON_HEAD) || state.is(MicroProcessorPistonHeadBlock.instance().get());
    }

    private void renderBlock(
            BlockPos pos,
            BlockState state,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            Level level,
            boolean checkSides,
            int packedOverlay
    ) {
        ClientHooks.renderPistonMovedBlocks(pos, state, poseStack, bufferSource, level, checkSides, packedOverlay, blockRenderer);
    }

    @Override
    public int getViewDistance() {
        return 68;
    }

    @Override
    public AABB getRenderBoundingBox(PistonMovingBlockEntity blockEntity) {
        return AABB.INFINITE;
    }
}
