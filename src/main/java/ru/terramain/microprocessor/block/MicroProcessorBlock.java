package ru.terramain.microprocessor.block;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.kinetics.base.IRotate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.registries.DeferredBlock;
import org.jetbrains.annotations.NotNull;
import ru.terramain.microprocessor.MicroProcessorMod;
import ru.terramain.microprocessor.logic.MicroProcessorCore;

public class MicroProcessorBlock extends BaseEntityBlock implements IRotate {
    public static final MapCodec<MicroProcessorBlock> CODEC = simpleCodec(MicroProcessorBlock::new);

    public MicroProcessorBlock(Properties properties) {
        super(properties);
    }
    public MicroProcessorBlock() {
        this(BlockBehaviour.Properties.of()
            .pushReaction(PushReaction.NORMAL)
            .jumpFactor(3)
            .isRedstoneConductor((state, level, pos) -> false)
        );
    }


    ///////////////// block entity
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MicroProcessorBlockEntity(pos, state);
    }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }
    @Override public @NotNull RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }
    ///////////////// end block entity


    ///////////////// actions
    @Override protected @NotNull ItemInteractionResult useItemOn(ItemStack itemStack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        MicroProcessorBlockEntity be = (MicroProcessorBlockEntity) level.getBlockEntity(pos);
        if (be == null) return ItemInteractionResult.FAIL;
        return be.onUseItem(itemStack, state, level, pos, player, hand, hitResult);
    }
    @Override protected void neighborChanged(BlockState blockState, Level level, BlockPos pos, Block block, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(blockState, level, pos, block, neighborPos, movedByPiston);
        MicroProcessorBlockEntity be = (MicroProcessorBlockEntity) level.getBlockEntity(pos);
        if (be == null) return;
        be.onNeighborChanged(blockState, level, pos, block, neighborPos, movedByPiston);
    }
    @Override protected void onRemove(BlockState blockState, Level level, BlockPos pos, BlockState newBlockState, boolean isMoving) {
        MicroProcessorBlockEntity be = (MicroProcessorBlockEntity) level.getBlockEntity(pos);
        if (be != null) {
            be.onRemove(blockState, level, pos, newBlockState, isMoving);
            be.remove();
        }
        super.onRemove(blockState, level, pos, newBlockState, false);
    }
    @Override protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        MicroProcessorBlockEntity be = (MicroProcessorBlockEntity) level.getBlockEntity(pos);
        if (be != null) {
            be.onPlace(state, level, pos, oldState, movedByPiston);
        }
        super.onPlace(state, level, pos, oldState, movedByPiston);
    }
    @Override protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        MicroProcessorBlockEntity be = (MicroProcessorBlockEntity) level.getBlockEntity(pos);
        if (be == null) return state;
        be.neighborShapeChanged(state, direction, neighborState, level, pos, neighborPos);
        return state;
    }
    ///////////////// end actions

    ///////////////// static actions
    @Override protected boolean isSignalSource(BlockState state) {
        return true;
    }
    @Override protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        MicroProcessorBlockEntity be = (MicroProcessorBlockEntity) level.getBlockEntity(pos);
        if (be == null) return 0;
        return be.onCheckWeakSignal(state, level, pos, direction.getOpposite());
    }
    @Override protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        MicroProcessorBlockEntity be = (MicroProcessorBlockEntity) level.getBlockEntity(pos);
        if (be == null) return 0;
        return be.onCheckStrongSignal(state, level, pos, direction.getOpposite());
    }
    ///////////////// end static actions

    ///////////////// create
    @Override public boolean hasShaftTowards(LevelReader levelReader, BlockPos blockPos, BlockState blockState, Direction direction) {
        MicroProcessorBlockEntity be = (MicroProcessorBlockEntity) levelReader.getBlockEntity(blockPos);
        if (be == null) return false;
        return be.hasShaftTowards(levelReader, blockPos, blockState, direction);
    }
    @Override public Direction.Axis getRotationAxis(BlockState blockState) {
        return null;
    }
    ///////////////// end create





    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide()
                ? createTickerHelper(
                    type,
                    MicroProcessorBlockEntity.instance().get(),
                    (level1, blockPos, blockState, be) -> be.clientTick(level1, blockPos, blockState)
                )
                : createTickerHelper(
                    type,
                    MicroProcessorBlockEntity.instance().get(),
                    (level1, blockPos, blockState, be) -> be.tick(level1, blockPos, blockState)
                );
    }

    private static DeferredBlock<MicroProcessorBlock> inst = null;
    public static DeferredBlock<MicroProcessorBlock> instance() {
        if (inst == null) {
            inst = MicroProcessorMod.BLOCKS.register("microprocessor", () -> new MicroProcessorBlock());
        }
        return inst;
    }
}
