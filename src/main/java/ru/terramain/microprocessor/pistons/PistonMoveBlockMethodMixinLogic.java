package ru.terramain.microprocessor.pistons;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.MovingPistonBlock;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.logging.log4j.util.InternalException;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;

public class PistonMoveBlockMethodMixinLogic {
    public static final ThreadLocal<Map<BlockPos, BlockEntity>> moveBlocks_ghostEntities = new ThreadLocal<>();
    public static final ThreadLocal<Level> moveBlocks_level = new ThreadLocal<>();


    public static void moveBlocks_start(Level level) {
        moveBlocks_ghostEntities.set(new HashMap<>());
        moveBlocks_level.set(level);
    }
    public static Object moveBlocks_put(Map map, Object k, Object v) {
        Object ret = map.put(k, v);

        BlockPos startPos = (BlockPos) k;
        BlockState originState = (BlockState) v;
        if (originState.hasBlockEntity()) {
            BlockEntity preGhostEntity = moveBlocks_level.get().getBlockEntity(startPos);
            if (preGhostEntity != null && originState.getBlock() instanceof EntityBlock entityBlock) {
                PistonBlockEntityBehaviour behaviour = PistonModifiersManager.getBehaviour(entityBlock);
                if (behaviour == null /* || !behaviour.movable(preGhostEntity) */) {
                    throw new InternalException("`behaviour == null` - in moveBlocks");
                }
                moveBlocks_ghostEntities.get().put(startPos, preGhostEntity);
                behaviour.onStartMovePre(preGhostEntity);
            }
        }
        return ret;
    }
    public static BlockEntity moveBlocks_newMovingBlockEntity(BlockPos pos, BlockState blockState, BlockState movedState, Direction direction, boolean extending, boolean isSourcePiston) {
        PistonMovingBlockEntity movingEntity = (PistonMovingBlockEntity) MovingPistonBlock.newMovingBlockEntity(pos, blockState, movedState, direction, extending, isSourcePiston);
        if (!isSourcePiston) {
            Direction moveDir = extending ? direction : direction.getOpposite();
            BlockPos startPos = pos.relative(moveDir.getOpposite());
            BlockEntity ghostEntity = moveBlocks_ghostEntities.get().get(startPos);
            if (ghostEntity != null) {
                PistonMovingBlockEntityDuck duck = (PistonMovingBlockEntityDuck) movingEntity;
                duck.setGhostMovingEntity(ghostEntity);

                PistonBlockEntityBehaviour behaviour = PistonModifiersManager.getBehaviour(ghostEntity);
                if (behaviour == null /* || !behaviour.movable(preGhostEntity) */) {
                    throw new InternalException("`behaviour == null` - in moveBlocks");
                }
                behaviour.onStartMoveIn(ghostEntity, new PistonMoveContext(movingEntity));
            }
        }
        return movingEntity;
    }

    public static void moveBlocks_return() {
        moveBlocks_ghostEntities.remove();
        moveBlocks_level.remove();
    }
}
