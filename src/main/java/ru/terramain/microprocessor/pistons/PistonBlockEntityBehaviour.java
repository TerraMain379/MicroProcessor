package ru.terramain.microprocessor.pistons;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
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

    default CompoundTag saveInMove(BlockEntity ghostEntity, HolderLookup.Provider provider, PistonMoveContext context, boolean isCloseGame) {
        return ghostEntity.saveWithoutMetadata(provider);
    }
    default CompoundTag saveInMove(BlockEntity ghostEntity, RegistryAccess registryAccess, PistonMoveContext context, boolean isCloseGame) {
        return ghostEntity.saveWithoutMetadata(registryAccess);
    }
    default void loadInMove(BlockEntity ghostEntity, CompoundTag tag, HolderLookup.Provider provider, PistonMoveContext context, boolean isCloseGame) {
        ghostEntity.loadWithComponents(tag, provider);
    }
    default void loadInMove(BlockEntity ghostEntity, CompoundTag tag, RegistryAccess registryAccess, PistonMoveContext context, boolean isCloseGame) {
        ghostEntity.loadWithComponents(tag, registryAccess);
    }
}
