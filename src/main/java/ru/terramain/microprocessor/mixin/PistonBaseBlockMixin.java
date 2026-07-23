package ru.terramain.microprocessor.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.MovingPistonBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.logging.log4j.util.InternalException;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.terramain.microprocessor.pistons.*;

import java.util.HashMap;
import java.util.Map;

@Mixin(PistonBaseBlock.class)
public class PistonBaseBlockMixin {

    @Unique private static final ThreadLocal<Level> isPushable_level = new ThreadLocal<>();
    @Unique private static final ThreadLocal<BlockPos> isPushable_pos = new ThreadLocal<>();

    @Inject(method = "isPushable", at = @At(value = "HEAD"))
    private static void isPushable_start(BlockState state, Level level, BlockPos pos, Direction movementDirection, boolean allowDestroy, Direction pistonFacing, CallbackInfoReturnable<Boolean> cir) {
        isPushable_level.set(level);
        isPushable_pos.set(pos);
    }
    @Redirect(
            method = "isPushable",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;hasBlockEntity()Z")
    )
    private static boolean hasBlockEntity(BlockState $this) {
        if ($this.getBlock() instanceof EntityBlock entityBlock) {
            BlockEntity blockEntity = isPushable_level.get().getBlockEntity(isPushable_pos.get());
            PistonBlockEntityBehaviour behaviour = PistonModifiersManager.getBehaviour(entityBlock);
            if (behaviour != null) {
                return !behaviour.movable(blockEntity);
            }
            return true;
        }
        return false;
    }
    @Inject(method = "isPushable", at = @At(value = "RETURN"))
    private static void isPushable_end(BlockState state, Level level, BlockPos pos, Direction movementDirection, boolean allowDestroy, Direction pistonFacing, CallbackInfoReturnable<Boolean> cir) {
        isPushable_level.remove();
        isPushable_pos.remove();
    }



//    @Unique private static final ThreadLocal<Map<BlockPos, BlockEntity>> moveBlocks_ghostEntities = new ThreadLocal<>();
//    @Unique private static final ThreadLocal<Level> moveBlocks_level = new ThreadLocal<>();

    @Inject(method = "moveBlocks", at = @At(value = "HEAD"))
    private static void moveBlocks_start(Level level, BlockPos pos, Direction facing, boolean extending, CallbackInfoReturnable<Boolean> cir) {
        PistonMoveBlockMethodMixinLogic.moveBlocks_start(level);
    }
    @Redirect(
            method = "moveBlocks",
            at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
    )
    private Object moveBlocks_put(Map map, Object k, Object v) {
        return PistonMoveBlockMethodMixinLogic.moveBlocks_put(map, k, v);
    }
    @Redirect(
            method = "moveBlocks",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/piston/MovingPistonBlock;newMovingBlockEntity(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;ZZ)Lnet/minecraft/world/level/block/entity/BlockEntity;")
    )
    private static BlockEntity moveBlocks_newMovingBlockEntity(BlockPos pos, BlockState blockState, BlockState movedState, Direction direction, boolean extending, boolean isSourcePiston) {
        return PistonMoveBlockMethodMixinLogic.moveBlocks_newMovingBlockEntity(pos, blockState, movedState, direction, extending, isSourcePiston);
    }
    @Inject(method = "moveBlocks", at = @At(value = "RETURN"))
    private static void moveBlocks_return(Level level, BlockPos pos, Direction facing, boolean extending, CallbackInfoReturnable<Boolean> cir) {
        PistonMoveBlockMethodMixinLogic.moveBlocks_return();
    }
}
