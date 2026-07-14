package ru.terramain.microprocessor.plate.plates;

import com.mojang.serialization.Codec;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.registries.DeferredItem;
import org.graalvm.polyglot.HostAccess;
import ru.terramain.microprocessor.MicroProcessorMod;
import ru.terramain.microprocessor.block.MicroProcessorBlock;
import ru.terramain.microprocessor.js.JsFuture;
import ru.terramain.microprocessor.logic.MicroProcessorException;
import ru.terramain.microprocessor.logic.MicroProcessorWorker;
import ru.terramain.microprocessor.plate.*;

public class PlateRedstoneDistributor extends Plate<PlateRedstoneDistributor.Data> {
    public static final String TYPE = "distributor";

    protected PlateRedstoneDistributor() {
        super(TYPE, DataCodec.INSTANCE, Item.instance, renderer, jsoGenerator);
    }

    public static class Data implements PlateData {
        public int signal;

        public Data(int signal) {
            this.signal = signal;
        }

        @Override
        public <D extends PlateData> D copy() {
            return (D) new Data(signal);
        }
    }
    public static class DataCodec implements PlateDataCodec<Data> {
        public static final DataCodec INSTANCE = new DataCodec();
        public static final Codec<Data> CODEC = Codec.INT.xmap(Data::new, data -> data.signal);
        public static final StreamCodec<RegistryFriendlyByteBuf, Data> STREAM_CODEC = StreamCodec.of(
                (o, data) -> ByteBufCodecs.INT.encode(o, data.signal),
                i -> new Data(ByteBufCodecs.INT.decode(i))
        );

        @Override
        public Data defaultData() {
            return new Data(0);
        }
        @Override
        public Codec<Data> getCodec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, Data> getStreamCodec() {
            return STREAM_CODEC;
        }
    }
    public static class Item extends AbstractPlateItem<Data, PlateRedstoneDistributor> {
        public static final DeferredItem<AbstractPlateItem<Data, ?>> instance = MicroProcessorMod.ITEMS.register("plate_" + TYPE, () -> new Item());

        public Item(Properties properties) {
            super(properties);
        }
        public Item() {
            this(new Properties());
        }

        @Override
        public PlateRedstoneDistributor getPlate() {
            return PlateRedstoneDistributor.instance();
        }
    }
    public static final PlateRenderer renderer = new TexturePlateRenderer() {
        public static final ResourceLocation texture_off = ResourceLocation.fromNamespaceAndPath(MicroProcessorMod.MODID, "block/microprocessor_distributor_off");
        public static final ResourceLocation texture_on = ResourceLocation.fromNamespaceAndPath(MicroProcessorMod.MODID, "block/microprocessor_distributor_on");

        @Override
        public TextureAtlasSprite sprite(PlateActionContext<?> context) {
            Data data = (Data) context.plateState.data;
            if (data.signal > 0) {
                return new Material(InventoryMenu.BLOCK_ATLAS, texture_on).sprite();
            }
            else {
                return new Material(InventoryMenu.BLOCK_ATLAS, texture_off).sprite();
            }
        }
    };
    public static class Jso extends AbstractJsoPlate {
        @HostAccess.Export public int signal;
        @HostAccess.Export public boolean isActive;

        public Jso(MicroProcessorWorker worker, Direction direction, PlateState<?, ?> plateState) {
            super(worker, PlateRedstoneDistributor.instance(), direction, plateState);
            this.update(plateState);
        }
        public void update(PlateState<?, ?> plateState1) {
            PlateState<Data, PlateRedstoneDistributor> plateState = (PlateState<Data, PlateRedstoneDistributor>) plateState1;
            this.signal = plateState.data.signal;
            this.isActive = plateState.data.signal > 0;
        }

        @HostAccess.Export
        public void setSignal(int signal) {
            if (signal >= 0 && signal <= 15) {
                this.worker.dataPool.pushW2S(
                        new SetSignalRequestMessage(
                                this.worker.nextId.getAndIncrement(),
                                this.direction,
                                this.plate,
                                signal
                        )
                );
            }
            else throw new MicroProcessorException("incorrect signal value");
        }

        @HostAccess.Export
        public JsFuture<Integer> readWeakSignal() {
            return this.worker.waitAnswerForW2SRequest(new GetWeakSignalRequestMessage(
                    this.worker.nextId.getAndIncrement(),
                    this.direction,
                    this.plate
            ));
        }
        @HostAccess.Export
        public JsFuture<Integer> readStrongSignal() {
            return this.worker.waitAnswerForW2SRequest(new GetStrongSignalRequestMessage(
                    this.worker.nextId.getAndIncrement(),
                    this.direction,
                    this.plate
            ));
        }
    }
    public static final AbstractJsoPlateGenerator<Jso> jsoGenerator = Jso::new;


