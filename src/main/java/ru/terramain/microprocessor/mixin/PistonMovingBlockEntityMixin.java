package ru.terramain.microprocessor.mixin;

import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.MovingPistonBlock;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.terramain.microprocessor.pistons.PistonBlockEntityBehaviour;
import ru.terramain.microprocessor.pistons.PistonModifiersManager;
import ru.terramain.microprocessor.pistons.PistonMoveContext;
import ru.terramain.microprocessor.pistons.PistonMovingBlockEntityDuck;


@Mixin(PistonMovingBlockEntity.class)
public class PistonMovingBlockEntityMixin implements PistonMovingBlockEntityDuck {
    @Unique private BlockEntity ghostMovingEntity = null;
    @Override public BlockEntity getGhostMovingEntity() {
        return ghostMovingEntity;
    }
    @Override public void setGhostMovingEntity(BlockEntity blockEntity) {
        this.ghostMovingEntity = blockEntity;
    }


    @Inject(method = "saveAdditional", at = @At(value = "RETURN"))
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider provider, CallbackInfo ci) {
        if (this.ghostMovingEntity != null) {
            PistonMovingBlockEntity $this = (PistonMovingBlockEntity) (Object) this;
            PistonBlockEntityBehaviour behaviour = PistonModifiersManager.getBehaviour(this.ghostMovingEntity);
            if (behaviour != null) {
                PistonMoveContext context = new PistonMoveContext($this);
                tag.put("microprocessor_block_entity", behaviour.saveInMove(this.ghostMovingEntity, context, true));
            }
        }
    }

    @Inject(method = "loadAdditional", at = @At(value = "RETURN"))
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider provider, CallbackInfo ci) {
        if (tag.contains("microprocessor_block_entity")) {
            CompoundTag blockEntityTag = tag.getCompound("microprocessor_block_entity");
            PistonMovingBlockEntity $this = (PistonMovingBlockEntity) (Object) this;
            if ($this.getMovedState().getBlock() instanceof EntityBlock entityBlock) {
                this.ghostMovingEntity = entityBlock.newBlockEntity($this.getBlockPos(), $this.getMovedState());
                PistonBlockEntityBehaviour behaviour = PistonModifiersManager.getBehaviour(entityBlock);
                if (behaviour != null) {
                    PistonMoveContext context = new PistonMoveContext($this);
                    behaviour.loadInMove(ghostMovingEntity, blockEntityTag, context, true);
                }
            }

        }
    }




    @Redirect(
            method = "finalTick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z")
    )
    private boolean finalTick_setBlock(Level instance, BlockPos pos, BlockState newState, int flags) {
        boolean ret = instance.setBlock(pos, newState, flags);
        BlockEntity newBlockEntity = instance.getBlockEntity(pos);

        PistonMovingBlockEntity $this = (PistonMovingBlockEntity) (Object) this;
        if (ret && newBlockEntity != null && this.ghostMovingEntity != null) {
            PistonBlockEntityBehaviour behaviour = PistonModifiersManager.getBehaviour($this.getMovedState().getBlock());
            restoreBlockEntity(this.ghostMovingEntity, newBlockEntity, behaviour, $this);
        }

        return ret;
    }



    @Unique
    private static final ThreadLocal<PistonMovingBlockEntity> tick_pistonMovingBlockEntity = new ThreadLocal<>();

    @Inject(method = "tick", at = @At(value = "HEAD"))
    private static void tick_start(Level level, BlockPos pos, BlockState state, PistonMovingBlockEntity blockEntity, CallbackInfo ci) {
        tick_pistonMovingBlockEntity.set(blockEntity);
        PistonMovingBlockEntityDuck duck = (PistonMovingBlockEntityDuck) blockEntity;

        BlockState movedState = blockEntity.getMovedState();
        BlockEntity ghostEntity = duck.getGhostMovingEntity();
        PistonBlockEntityBehaviour behaviour = PistonModifiersManager.getBehaviour(movedState.getBlock());
        if (behaviour != null && ghostEntity != null) {
            PistonMovingBlockEntity $this = tick_pistonMovingBlockEntity.get();
            behaviour.tick(ghostEntity, new PistonMoveContext($this));
        }
    }

    @Redirect(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z")
    )
    private static boolean tick_setBlock(Level instance, BlockPos pos, BlockState newState, int flags) {
        boolean ret = instance.setBlock(pos, newState, flags);
        BlockEntity newBlockEntity = instance.getBlockEntity(pos);

        PistonMovingBlockEntity $this = tick_pistonMovingBlockEntity.get();
        PistonMovingBlockEntityDuck duck = (PistonMovingBlockEntityDuck) $this;
        if (ret && newBlockEntity != null && duck.getGhostMovingEntity() != null) {
            PistonBlockEntityBehaviour behaviour = PistonModifiersManager.getBehaviour($this.getMovedState().getBlock());
            if (behaviour != null) {
                restoreBlockEntity(duck.getGhostMovingEntity(), newBlockEntity, behaviour, $this);
            }
        }

        return ret;
    }

    @Inject(method = "tick", at = @At(value = "RETURN"))
    private static void tick_end(Level level, BlockPos pos, BlockState state, PistonMovingBlockEntity blockEntity, CallbackInfo ci) {
        tick_pistonMovingBlockEntity.remove();
    }



    @Unique private static void restoreBlockEntity(BlockEntity ghostEntity, BlockEntity newBlockEntity, PistonBlockEntityBehaviour behaviour, PistonMovingBlockEntity movingEntity) {
        if (behaviour != null) {
            behaviour.onEndMoveIn(ghostEntity, new PistonMoveContext(movingEntity));
            CompoundTag tag = behaviour.saveInMove(ghostEntity, new PistonMoveContext(movingEntity), false);
            behaviour.loadInMove(newBlockEntity, tag, new PistonMoveContext(movingEntity), false);
            newBlockEntity.clearRemoved();
            behaviour.onEndMovePost(newBlockEntity);
        }
    }
}
