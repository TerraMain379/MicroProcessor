package ru.terramain.microprocessor.plate.plates;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.registries.DeferredItem;
import org.graalvm.polyglot.HostAccess;
import ru.terramain.microprocessor.MicroProcessorMod;
import ru.terramain.microprocessor.js.JsFuture;
import ru.terramain.microprocessor.logic.MicroProcessorWorker;
import ru.terramain.microprocessor.plate.*;

public class PlatePiston extends Plate<PlatePiston.Data> {
    public static String TYPE = "piston";

    protected PlatePiston() {
        super(TYPE, DataCodec.INSTANCE, Item.instance, renderer, jsoGenerator);
    }

    public static class Data implements PlateData {
        public boolean isSpread;
        public int moveDelta; // 0|1|2

        public Data(boolean isSpread, int moveDelta) {
            this.isSpread = isSpread;
            this.moveDelta = moveDelta;
        }

        @Override
        public <D extends PlateData> D copy() {
            return (D) new Data(isSpread, moveDelta);
        }
    }
    public static class DataCodec implements PlateDataCodec<Data> {
        public static final DataCodec INSTANCE = new DataCodec();
        public static final Codec<Data> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.BOOL.optionalFieldOf("is_spread", false).forGetter(o -> o.isSpread),
                Codec.INT.optionalFieldOf("move_delta", 0).forGetter(o -> o.moveDelta)
        ).apply(instance, Data::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, Data> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, o -> o.isSpread,
                ByteBufCodecs.INT, o -> o.moveDelta,
                Data::new
        );

        @Override
        public Data defaultData() {
            return new Data(false, 0);
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
    public static class Item extends AbstractPlateItem<Data, PlatePiston> {
        public static final DeferredItem<AbstractPlateItem<Data, ?>> instance = MicroProcessorMod.ITEMS.register("plate_" + TYPE, () -> new Item());

        public Item(Properties properties) {
            super(properties);
        }
        public Item() {
            this(new Properties());
        }

        @Override
        public PlatePiston getPlate() {
            return PlatePiston.instance();
        }
    }
    public static final PlateRenderer renderer = new TexturePlateRenderer() {
        public static final ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(MicroProcessorMod.MODID, "block/microprocessor_piston");
        @Override
        public TextureAtlasSprite sprite(PlateActionContext<?> context) {
            return new Material(InventoryMenu.BLOCK_ATLAS, texture).sprite();
        }
    };
    public static class Jso extends AbstractJsoPlate {
        @HostAccess.Export public boolean isSpread;
        @HostAccess.Export public boolean inMove;

        public Jso(MicroProcessorWorker worker, Direction direction, PlateState<?, ?> plateState) {
            super(worker, PlatePiston.instance(), direction, plateState);
            this.update(plateState);
        }
        public void update(PlateState<?, ?> plateState1) {
            PlateState<Data, PlatePiston> plateState = (PlateState<Data, PlatePiston>) plateState1;
            this.isSpread = plateState.data.isSpread;
            this.inMove = plateState.data.moveDelta != 0;
        }

        @HostAccess.Export
        public JsFuture<Boolean> checkForMove() {
            return this.worker.waitAnswerForW2SRequest(new CheckForMoveRequestMessage(
                    this.worker.nextId.getAndIncrement(),
                    direction,
                    plate
            ));
        }
        @HostAccess.Export
        public JsFuture<Boolean> setPowered(boolean isPowered) {
            return this.worker.waitAnswerForW2SRequest(new SetPoweredRequestMessage(
                    this.worker.nextId.getAndIncrement(),
                    direction,
                    plate,
                    isPowered
            ));
        }
    }
    public static final AbstractJsoPlateGenerator<Jso> jsoGenerator = Jso::new;



    public static class CheckForMoveRequestMessage extends MicroProcessorWorker.RequestPlateW2SMessage {
        public CheckForMoveRequestMessage(long id, Direction direction, Plate<?> plate) {
            super(id, direction, plate);
        }
    }
    public static class SetPoweredRequestMessage extends MicroProcessorWorker.RequestPlateW2SMessage {
        boolean isPowered;
        public SetPoweredRequestMessage(long id, Direction direction, Plate<?> plate, boolean isPowered) {
            super(id, direction, plate);
            this.isPowered = isPowered;
        }
    }
    @Override
    public MicroProcessorWorker.AnswerS2WMessage request(MicroProcessorWorker.RequestPlateW2SMessage request, PlateActionContext<?> context) {
        if (request instanceof CheckForMoveRequestMessage) {
            // TODO:
            return new MicroProcessorWorker.AnswerS2WMessage(request.id, false, true);
        }
        else if (request instanceof SetPoweredRequestMessage setSignalRequest) {
            // TODO:
            Data data = (Data) context.plateState.data;
            data.isSpread = setSignalRequest.isPowered;
            context.setChanged();
            return new MicroProcessorWorker.AnswerS2WMessage(request.id, false, setSignalRequest.isPowered);
        }
        return super.request(request, context);
    }

    private static PlatePiston inst = null;
    public static PlatePiston instance() {
        if (inst == null) {
            inst = PlateRegister.register(new PlatePiston());
        }
        return inst;
    }
}
