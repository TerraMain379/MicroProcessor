package ru.terramain.microprocessor.plate.plates.piston;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.MovingPistonBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.PistonType;
import net.minecraft.world.level.block.entity.BlockEntity;
import ru.terramain.microprocessor.block.MicroProcessorBlockEntity;
import ru.terramain.microprocessor.plate.PlateState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlatePistonLogic {
    public static boolean startSpread(MicroProcessorBlockEntity be, Direction direction) {
//        PistonBaseBlock
        Level level = be.getLevel();
        PlateState<?, ?> plateState = be.getPlateState(direction);
        if (plateState.plate instanceof AbstractPlatePiston abstractPlatePiston) {
            AbstractPlatePiston plate = (AbstractPlatePiston) plateState.plate;
            AbstractPlatePiston.Data data = (AbstractPlatePiston.Data) plateState.data;
            if (data.moveDelta > 0) return false;
            if (data.isSpread) return false;

            return moveBlocks(
                    be.getLevel(),
                    MicroProcessorPistonHeadBlock.instance().get(), // TODO: replace to custom head
                    be.getBlockPos(),
                    direction,
                    true,
                    false
            );
        }
        return false;
    }
    public static PistonStructureResolver resolver(MicroProcessorBlockEntity be, Direction direction, boolean extending) {
        return new PistonStructureResolver(be.getLevel(), be.getBlockPos(), direction, extending);
    }

    /**
     * copy by {@link PistonBaseBlock#moveBlocks(Level, BlockPos, Direction, boolean)}.
     */
    public static boolean moveBlocks(Level level, Block headBlock, BlockPos pistonPos, Direction pistonFacing, boolean isExtending, boolean isSticky) {
        BlockPos headPos = pistonPos.relative(pistonFacing);
        if (!isExtending && level.getBlockState(headPos).is(headBlock)) {
            level.setBlock(headPos, Blocks.AIR.defaultBlockState(), 20);
        }

        PistonStructureResolver structureResolver = new PistonStructureResolver(level, pistonPos, pistonFacing, isExtending);
        if (!structureResolver.resolve()) {
            return false;
        }

        Map<BlockPos, BlockState> blocksToClear = new HashMap<>();
        List<BlockPos> blocksToPush = structureResolver.getToPush();
        List<BlockState> originalStates = new ArrayList<>();

        // Сохраняем исходные state всех блоков, которые будут двигаться
        for (BlockPos pushBlockPos : blocksToPush) {
            BlockState blockState = level.getBlockState(pushBlockPos);
            originalStates.add(blockState);
            blocksToClear.put(pushBlockPos, blockState);
        }

        List<BlockPos> blocksToDestroy = structureResolver.getToDestroy();
        BlockState[] statesForNeighborUpdates = new BlockState[blocksToPush.size() + blocksToDestroy.size()];
        Direction moveDirection = isExtending ? pistonFacing : pistonFacing.getOpposite();
        int neighborUpdateIndex = 0;

        // Ломаем блоки с PushReaction.DESTROY (с конца списка)
        for (int destroyIndex = blocksToDestroy.size() - 1; destroyIndex >= 0; destroyIndex--) {
            BlockPos destroyPos = blocksToDestroy.get(destroyIndex);
            BlockState destroyState = level.getBlockState(destroyPos);
            BlockEntity blockEntity = destroyState.hasBlockEntity() ? level.getBlockEntity(destroyPos) : null;
            Block.dropResources(destroyState, level, destroyPos, blockEntity);
            destroyState.onDestroyedByPushReaction(level, destroyPos, moveDirection, level.getFluidState(destroyPos));
            if (!destroyState.is(BlockTags.FIRE)) {
                level.addDestroyBlockEffect(destroyPos, destroyState);
            }

            statesForNeighborUpdates[neighborUpdateIndex++] = destroyState;
        }

        // Ставим moving_piston на новые позиции (с дальнего конца цепочки, чтобы не перезаписать блоки)
        for (int pushIndex = blocksToPush.size() - 1; pushIndex >= 0; pushIndex--) {
            BlockPos movingPos = blocksToPush.get(pushIndex);
            BlockState oldStateAtOrigin = level.getBlockState(movingPos);
            BlockPos newPos = movingPos.relative(moveDirection);
            blocksToClear.remove(newPos);
            BlockState originBlockState = originalStates.get(pushIndex);
            BlockState movingPistonState = Blocks.MOVING_PISTON.defaultBlockState().setValue(MovingPistonBlock.FACING, pistonFacing);
            level.setBlock(newPos, movingPistonState, 68);
            level.setBlockEntity(MovingPistonBlock.newMovingBlockEntity(
                    newPos, movingPistonState, originBlockState, pistonFacing, isExtending, false
            ));
            statesForNeighborUpdates[neighborUpdateIndex++] = oldStateAtOrigin;
        }

        if (isExtending) {
            PistonType pistonHeadType = isSticky ? PistonType.STICKY : PistonType.DEFAULT;
            BlockState pistonHeadState = headBlock
                    .defaultBlockState()
                    .setValue(MicroProcessorPistonHeadBlock.FACING, pistonFacing)
                    .setValue(MicroProcessorPistonHeadBlock.TYPE, pistonHeadType);
            BlockState headMovingPistonState = Blocks.MOVING_PISTON
                    .defaultBlockState()
                    .setValue(MovingPistonBlock.FACING, pistonFacing)
                    .setValue(MovingPistonBlock.TYPE, isSticky ? PistonType.STICKY : PistonType.DEFAULT);
            blocksToClear.remove(headPos);
            level.setBlock(headPos, headMovingPistonState, 68);
            level.setBlockEntity(MovingPistonBlock.newMovingBlockEntity(
                    headPos, headMovingPistonState, pistonHeadState, pistonFacing, true, true
            ));
        }

        BlockState airState = Blocks.AIR.defaultBlockState();

        // Очищаем исходные клетки, откуда блоки уехали
        for (BlockPos clearPos : blocksToClear.keySet()) {
            level.setBlock(clearPos, airState, 82);
        }

        // Обновляем формы соседей после очистки
        for (Map.Entry<BlockPos, BlockState> clearedEntry : blocksToClear.entrySet()) {
            BlockPos clearedPos = clearedEntry.getKey();
            BlockState oldStateBeforeMove = clearedEntry.getValue();
            oldStateBeforeMove.updateIndirectNeighbourShapes(level, clearedPos, 2);
            airState.updateNeighbourShapes(level, clearedPos, 2);
            airState.updateIndirectNeighbourShapes(level, clearedPos, 2);
        }

        neighborUpdateIndex = 0;

        // Redstone/neighbor update для уничтоженных блоков
        for (int destroyedUpdateIndex = blocksToDestroy.size() - 1; destroyedUpdateIndex >= 0; destroyedUpdateIndex--) {
            BlockState destroyedState = statesForNeighborUpdates[neighborUpdateIndex++];
            BlockPos destroyedPos = blocksToDestroy.get(destroyedUpdateIndex);
            destroyedState.updateIndirectNeighbourShapes(level, destroyedPos, 2);
            level.updateNeighborsAt(destroyedPos, destroyedState.getBlock());
        }

        // Redstone/neighbor update для сдвинутых блоков (по старым позициям)
        for (int pushedUpdateIndex = blocksToPush.size() - 1; pushedUpdateIndex >= 0; pushedUpdateIndex--) {
            level.updateNeighborsAt(blocksToPush.get(pushedUpdateIndex), statesForNeighborUpdates[neighborUpdateIndex++].getBlock());
        }

        if (isExtending) {
            level.updateNeighborsAt(headPos, headBlock);
        }

        return true;
    }
}
