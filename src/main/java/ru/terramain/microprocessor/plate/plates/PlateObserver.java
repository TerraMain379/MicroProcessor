package ru.terramain.microprocessor.plate.plates;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredItem;
import org.graalvm.polyglot.HostAccess;
import ru.terramain.microprocessor.MicroProcessorMod;
import ru.terramain.microprocessor.logic.MicroProcessorWorker;
import ru.terramain.microprocessor.plate.AbstractPlateItem;
import ru.terramain.microprocessor.plate.*;

public class PlateObserver extends Plate<PlateObserver.Data> {
    public static String TYPE = "observer";

    protected PlateObserver() {
        super(TYPE, DataCodec.INSTANCE, Item.instance, renderer, jsoGenerator);
    }

    public static class Data implements PlateData {
        public int preActiveWaitCounter; // 2|1|0
        public int activeCounter; // 2|1|0

        public Data(int preActiveWaitCounter, int activeCounter) {
            this.preActiveWaitCounter = preActiveWaitCounter;
            this.activeCounter = activeCounter;
        }

        @Override
        public <D extends PlateData> D copy() {
            return (D) new Data(preActiveWaitCounter, activeCounter);
        }
    }
    public static class DataCodec implements PlateDataCodec<Data> {
        public static final DataCodec INSTANCE = new DataCodec();
        public static final Codec<Data> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("pre_active_wait_counter").forGetter(o -> o.preActiveWaitCounter),
                Codec.INT.fieldOf("active_counter").forGetter(o -> o.activeCounter)
        ).apply(instance, Data::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, Data> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.INT, o -> o.preActiveWaitCounter,
                ByteBufCodecs.INT, o -> o.activeCounter,
                Data::new
        );

        @Override
        public Data defaultData() {
            return new Data(0, 0);
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
    public static class Item extends AbstractPlateItem<Data, PlateObserver> {
        public static final DeferredItem<AbstractPlateItem<Data, ?>> instance = MicroProcessorMod.ITEMS.register("plate_" + TYPE, () -> new Item());

        public Item(Properties properties) {
            super(properties);
        }
        public Item() {
            this(new Properties());
        }

        @Override
        public PlateObserver getPlate() {
            return PlateObserver.instance();
        }
    }
    public static final PlateRenderer renderer = new TexturePlateRenderer() {
        public static final ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(MicroProcessorMod.MODID, "block/microprocessor_observer");
        @Override
        public TextureAtlasSprite sprite(PlateActionContext<?> context) {
            return new Material(InventoryMenu.BLOCK_ATLAS, texture).sprite();
        }
    };
    public static class Jso extends AbstractJsoPlate {
        @HostAccess.Export public boolean isTriggered;

        public Jso(MicroProcessorWorker worker, Direction direction, PlateState<?, ?> plateState) {
            super(worker, PlateObserver.instance(), direction, plateState);
            this.update(plateState);
        }
        public void update(PlateState<?, ?> plateState1) {
            PlateState<Data, PlateObserver> plateState = (PlateState<Data, PlateObserver>) plateState1;
            this.isTriggered = plateState.data.activeCounter != 0;
        }
    }
    public static final AbstractJsoPlateGenerator<Jso> jsoGenerator = Jso::new;


    public static class OnUpdateEventMessage extends MicroProcessorWorker.PlateEventS2WMessage {
        public OnUpdateEventMessage(Direction direction) {
            super(direction, "update", new Object[]{ });
        }
    }


    public boolean startByUpdate(PlateActionContext<?> context) {
        Data data = (Data) context.plateState.data;
        if (data.activeCounter > 0 || data.preActiveWaitCounter > 0) return false;
        data.preActiveWaitCounter = 2;
        context.setChanged();
        return true;
    }

    @Override public void onTick(PlateActionContext<?> context) {
        Data data = (Data) context.plateState.data;
        if (data.activeCounter > 0) {
            data.activeCounter--;
            context.setChanged();
        }
        if (data.preActiveWaitCounter > 0) {
            data.preActiveWaitCounter--;
            if (data.preActiveWaitCounter == 0) {
                context.context.be.core.worker.dataPool.pushS2WMessage(new OnUpdateEventMessage(context.direction));
                data.activeCounter = 2;
            }
            context.setChanged();
        }
    }
    @Override public void onNeighborShapeChanged(PlateActionContext<?> context, BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (pos.relative(context.direction).equals(neighborPos)) {
            startByUpdate(context);
        }
    }


    private static PlateObserver inst = null;
    public static PlateObserver instance() {
        if (inst == null) {
            inst = PlateRegister.register(new PlateObserver());
        }
        return inst;
    }
}
