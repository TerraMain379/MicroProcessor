package ru.terramain.microprocessor.plate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.registries.DeferredItem;
import ru.terramain.microprocessor.block.MicroProcessorBlockEntity;
import ru.terramain.microprocessor.logic.MicroProcessorWorker;

public abstract class Plate<D extends PlateData> {
    public final String type;
    public final PlateDataCodec<D> dataCodec;
    public final DeferredItem<AbstractPlateItem<D, ?>> item;
    public final PlateRenderer renderer;
    public final AbstractJsoPlateGenerator<?> jsoGenerator;

    protected Plate(String type, PlateDataCodec<D> dataCodec, DeferredItem<AbstractPlateItem<D, ?>> item, PlateRenderer renderer, AbstractJsoPlateGenerator<?> jsoGenerator) {
        this.type = type;
        this.dataCodec = dataCodec;
        this.item = item;
        this.renderer = renderer;
        this.jsoGenerator = jsoGenerator;
    }

    public MicroProcessorWorker.AnswerS2WMessage request(MicroProcessorWorker.RequestPlateW2SMessage request, PlateActionContext<?> context) {
        return new MicroProcessorWorker.AnswerS2WMessage(request.id, true, null);
    }

    public ItemInteractionResult onClickOnMicroprocessor(PlateActionContext<?> context, ItemStack itemStack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (itemStack.getItem() instanceof AbstractPlateItem<?, ?> plateItem) {
            return onClickOnMicroprocessorWithPlate(context, itemStack, state, level, pos, player, hand, hitResult, plateItem);
        }
        else if (player.isCrouching()) {
            return onClickOnMicroprocessorInCrouching(context, itemStack, state, level, pos, player, hand, hitResult);
        }
        else {
            return onClickOnMicroprocessorAny(context, itemStack, state, level, pos, player, hand, hitResult);
        }
    }
    public ItemInteractionResult onClickOnMicroprocessorWithPlate(PlateActionContext<?> context, ItemStack itemStack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult, AbstractPlateItem<?, ?> plateItem) {
        MicroProcessorBlockEntity be = context.context.be;
        Direction direction = context.direction;

        Plate<?> plate = plateItem.getPlate();
        PlateState<?, ?> plateState = PlateState.of(plate);

        onPlateRemoval(context, plateState, player);
        be.takeOutPlate(direction, plateState);
        afterPlateRemoval(context, plateState, player);

        itemStack.setCount(itemStack.getCount()-1);

        PlateActionContext<?> plateActionContext = new PlateActionContext<>(plateState, direction, context.context);
        plate.onPlatePlacement(plateActionContext, player);
        return ItemInteractionResult.SUCCESS;
    }
    public ItemInteractionResult onClickOnMicroprocessorInCrouching(PlateActionContext<?> context, ItemStack itemStack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        MicroProcessorBlockEntity be = context.context.be;
        Direction direction = context.direction;

        onPlateRemoval(context, null, player);
        if (!be.takeOutPlate(direction, null)) return ItemInteractionResult.FAIL;
        afterPlateRemoval(context, null, player);
        return ItemInteractionResult.SUCCESS;
    }
    public ItemInteractionResult onClickOnMicroprocessorAny(PlateActionContext<?> context, ItemStack itemStack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!player.level().isClientSide) {
            ServerPlayer serverPlayer = (ServerPlayer) player;
            context.context.be.openMenu(serverPlayer);
        }
        return ItemInteractionResult.SUCCESS;
    }

    public void onPlatePlacement(PlateActionContext<?> context, Player player) { }
    public void onPlateRemoval(PlateActionContext<?> context, PlateState<?, ?> newPlateState, Player player) { }
    public void afterPlateRemoval(PlateActionContext<?> context, PlateState<?, ?> newPlateState, Player player) { }
    public void onNeighborChanged(PlateActionContext<?> context, BlockState state, Level level, BlockPos pos, Block block, BlockPos neighborPos, boolean movedByPiston) { }
    public void onNeighborShapeChanged(PlateActionContext<?> context, BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) { }
    public void onTick(PlateActionContext<?> context) { }
    public int calculateWeakSignal(PlateActionContext<?> context, Direction direction) { return 0; }
    public int calculateStrongSignal(PlateActionContext<?> context, Direction direction) { return 0; }
    public boolean hasShaft(PlateActionContext<?> context) { return false; }
}
