package ru.terramain.microprocessor.deps.create;

import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import ru.terramain.microprocessor.block.MicroProcessorBlock;
import ru.terramain.microprocessor.block.MicroProcessorBlockEntity;

public class MicroProcessorMovementBehaviour implements MovementBehaviour {

    @Override
    public boolean isActive(MovementContext context) {
        return true;
    }

    @Override
    public void tick(MovementContext context) {
        if (context.world == null || context.world.isClientSide) return;
        MicroProcessorBlockEntity be = getOrCreateBE(context);
        be.tick(context.world, context.localPos, context.state);
        saveBE(context, be);
    }
    private MicroProcessorBlockEntity getOrCreateBE(MovementContext context) {
        if (context.temporaryData instanceof MicroProcessorBlockEntity cached) {
            return cached;
        }
        BlockEntity loaded = BlockEntity.loadStatic(
                context.localPos,
                context.state,
                context.blockEntityData,
                context.world.registryAccess()
        );
        if (loaded instanceof MicroProcessorBlockEntity be) {
            return be;
        }
        else {
            MicroProcessorBlockEntity be = new MicroProcessorBlockEntity(context.localPos, context.state);
            context.temporaryData = be;
            return be;
        }
    }
    private void saveBE(MovementContext context, MicroProcessorBlockEntity be) {
        CompoundTag tag = be.saveWithFullMetadata(context.world.registryAccess());
        context.blockEntityData = tag;
    }}