    public static class SetSignalRequestMessage extends MicroProcessorWorker.RequestPlateW2SMessage {
        int signal;
        public SetSignalRequestMessage(long id, Direction direction, Plate<?> plate, int signal) {
            super(id, direction, plate);
            this.signal = signal;
        }
    }
    public static class GetWeakSignalRequestMessage extends MicroProcessorWorker.RequestPlateW2SMessage {
        public GetWeakSignalRequestMessage(long id, Direction direction, Plate<?> plate) {
            super(id, direction, plate);
        }
    }
    public static class GetStrongSignalRequestMessage extends MicroProcessorWorker.RequestPlateW2SMessage {
        public GetStrongSignalRequestMessage(long id, Direction direction, Plate<?> plate) {
            super(id, direction, plate);
        }
    }
    public static class SetSignalEventMessage extends MicroProcessorWorker.PlateEventS2WMessage {
        public SetSignalEventMessage(Direction direction, int signal) {
            super(direction, "set_signal", new Object[]{ signal });
        }
    }
    public static class SetActiveEventMessage extends MicroProcessorWorker.PlateEventS2WMessage {
        public SetActiveEventMessage(Direction direction, boolean isActive) {
            super(direction, "set_active", new Object[]{ isActive });
        }
    }
    @Override public MicroProcessorWorker.AnswerS2WMessage request(MicroProcessorWorker.RequestPlateW2SMessage request, PlateActionContext<?> context) {
        if (request instanceof SetSignalRequestMessage setSignalMessage) {
            setSignal(context, setSignalMessage.signal, false);
            return new MicroProcessorWorker.AnswerS2WMessage(request.id, false, setSignalMessage.signal);
        }
        else if (request instanceof GetWeakSignalRequestMessage) {
            Level level = context.context.be.getLevel();
            BlockPos pos = context.context.be.getBlockPos();
            Direction side = context.direction;
            int signal = level.getSignal(pos.relative(side), side);
            return new MicroProcessorWorker.AnswerS2WMessage(request.id, false, signal);
        }
        else if (request instanceof GetStrongSignalRequestMessage) {
            Level level = context.context.be.getLevel();
            BlockPos pos = context.context.be.getBlockPos();
            Direction side = context.direction;
            int signal = level.getDirectSignal(pos.relative(side), side);
            return new MicroProcessorWorker.AnswerS2WMessage(request.id, false, signal);
        }
        return super.request(request, context);
    }


    public void setSignal(PlateActionContext<?> context, int signal, boolean notify) {
        Data data = (Data) context.plateState.data;
        data.signal = signal;
        context.setChanged();

        Level level = context.context.be.getLevel();
        BlockPos pos = context.context.be.getBlockPos();
        BlockPos relative = pos.relative(context.direction);
        Block updater = MicroProcessorBlock.instance().get();
        level.neighborChanged(relative, updater, pos); // update neighbor
        level.updateNeighborsAtExceptFromFacing(relative, updater, context.direction.getOpposite()); // update neighbor neighbors

        if (notify) {
            context.context.be.core.worker.dataPool.pushS2WMessage(
                    new SetSignalEventMessage(context.direction, data.signal)
            );
            context.context.be.core.worker.dataPool.pushS2WMessage(
                    new SetActiveEventMessage(context.direction, data.signal > 0)
            );
        }
    }

    @Override public ItemInteractionResult onClickOnMicroprocessorAny(PlateActionContext<?> context, ItemStack itemStack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (player.getMainHandItem().isEmpty()) {
            Data data = (Data) context.plateState.data;
            setSignal(
                    context,
                    data.signal == 0 ? 15 : 0,
                    true
            );
            context.setChanged();
            return ItemInteractionResult.SUCCESS;
        }
        return super.onClickOnMicroprocessorAny(context, itemStack, state, level, pos, player, hand, hitResult);
    }
    @Override public int calculateWeakSignal(PlateActionContext<?> context, Direction direction) {
        Data data = (Data) context.plateState.data;
        return data.signal;
    }
    @Override public int calculateStrongSignal(PlateActionContext<?> context, Direction direction) {
        Data data = (Data) context.plateState.data;
        return data.signal;
    }

    private static PlateRedstoneDistributor inst = null;
    public static PlateRedstoneDistributor instance() {
        if (inst == null) {
            inst = PlateRegister.register(new PlateRedstoneDistributor());
        }
        return inst;
    }
}
