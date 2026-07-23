package ru.terramain.microprocessor.pistons;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.piston.MovingPistonBlock;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.PistonType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.registries.DeferredBlock;
import org.apache.logging.log4j.util.InternalException;
import ru.terramain.microprocessor.block.MicroProcessorBlockEntity;
import ru.terramain.microprocessor.block.MicroProcessorBlockEventsManager;
import ru.terramain.microprocessor.plate.PlateState;
import ru.terramain.microprocessor.plate.plates.AbstractPlatePiston;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlatePistonLogic {
    public static final int DELTA_TIME = 3;
    public static final DeferredBlock<?> HEAD_BLOCK = MicroProcessorPistonHeadBlock.instance();

    public static void sendHandlingSignal(MicroProcessorBlockEntity be, Direction direction, boolean isPowered) {
        MicroProcessorBlockEventsManager.<Direction>triggerBlockEvent(
                isPowered ? AbstractPlatePiston.BLOCK_EVENT_POWER_ON : AbstractPlatePiston.BLOCK_EVENT_POWER_OFF,
                be,
                direction
        );
    }

    // blocks moved - true
    // no changes or internal plate changes - false
    public static boolean handleSignal(MicroProcessorBlockEntity be, Direction direction, boolean isPowered) {
        Level level = be.getLevel();
        BlockPos pos = be.getBlockPos();
        if (level == null) return false;

        PlateState<?, ?> plateState = be.getPlateState(direction);
        if (plateState == null || !(plateState.plate instanceof AbstractPlatePiston plate)) return false;

        AbstractPlatePiston.Data data = (AbstractPlatePiston.Data) plateState.data;
        boolean isMoved;

        if (!be.getLevel().isClientSide) isMoved = handleSignalServer(be, pos, direction, data, plate.isSticky, isPowered);
        else isMoved = handleSignalClient(be, pos, direction, data, plate.isSticky, isPowered);

        be.core.worker.dataPool.pushS2WMessage(new AbstractPlatePiston.SetPoweredResultEventMessage(direction, isMoved));
        return isMoved;
    }

    public static boolean handleSignalServer(MicroProcessorBlockEntity be, BlockPos pos, Direction direction, AbstractPlatePiston.Data data, boolean isSticky, boolean isPowered) {
        Level level = be.getLevel();

        data.isPowered = isPowered;
        be.setChanged();
        if (data.isSpread == isPowered) {
            return false;
        }

        if (data.moveDelta > 0) {
            if (isPowered) return false;
            else return finalSpread(be, pos, direction, data, isSticky);
        }

        boolean result = moveBlocks(level, pos, direction, isPowered, isSticky);
        if (!isPowered || result) {
            data.moveDelta = DELTA_TIME;
            data.isSpread = isPowered;
            be.setChanged();
        }
        return result;
    }


    public static boolean handleSignalClient(MicroProcessorBlockEntity be, BlockPos pos, Direction direction, AbstractPlatePiston.Data data, boolean isSticky, boolean isPowered) {
        if (!isPowered) {
            boolean wasInMove = finalSpread(be, pos, direction, data, isSticky);
            if (wasInMove) return true;
        }
        return moveBlocks(be.getLevel(), pos, direction, isPowered, isSticky);
    }

    public static boolean finalSpread(MicroProcessorBlockEntity be, BlockPos pos, Direction direction, AbstractPlatePiston.Data data, boolean isSticky) {
        // server-assert data.moveDelta > 0 && data.isSpread;
        Level level = be.getLevel();
        BlockPos headPos = pos.relative(direction);

        boolean hasMovingHead;
        {
            BlockState blockState = level.getBlockState(headPos);
            if (blockState.is(Blocks.MOVING_PISTON) && blockState.getValue(MovingPistonBlock.FACING) == direction) {
                PistonMovingBlockEntity mbe = (PistonMovingBlockEntity) level.getBlockEntity(headPos);
                if (mbe != null) {
                    mbe.finalTick();
                }
                level.setBlock(headPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_MOVE_BY_PISTON | Block.UPDATE_CLIENTS);
                hasMovingHead = true;
            }
            else hasMovingHead = false;
        }

        if (hasMovingHead) {
            data.moveDelta = DELTA_TIME;
            data.isSpread = false;
            be.setChanged();
        }
        else return false;

        {
            BlockPos movingBlockPos = pos.relative(direction, 2);
            BlockState blockState = level.getBlockState(movingBlockPos);
            if (blockState.is(Blocks.MOVING_PISTON) && blockState.getValue(MovingPistonBlock.FACING) == direction) {
                PistonMovingBlockEntity mbe = (PistonMovingBlockEntity) level.getBlockEntity(movingBlockPos);
                mbe.finalTick();
            }
            else {
                moveBlocks(level, pos, direction, false, isSticky);
            }
        }
        return true;
    }
    public static boolean moveBlocks(Level level, BlockPos pistonPos, Direction pistonFacing, boolean isExtending, boolean isSticky) {
        // by piston modifiers manager
        PistonMoveBlockMethodMixinLogic.moveBlocks_start(level);

        BlockPos headPos = pistonPos.relative(pistonFacing);
        Direction moveDirection = isExtending ? pistonFacing : pistonFacing.getOpposite();
        if (!isExtending && level.getBlockState(headPos).is(HEAD_BLOCK)) {
            level.setBlock(headPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_MOVE_BY_PISTON | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_INVISIBLE);
        }

        PistonStructureResolver structureResolver = new PistonStructureResolver(level, pistonPos, pistonFacing, isExtending);
        if (!structureResolver.resolve()) {
            return false;
        }

        if (!isExtending && !isSticky) {
            return true;
        }

        Map<BlockPos, BlockState> blocksToClear = new HashMap<>();
        List<BlockPos> blocksToPush = structureResolver.getToPush();
        List<BlockPos> blocksToDestroy = structureResolver.getToDestroy();
        List<BlockState> originalStates = new ArrayList<>();
        BlockState[] statesForNeighborUpdates = new BlockState[blocksToPush.size() + blocksToDestroy.size()];
        int neighborUpdateIndex = 0;

        // Save original states all moving blocks
        for (BlockPos pushBlockPos : blocksToPush) {
            BlockState blockState = level.getBlockState(pushBlockPos);
            originalStates.add(blockState);
            // by piston modifiers manager
            PistonMoveBlockMethodMixinLogic.moveBlocks_put(blocksToClear, pushBlockPos, blockState);
            // blocksToClear.put(pushBlockPos, blockState);
        }

        // break blocks with PushReaction.DESTROY
        for (int destroyIndex = blocksToDestroy.size() - 1; destroyIndex >= 0; destroyIndex--) {
            BlockPos pos = blocksToDestroy.get(destroyIndex);
            BlockState state = level.getBlockState(pos);
            BlockEntity blockEntity = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;

            Block.dropResources(state, level, pos, blockEntity);
            state.onDestroyedByPushReaction(level, pos, moveDirection, level.getFluidState(pos));

            if (!state.is(BlockTags.FIRE)) {
                level.addDestroyBlockEffect(pos, state);
            }

            statesForNeighborUpdates[neighborUpdateIndex++] = state;
        }

        // placing MOVING_PISTON to new positions
        for (int pushIndex = blocksToPush.size() - 1; pushIndex >= 0; pushIndex--) {
            BlockPos movingPos = blocksToPush.get(pushIndex);
            BlockState stateAfterReaction = level.getBlockState(movingPos);
            BlockPos newPos = movingPos.relative(moveDirection);
            blocksToClear.remove(newPos);

            BlockState originBlockState = originalStates.get(pushIndex);
            BlockState movingPistonState = Blocks.MOVING_PISTON.defaultBlockState().setValue(MovingPistonBlock.FACING, pistonFacing);
            level.setBlock(newPos, movingPistonState, Block.UPDATE_MOVE_BY_PISTON | Block.UPDATE_INVISIBLE);
            // by piston modifiers manager
            PistonMovingBlockEntity movingBlockEntity = (PistonMovingBlockEntity) PistonMoveBlockMethodMixinLogic.moveBlocks_newMovingBlockEntity(
                    newPos, movingPistonState, originBlockState, pistonFacing, isExtending, false
            );
            // PistonMovingBlockEntity movingBlockEntity = (PistonMovingBlockEntity) MovingPistonBlock.newMovingBlockEntity(
            //         newPos, movingPistonState, originBlockState, pistonFacing, isExtending, false
            // );
            level.setBlockEntity(movingBlockEntity);
            statesForNeighborUpdates[neighborUpdateIndex++] = stateAfterReaction;
        }

        // placing MOVING_BLOCK by PISTON_HEAD
        if (isExtending) {
            PistonType pistonHeadType = isSticky ? PistonType.STICKY : PistonType.DEFAULT;
            BlockState pistonHeadState = HEAD_BLOCK.get()
                    .defaultBlockState()
                    .setValue(MicroProcessorPistonHeadBlock.FACING, pistonFacing)
                    .setValue(MicroProcessorPistonHeadBlock.TYPE, pistonHeadType);
            BlockState headMovingPistonState = Blocks.MOVING_PISTON
                    .defaultBlockState()
                    .setValue(MovingPistonBlock.FACING, pistonFacing)
                    .setValue(MovingPistonBlock.TYPE, isSticky ? PistonType.STICKY : PistonType.DEFAULT);
            blocksToClear.remove(headPos);
            level.setBlock(headPos, headMovingPistonState, Block.UPDATE_MOVE_BY_PISTON | Block.UPDATE_CLIENTS);
            level.setBlockEntity(MovingPistonBlock.newMovingBlockEntity(
                    headPos, headMovingPistonState, pistonHeadState, pistonFacing, true, true
            ));
        }

        BlockState airState = Blocks.AIR.defaultBlockState();

        // Очищаем исходные клетки, откуда блоки уехали
        for (BlockPos clearPos : blocksToClear.keySet()) {
            level.setBlock(clearPos, airState, Block.UPDATE_MOVE_BY_PISTON | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_CLIENTS);
        }

        // Обновляем формы соседей после очистки
        for (Map.Entry<BlockPos, BlockState> clearedEntry : blocksToClear.entrySet()) {
            BlockPos clearedPos = clearedEntry.getKey();
            BlockState oldStateBeforeMove = clearedEntry.getValue();
            oldStateBeforeMove.updateIndirectNeighbourShapes(level, clearedPos, Block.UPDATE_CLIENTS);
            airState.updateNeighbourShapes(level, clearedPos, Block.UPDATE_CLIENTS);
            airState.updateIndirectNeighbourShapes(level, clearedPos, Block.UPDATE_CLIENTS);
        }

        neighborUpdateIndex = 0;

        // Redstone/neighbor update для уничтоженных блоков
        for (int destroyedUpdateIndex = blocksToDestroy.size() - 1; destroyedUpdateIndex >= 0; destroyedUpdateIndex--) {
            BlockState destroyedState = statesForNeighborUpdates[neighborUpdateIndex++];
            BlockPos destroyedPos = blocksToDestroy.get(destroyedUpdateIndex);
            destroyedState.updateIndirectNeighbourShapes(level, destroyedPos, Block.UPDATE_CLIENTS);
            level.updateNeighborsAt(destroyedPos, destroyedState.getBlock());
        }

        // Redstone/neighbor update для сдвинутых блоков (по старым позициям)
        for (int pushedUpdateIndex = blocksToPush.size() - 1; pushedUpdateIndex >= 0; pushedUpdateIndex--) {
            level.updateNeighborsAt(blocksToPush.get(pushedUpdateIndex), statesForNeighborUpdates[neighborUpdateIndex++].getBlock());
        }

        if (isExtending) {
            level.updateNeighborsAt(headPos, HEAD_BLOCK.get());
        }

        // by piston modifiers manager
        PistonMoveBlockMethodMixinLogic.moveBlocks_return();

        return true;
    }

}
