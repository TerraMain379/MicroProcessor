package ru.terramain.microprocessor.plate.plates;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.PistonType;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.registries.DeferredItem;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.graalvm.polyglot.HostAccess;
import ru.terramain.microprocessor.MicroProcessorMod;
import ru.terramain.microprocessor.block.MicroProcessorBlockEntity;
import ru.terramain.microprocessor.block.MicroProcessorBlockEventsManager;
import ru.terramain.microprocessor.js.JsFuture;
import ru.terramain.microprocessor.logic.MicroProcessorWorker;
import ru.terramain.microprocessor.plate.*;
import ru.terramain.microprocessor.pistons.MicroProcessorPistonHeadBlock;
import ru.terramain.microprocessor.pistons.PlatePistonLogic;

import java.util.function.Supplier;

public class AbstractPlatePiston extends Plate<AbstractPlatePiston.Data> {
    public static int BLOCK_EVENT_POWER_ON = MicroProcessorBlockEventsManager.<Direction>registerBlockEvent((be, direction) -> {
        return PlatePistonLogic.handleSignal(be, direction, true);
    }, Direction::get3DDataValue, Direction::from3DDataValue);
    public static int BLOCK_EVENT_POWER_OFF = MicroProcessorBlockEventsManager.<Direction>registerBlockEvent((be, direction) -> {
        return PlatePistonLogic.handleSignal(be, direction, false);
    }, Direction::get3DDataValue, Direction::from3DDataValue);


    public boolean isSticky;
    public Supplier<AbstractPlatePiston> getPlateMethod;

    protected AbstractPlatePiston(String type, String texturePath, Supplier<AbstractPlatePiston> getPlateMethod, boolean isSticky) {
        super(type, DataCodec.INSTANCE, Item.register(type, getPlateMethod), new Renderer(texturePath), createJsoGenerator(getPlateMethod));
        this.isSticky = isSticky;
        this.getPlateMethod = getPlateMethod;
    }

    public static class Data implements PlateData {
        public boolean isPowered;
        public boolean isSpread;
        public int moveDelta; // 0|1|2

        public Data(boolean isPowered, boolean isSpread, int moveDelta) {
            this.isPowered = isPowered;
            this.isSpread = isSpread;
            this.moveDelta = moveDelta;
        }

        @Override
        public <D extends PlateData> D copy() {
            return (D) new Data(isPowered, isSpread, moveDelta);
        }
    }
    public static class DataCodec implements PlateDataCodec<Data> {
        public static final DataCodec INSTANCE = new DataCodec();
        public static final Codec<Data> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.BOOL.optionalFieldOf("is_powered", false).forGetter(o -> o.isPowered),
                Codec.BOOL.optionalFieldOf("is_spread", false).forGetter(o -> o.isSpread),
                Codec.INT.optionalFieldOf("move_delta", 0).forGetter(o -> o.moveDelta)
        ).apply(instance, Data::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, Data> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, o -> o.isPowered,
                ByteBufCodecs.BOOL, o -> o.isSpread,
                ByteBufCodecs.INT, o -> o.moveDelta,
                Data::new
        );

