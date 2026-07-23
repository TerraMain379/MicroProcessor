package ru.terramain.microprocessor.block;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import ru.terramain.microprocessor.pistons.PistonBlockEntityBehaviour;
import ru.terramain.microprocessor.pistons.PistonModifiersManager;
import ru.terramain.microprocessor.pistons.PistonMoveContext;

public class MicroProcessorBlockPistonBehaviour implements PistonBlockEntityBehaviour {
    @Override public boolean movable(BlockEntity blockEntity) {
        MicroProcessorBlockEntity be = (MicroProcessorBlockEntity) blockEntity;
        return be.movable();
    }

    @Override
    public void onStartMovePre(BlockEntity blockEntity) {
        MicroProcessorBlockEntity be = (MicroProcessorBlockEntity) blockEntity;
        be.onStartPistonMovePre();
    }

    @Override public CompoundTag saveInMove(BlockEntity ghostEntity, PistonMoveContext context, boolean isCloseGame) {
        MicroProcessorBlockEntity be = (MicroProcessorBlockEntity) ghostEntity;
        be.markMovingSave();
        return PistonBlockEntityBehaviour.super.saveInMove(ghostEntity, context, isCloseGame);
    }

    @Override
    public void tick(BlockEntity ghostEntity, PistonMoveContext context) {
        MicroProcessorBlockEntity be = (MicroProcessorBlockEntity) ghostEntity;
        be.tickInPistonMove(context);
    }

    @Override
    public void onEndMovePost(BlockEntity blockEntity) {
        MicroProcessorBlockEntity be = (MicroProcessorBlockEntity) blockEntity;
        be.onEndPistonMovePost();
    }

    public static void register() {
        PistonModifiersManager.register(new MicroProcessorBlockPistonBehaviour(), MicroProcessorBlock.instance());
    }
}
