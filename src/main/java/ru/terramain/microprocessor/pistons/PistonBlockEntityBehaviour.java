package ru.terramain.microprocessor.pistons;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;

public interface PistonBlockEntityBehaviour {
    default boolean movable(BlockEntity blockEntity) {
        return true;
    }

    default void onStartMovePre(BlockEntity blockEntity) { } // in world
    default void onStartMoveIn(BlockEntity ghostEntity, PistonMoveContext context) { } // in moving-entity
    default void onEndMoveIn(BlockEntity ghostMovingEntity, PistonMoveContext context) { } // in moving-entity
    default void onEndMovePost(BlockEntity blockEntity) { } // in world
    default void tick(BlockEntity ghostEntity, PistonMoveContext context) { } // in moving-entity

    default CompoundTag saveInMove(BlockEntity ghostEntity, PistonMoveContext context, boolean isCloseGame) {
        return ghostEntity.saveWithoutMetadata(ghostEntity.getLevel().registryAccess());
    }
    default void loadInMove(BlockEntity ghostEntity, CompoundTag tag, PistonMoveContext context, boolean isCloseGame) {
        ghostEntity.loadWithComponents(tag, ghostEntity.getLevel().registryAccess());
    }
}
