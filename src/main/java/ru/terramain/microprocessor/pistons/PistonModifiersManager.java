package ru.terramain.microprocessor.pistons;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.registries.DeferredBlock;

import java.util.HashMap;
import java.util.Map;

public class PistonModifiersManager {
    public static Map<DeferredBlock<? extends EntityBlock>, PistonBlockEntityBehaviour> behaviours = new HashMap<>();
    public static Map<EntityBlock, PistonBlockEntityBehaviour> runtimeBehaviours = new HashMap<>();

    public static PistonBlockEntityBehaviour getBehaviour(DeferredBlock<? extends EntityBlock> entityBlock) {
        return behaviours.get(entityBlock);
    }
    public static PistonBlockEntityBehaviour getBehaviour(EntityBlock entityBlock) {
        return runtimeBehaviours.get(entityBlock);
    }
    public static PistonBlockEntityBehaviour getBehaviour(Block block) {
        if (block instanceof EntityBlock entityBlock) {
            return PistonModifiersManager.getBehaviour(entityBlock);
        }
        return null;
    }
    public static PistonBlockEntityBehaviour getBehaviour(BlockEntity blockEntity) {
        if (blockEntity.getBlockState().getBlock() instanceof EntityBlock entityBlock) {
            return PistonModifiersManager.getBehaviour(entityBlock);
        }
        return null;
    }

    public static void compile() {
        behaviours.forEach((deferredBlock, behaviour) -> {
            runtimeBehaviours.put(deferredBlock.get(), behaviour);
        });
    }

    public static void register(PistonBlockEntityBehaviour behaviour, DeferredBlock<? extends EntityBlock> entityBlock) {
        behaviours.put(entityBlock, behaviour);
    }
    public static void register(PistonBlockEntityBehaviour behaviour, EntityBlock entityBlock) {
        runtimeBehaviours.put(entityBlock, behaviour);
    }
}
