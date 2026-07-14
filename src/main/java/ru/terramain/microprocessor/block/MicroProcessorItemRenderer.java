package ru.terramain.microprocessor.block;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class MicroProcessorItemRenderer extends BlockEntityWithoutLevelRenderer {
    private MicroProcessorBlockEntity dummyBlockEntity;
    private final BlockEntityRenderDispatcher dispatcher;

    public MicroProcessorItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        this.dispatcher = Minecraft.getInstance().getBlockEntityRenderDispatcher();
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        // Ленивая инициализация фиктивного блока
        if (this.dummyBlockEntity == null) {
            BlockState defaultState = MicroProcessorBlock.instance().get().defaultBlockState();
            this.dummyBlockEntity = new MicroProcessorBlockEntity(BlockPos.ZERO, defaultState);
        }

        // КРИТИЧЕСКИ ВАЖНО: Копируем дата-компоненты из ItemStack в наш фиктивный блок перед рендером!
        // Это заставит ваш BER "видеть" установленные пластины прямо на иконке в инвентаре!
        this.dummyBlockEntity.applyComponentsFromItemStack(stack);

        // Перенаправляем отрисовку в ваш существующий BlockEntityRenderer
        this.dispatcher.renderItem(this.dummyBlockEntity, poseStack, bufferSource, packedLight, packedOverlay);
    }
}
