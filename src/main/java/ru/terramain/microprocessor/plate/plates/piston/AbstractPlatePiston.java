package ru.terramain.microprocessor.plate.plates.piston;

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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.registries.DeferredItem;
import org.graalvm.polyglot.HostAccess;
import ru.terramain.microprocessor.MicroProcessorMod;
import ru.terramain.microprocessor.js.JsFuture;
import ru.terramain.microprocessor.logic.MicroProcessorWorker;
import ru.terramain.microprocessor.network.payload.MicroProcessorPistonActionPayload;
import ru.terramain.microprocessor.plate.*;

import java.util.function.Function;
import java.util.function.Supplier;

public class AbstractPlatePiston extends Plate<AbstractPlatePiston.Data> {
    public boolean isSticky;
    public Supplier<AbstractPlatePiston> getPlateMethod;

    protected AbstractPlatePiston(String type, String texturePath, Supplier<AbstractPlatePiston> getPlateMethod, boolean isSticky) {
        super(type, DataCodec.INSTANCE, Item.register(type, getPlateMethod), createRenderer(texturePath), createJsoGenerator(getPlateMethod));
        this.isSticky = isSticky;
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
    public static class Item extends AbstractPlateItem<Data, AbstractPlatePiston> {
        public static DeferredItem<AbstractPlateItem<Data, ?>> register(String type, Supplier<AbstractPlatePiston> getPlateMethod) {
            return MicroProcessorMod.ITEMS.register("plate_" + type, () -> new Item(getPlateMethod));
        }
        public Supplier<AbstractPlatePiston> getPlateMethod;

        public Item(Properties properties) {
            super(properties);
        }
        public Item(Supplier<AbstractPlatePiston> getPlateMethod) {
            this(new Properties());
            this.getPlateMethod = getPlateMethod;
        }

        @Override
        public AbstractPlatePiston getPlate() {
            return getPlateMethod.get();
        }
    }
    public static PlateRenderer createRenderer(String texturePath) {
        return new TexturePlateRenderer() {
            public final ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(MicroProcessorMod.MODID, texturePath);
            @Override
            public TextureAtlasSprite sprite(PlateActionContext<?> context) {
                return new Material(InventoryMenu.BLOCK_ATLAS, texture).sprite();
            }
        };
    }
    public static class Jso extends AbstractJsoPlate {
        @HostAccess.Export public boolean isSpread;
        @HostAccess.Export public boolean inMove;

        public Jso(MicroProcessorWorker worker, Direction direction, PlateState<?, ?> plateState, Supplier<AbstractPlatePiston> getPlateMethod) {
            super(worker, getPlateMethod.get(), direction, plateState);
            this.update(plateState);
        }
        public void update(PlateState<?, ?> plateState1) {
            PlateState<Data, AbstractPlatePiston> plateState = (PlateState<Data, AbstractPlatePiston>) plateState1;
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
    public static AbstractJsoPlateGenerator<Jso> createJsoGenerator(Supplier<AbstractPlatePiston> getPlateMethod) {
        return (worker, direction, plateState) -> new Jso(worker, direction, plateState, getPlateMethod);
    }


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
            if (setSignalRequest.isPowered) {
                BlockPos pos = context.context.be.getBlockPos();
                MicroProcessorPistonActionPayload payload = new MicroProcessorPistonActionPayload(
                        pos,
                        context.direction,
                        true
                );
                PacketDistributor.sendToPlayersNear(
                        (ServerLevel) context.context.be.getLevel(),
                        null,
                        pos.getX(), pos.getY(), pos.getZ(), 50,
                        payload
                );
                PlatePistonLogic.startSpread(context.context.be, context.direction);
            }
            // TODO:
            Data data = (Data) context.plateState.data;
            data.isSpread = setSignalRequest.isPowered;
            context.setChanged();
            return new MicroProcessorWorker.AnswerS2WMessage(request.id, false, setSignalRequest.isPowered);
        }
        return super.request(request, context);
    }
}
