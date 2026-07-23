package ru.terramain.microprocessor.pistons;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;

public class PistonMoveContext {
    public BlockPos startPos;
    public BlockPos endPos;
    public Direction moveDirection;
    public int progress;

    public PistonMoveContext(BlockPos startPos, BlockPos endPos, Direction moveDirection, int progress) {
        this.startPos = startPos;
        this.endPos = endPos;
        this.moveDirection = moveDirection;
        this.progress = progress;
    }
    public PistonMoveContext(PistonMovingBlockEntity movingBlockEntity) {
        this(
            movingBlockEntity.getBlockPos().relative(movingBlockEntity.getDirection().getOpposite()),
            movingBlockEntity.getBlockPos(),
            movingBlockEntity.getDirection(),
            (int) (movingBlockEntity.getProgress(0) * 2)
        );
    }
}
