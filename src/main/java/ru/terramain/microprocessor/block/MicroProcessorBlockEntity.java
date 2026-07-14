package ru.terramain.microprocessor.block;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import com.mojang.serialization.DynamicOps;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import ru.terramain.microprocessor.MicroProcessorMod;
import ru.terramain.microprocessor.logic.MicroProcessorContext;
import ru.terramain.microprocessor.logic.MicroProcessorCore;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import ru.terramain.microprocessor.MicroProcessorDataComponents;
import ru.terramain.microprocessor.scriptscreen.ScriptScreenSessions;
import ru.terramain.microprocessor.network.payload.scriptscreen.OpenScriptScreenPayload;
import ru.terramain.microprocessor.network.payload.PlatesUpdatePayload;
import ru.terramain.microprocessor.network.payload.scriptscreen.UpdateScriptScreenPayload;
import ru.terramain.microprocessor.plate.*;
import ru.terramain.microprocessor.plate.plates.NullPlate;

public class MicroProcessorBlockEntity extends BlockEntity {
    public MicroProcessorCore core;

    public Plates plates;
    public String code;
    public List<String> logs;

    public MicroProcessorBlockEntity(BlockPos pos, BlockState state) {
        super(instance().get(), pos, state);
        this.plates = new Plates();
        this.code = "";
        this.logs = new ArrayList<>();
        this.logs.add("[SYSTEM]: MicroProcessor created");
        this.core = new MicroProcessorCore();
    }

    ///////////////// serialize/deserialize
    // nbt
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        DynamicOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
        Tag platesTag = Plates.CODEC.encodeStart(ops, Plates.Snapshot.by(this.plates)).getOrThrow();
        tag.put("plates", platesTag);

        tag.putString("code", this.code);
        ListTag listTag = new ListTag();
        this.logs.forEach(string -> listTag.add(net.minecraft.nbt.StringTag.valueOf(string)));
        tag.put("logs", listTag);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        DynamicOps<Tag> ops = registries.createSerializationContext(NbtOps.INSTANCE);
        if (tag.contains("plates")) {
            Tag platesTag = tag.get("plates");
            this.plates = Plates.CODEC.parse(ops, platesTag).result().orElseGet(Plates.Snapshot::by).copy();
        }
        else this.plates = new Plates();