        @Override
        public Data defaultData() {
            return new Data(false, false, 0);
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
    public static class Renderer implements TexturePlateRenderer {
        public final String texturePath;
        public final ResourceLocation texture;

        public Renderer(String texturePath) {
            this.texturePath = texturePath;
            this.texture = ResourceLocation.fromNamespaceAndPath(MicroProcessorMod.MODID, texturePath);
        }

        @Override
        public TextureAtlasSprite sprite(PlateActionContext<?> context) {
            return new Material(InventoryMenu.BLOCK_ATLAS, texture).sprite();
        }

        @Override
        public void renderPlate(PlateActionContext<?> plateContext, PlateRendererContext rendererContext) {
            Data data = (Data) plateContext.plateState.data;
            AbstractPlatePiston plate = (AbstractPlatePiston)  plateContext.plateState.plate;

            renderBase(plateContext, rendererContext);
            if (data.isSpread || data.moveDelta > 0) {
                TexturePlateRenderer.renderPlateTextureDepth(
                        NullPlate.renderer.sprite(plateContext),
                        rendererContext,
                        ONE_PIXEL*2
                );
                TexturePlateRenderer.renderPlateWallsForDepth(
                        base(),
                        rendererContext,
                        ONE_PIXEL*2
                );
            }
            else {
                TexturePlateRenderer.renderPlateTexture(
                        sprite(plateContext),
                        rendererContext
                );
            }

            Level level = plateContext.context.be.getLevel();
            if (level == null || rendererContext.bufferSource == null) {
                return;
            }

            if (data.moveDelta > 0 && !data.isSpread) {
                float progress = (data.moveDelta - rendererContext.partialTick) / 2;
                rendererContext.poseStack.pushPose();
                rendererContext.poseStack.translate(
                        plateContext.direction.getStepX() * progress,
                        plateContext.direction.getStepY() * progress,
                        plateContext.direction.getStepZ() * progress
                );
                BlockState headState = PlatePistonLogic.HEAD_BLOCK.get().defaultBlockState()
                        .setValue(MicroProcessorPistonHeadBlock.FACING, plateContext.direction)
                        .setValue(MicroProcessorPistonHeadBlock.TYPE, plate.isSticky ? PistonType.STICKY : PistonType.DEFAULT)
                        .setValue(MicroProcessorPistonHeadBlock.SHORT, progress <= 0.5f);
                ClientHooks.renderPistonMovedBlocks(
                        plateContext.context.be.getBlockPos().relative(rendererContext.direction),
                        headState,
                        rendererContext.poseStack,
                        rendererContext.bufferSource,
                        plateContext.context.be.getLevel(),
                        false,
                        rendererContext.packedOverlay,
                        Minecraft.getInstance().getBlockRenderer()
                );
                rendererContext.poseStack.popPose();
            }
        }
    }
    public static class Jso extends AbstractJsoPlate {
        @HostAccess.Export public boolean isPowered;
        @HostAccess.Export public boolean isSpread;
        @HostAccess.Export public boolean inMove;

        public Jso(MicroProcessorWorker worker, Direction direction, PlateState<?, ?> plateState, Supplier<AbstractPlatePiston> getPlateMethod) {
            super(worker, getPlateMethod.get(), direction, plateState);
            this.update(plateState);
        }
        public void update(PlateState<?, ?> plateState1) {
            PlateState<Data, AbstractPlatePiston> plateState = (PlateState<Data, AbstractPlatePiston>) plateState1;
            this.isPowered = plateState.data.isPowered;
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
    public static class SetPoweredResultEventMessage extends MicroProcessorWorker.PlateEventS2WMessage {
        public SetPoweredResultEventMessage(Direction direction, boolean isMoved) {
            super(direction, "set_powered_result", new Object[]{ isMoved });
        }
    }
    @Override public MicroProcessorWorker.AnswerS2WMessage request(MicroProcessorWorker.RequestPlateW2SMessage request, PlateActionContext<?> context) {
        if (request instanceof CheckForMoveRequestMessage) {
            // TODO:
            return new MicroProcessorWorker.AnswerS2WMessage(request.id, false, true);
        }
        else if (request instanceof SetPoweredRequestMessage setSignalRequest) {
            MicroProcessorBlockEntity be = context.context.be;
            PlatePistonLogic.sendHandlingSignal(be, context.direction, setSignalRequest.isPowered);
//            PlatePistonLogic.handleSignal(context.context.be, context.direction, setSignalRequest.isPowered);
//            Data data = (Data) context.plateState.data; data.isSpread = setSignalRequest.isPowered;
//            context.setChanged();
            return new MicroProcessorWorker.AnswerS2WMessage(request.id, false, null);
        }
        return super.request(request, context);
    }

    @Override public void onTick(PlateActionContext<?> context) {
        super.onTick(context);
        Data data = (Data) context.plateState.data;
        if (data.moveDelta > 0) {
            data.moveDelta--;
            context.setChanged();
        }
    }

    @Override
    public void checkMovable(PlateActionContext<?> context, MutableBoolean movable) {
        Data data = (Data) context.plateState.data;
        if (data.isSpread || data.moveDelta > 0) movable.setValue(false);
    }

    public void onDestroyHead(PlateActionContext<?> context) {
        context.context.be.takeOutPlate(context.direction, null);
    }
}
