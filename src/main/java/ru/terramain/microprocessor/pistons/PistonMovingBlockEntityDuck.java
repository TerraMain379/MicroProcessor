package ru.terramain.microprocessor.pistons;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Unique;

public interface PistonMovingBlockEntityDuck {
    @Unique BlockEntity getGhostMovingEntity();
    @Unique void setGhostMovingEntity(BlockEntity blockEntity);

//    @Unique BlockPos getStartPos();
//    @Unique void setStartPos(BlockPos blockPos);
}
