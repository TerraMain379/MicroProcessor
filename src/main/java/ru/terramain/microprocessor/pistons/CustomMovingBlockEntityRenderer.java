package ru.terramain.microprocessor.pistons;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
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
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
public class CustomMovingBlockEntityRenderer implements BlockEntityRenderer<PistonMovingBlockEntity> {
    private final BlockRenderDispatcher blockDispatcher;
    private final BlockEntityRenderDispatcher blockEntityDispatcher;

    public CustomMovingBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.blockDispatcher = context.getBlockRenderDispatcher();
        this.blockEntityDispatcher = context.getBlockEntityRenderDispatcher();
    }

    @Override
    public void render(PistonMovingBlockEntity movingEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        Level level = movingEntity.getLevel();
        if (level == null) {
            return;
        }

        BlockPos renderPos = movingEntity.getBlockPos().relative(movingEntity.getMovementDirection().getOpposite());
        BlockState movedState = movingEntity.getMovedState();
        if (movedState.isAir()) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(movingEntity.getXOff(partialTick), movingEntity.getYOff(partialTick), movingEntity.getZOff(partialTick));

        float progress = movingEntity.getProgress(partialTick);
        // head block
        if (isAnimatedHead(movedState)) {
            movedState = movedState.setValue(BlockStateProperties.SHORT, progress <= 0.5F);
            renderBlock(renderPos, movedState, poseStack, bufferSource, level, false, packedOverlay);
        }
        // base block run this renderer (vanilla pistons)
        else if (movingEntity.isSourcePiston() && !movingEntity.isExtending()) {
            PistonType pistonType = movedState.is(Blocks.STICKY_PISTON) ? PistonType.STICKY : PistonType.DEFAULT;
            BlockState headState = Blocks.PISTON_HEAD
                    .defaultBlockState()
                    .setValue(PistonHeadBlock.TYPE, pistonType)
                    .setValue(PistonHeadBlock.FACING, movedState.getValue(PistonBaseBlock.FACING))
                    .setValue(BlockStateProperties.SHORT, progress >= 0.5F);
            renderBlock(renderPos, headState, poseStack, bufferSource, level, false, packedOverlay);

            BlockPos basePos = renderPos.relative(movingEntity.getMovementDirection());
            poseStack.popPose();
            poseStack.pushPose();
            movedState = movedState.setValue(PistonBaseBlock.EXTENDED, true);
            renderBlock(basePos, movedState, poseStack, bufferSource, level, true, packedOverlay);
        }
        else {
            PistonMovingBlockEntityDuck duck = (PistonMovingBlockEntityDuck) movingEntity;
            BlockEntity ghostEntity = duck.getGhostMovingEntity();
            if (ghostEntity != null) {
                renderBlockEntity(ghostEntity, partialTick, poseStack, bufferSource);
            }
            else renderBlock(renderPos, movedState, poseStack, bufferSource, level, false, packedOverlay);
        }

        poseStack.popPose();
        ModelBlockRenderer.clearCache();
    }

    private static boolean isAnimatedHead(BlockState state) {
        return state.is(Blocks.PISTON_HEAD) || state.is(MicroProcessorPistonHeadBlock.instance().get());
    }

    private void renderBlock(BlockPos pos, BlockState state, PoseStack poseStack, MultiBufferSource bufferSource, Level level, boolean checkSides, int packedOverlay) {
        ClientHooks.renderPistonMovedBlocks(pos, state, poseStack, bufferSource, level, checkSides, packedOverlay, blockDispatcher);
    }
    private void renderBlockEntity(BlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource) {
        this.blockEntityDispatcher.render(blockEntity, partialTick, poseStack, bufferSource);
    }

    @Override
    public int getViewDistance() {
        return 68;
    }

    @Override
    public @NotNull AABB getRenderBoundingBox(PistonMovingBlockEntity blockEntity) {
        return AABB.INFINITE;
    }
}