        this.code = tag.getString("code");
        if (tag.contains("logs")) {
            ListTag logsTag = tag.getList("logs", Tag.TAG_STRING);
            this.logs = new ArrayList<>();
            for (int i = 0; i < logsTag.size(); i++) {
                this.logs.add(logsTag.getString(i));
            }
        } else {
            this.logs = new ArrayList<>();
        }
    }

    // sync client-server
    @Override public @NotNull CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomAndMetadata(registries);
    }
    @Override public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // генерация данных для/из предмета
    @Override protected void collectImplicitComponents(DataComponentMap.Builder dataComponentOutput) {
        super.collectImplicitComponents(dataComponentOutput);
        dataComponentOutput.set(MicroProcessorDataComponents.PLATES, Plates.Snapshot.by(this.plates));
        dataComponentOutput.set(MicroProcessorDataComponents.CODE, this.code);
    }
    @Override protected void applyImplicitComponents(DataComponentInput dataComponentInput) {
        super.applyImplicitComponents(dataComponentInput);
        Plates.Snapshot snapshot = dataComponentInput.getOrDefault(MicroProcessorDataComponents.PLATES, null);
        this.plates = snapshot != null ? snapshot.copy() : new Plates();
        this.code = dataComponentInput.getOrDefault(MicroProcessorDataComponents.CODE, "");
    }
    ///////////////// end serialize/deserialize


    ///////////////// client sync
    @Override public void setChanged() {
        if (!this.level.isClientSide) {
            ServerLevel serverLevel = (ServerLevel) level;
            PacketDistributor.sendToPlayersTrackingChunk(
                    serverLevel,
                    new ChunkPos(this.getBlockPos()),
                    new PlatesUpdatePayload(
                            this.getBlockPos(),
                            Plates.Snapshot.by(this.plates)
                    )
            );
        }
        super.setChanged();
    }
    ///////////////// end client sync


    ///////////////// plates actions
    public void onePlateConsumer(Direction direction, BiConsumer<PlateState<?, ?>, PlateActionContext<?>> action) {
        PlateState<?, ?> plateState = getPlateState(direction);
        if (plateState != null) {
            MicroProcessorContext context1 = new MicroProcessorContext(this);
            PlateActionContext<?> context2 = new PlateActionContext<>(plateState, direction, context1);
            action.accept(plateState, context2);
        }
    }
    public <T> T onePlateFunction(Direction direction, BiFunction<PlateState<?, ?>, PlateActionContext<?>, T> action, T defaultValue) {
        PlateState<?, ?> plateState = getPlateState(direction);
        if (plateState != null) {
            MicroProcessorContext context1 = new MicroProcessorContext(this);
            PlateActionContext<?> context2 = new PlateActionContext<>(plateState, direction, context1);
            return action.apply(plateState, context2);
        }
        return defaultValue;
    }
    public <T> T onePlateFunction(Direction direction, BiFunction<PlateState<?, ?>, PlateActionContext<?>, T> action, PlateState<?, ?> defaultPlate) {
        PlateState<?, ?> plateState = getPlateState(direction);
        if (plateState == null) plateState = defaultPlate;
        MicroProcessorContext context1 = new MicroProcessorContext(this);
        PlateActionContext<?> context2 = new PlateActionContext<>(plateState, direction, context1);
        return action.apply(plateState, context2);
    }
    public void allPlatesConsumer(BiConsumer<PlateState<?, ?>, PlateActionContext<?>> action) {
        for (Direction direction : Direction.values()) {
            onePlateConsumer(direction, action);
        }
    }

    public ItemInteractionResult onUseItem(ItemStack itemStack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        Direction direction = hitResult.getDirection();
        return this.<ItemInteractionResult>onePlateFunction(direction, (plateState, context) -> {
            return plateState.plate.onClickOnMicroprocessor(context, itemStack, state, level, pos, player, hand, hitResult);
        }, NullPlate.PLATE_STATE);
    }
    public void onNeighborChanged(BlockState blockState, Level level, BlockPos pos, Block block, BlockPos neighborPos, boolean movedByPiston) {
        allPlatesConsumer((plateState, context) -> {
            plateState.plate.onNeighborChanged(context, blockState, level, pos, block, neighborPos, movedByPiston);
        });
    }
    public void neighborShapeChanged(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        allPlatesConsumer((plateState, context) -> {
            plateState.plate.onNeighborShapeChanged(context, state, direction, neighborState, level, pos, neighborPos);
        });
    }

    public int onCheckWeakSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return onePlateFunction(direction, (plateState, context) -> {
            return plateState.plate.calculateWeakSignal(context, direction);
        }, 0);
    }
    public int onCheckStrongSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return onePlateFunction(direction, (plateState, context) -> {
            return plateState.plate.calculateStrongSignal(context, direction);
        }, 0);
    }
    ///////////////// end plates actions


    ///////////////// logic center
    public void openMenu(ServerPlayer player) {
        BlockPos pos = this.getBlockPos();
        ScriptScreenSessions.open(player, pos);
        UpdateScriptScreenPayload updatePayload = new UpdateScriptScreenPayload(
                pos,
                true,
                code,
                true, true, new ArrayList<>(this.logs),
                true,
                this.core.isRunning()
        );
        PacketDistributor.sendToPlayer(player, new OpenScriptScreenPayload(updatePayload));
    }
    public void handleUpdatePacket(UpdateScriptScreenPayload payload, ServerPlayer player) {
        if (payload.codeChanged()) this.code = payload.code();
        if (payload.logsCleared()) this.logs.clear();
        if (payload.logsPushed()) this.logs.addAll(payload.logs());
        if (payload.isRunningChanged()) this.setRunning(payload.isRunning(), false);

        if (this.level instanceof ServerLevel serverLevel) {
            if (player == null) ScriptScreenSessions.sendUpdate(serverLevel.getServer(), getBlockPos(), payload);
            else ScriptScreenSessions.sendUpdate(serverLevel.getServer(), getBlockPos(), payload, player);
        }
    }
    public void setRunning(boolean isRunning, boolean notify) {
        if (this.core.worker.isRunningStorage.isRunning) {
            this.core.worker.stop();
        }
        if (isRunning) {
            this.core.worker.run(this.code);
        }
        if (notify) setRunningNotify(isRunning);
    }
    public void setRunningNotify(boolean isRunning) {
        ScriptScreenSessions.sendUpdate(this.getLevel().getServer(), this.getBlockPos(), new UpdateScriptScreenPayload(
                this.getBlockPos(),
                false, "",
                false, false, new ArrayList<>(),
                true, isRunning
        ));
    }
    public void pushLogs(String logs) {
        ArrayList<String> newLogs = new ArrayList<>(List.of(logs.split("\\n")));
        this.logs.addAll(newLogs);
        ScriptScreenSessions.sendUpdate(level.getServer(), this.getBlockPos(), new UpdateScriptScreenPayload(
                this.getBlockPos(),
                false, "",
                true, false, newLogs,
                false, false
        ));
    }
    public void tick(Level level, BlockPos pos, BlockState state) {
        this.core.tick(new MicroProcessorContext(this));
        allPlatesConsumer((plateState, context) -> {
            plateState.plate.onTick(context);
        });
    }
    ///////////////// end logic center


    ///////////////// actions
    protected void onRemove(BlockState blockState, Level level, BlockPos pos, BlockState newBlockState, boolean isMoving) {
        if (!level.isClientSide) {
            ScriptScreenSessions.closeAll(level.getServer(), pos);
        }
    }
    ///////////////// end actions


    ///////////////// getters/setters
    public PlateState<?, ?> getPlateState(Direction direction) {
        return this.plates.plates[direction.ordinal()];
    }
    public void setPlateState(Direction direction, PlateState<?, ?> plateState) {
        this.plates.plates[direction.ordinal()] = plateState;
        this.setChanged();
    }
    ///////////////// end getters/setters


    ///////////////// instance
    private static Supplier<BlockEntityType<MicroProcessorBlockEntity>> inst = null;
    public static Supplier<BlockEntityType<MicroProcessorBlockEntity>> instance() {
        if (inst == null) {
            inst = MicroProcessorMod.BLOCK_ENTITIES.register(
                "micro_processor_be",
                () -> BlockEntityType.Builder.of(MicroProcessorBlockEntity::new, MicroProcessorMod.MICRO_PROCESSOR_BLOCK.get()).build(null)
            );
        }
        return inst;
    }
    ///////////////// end instance
}
