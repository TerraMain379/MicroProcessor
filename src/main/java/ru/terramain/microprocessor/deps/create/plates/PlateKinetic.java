package ru.terramain.microprocessor.deps.create.plates;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredItem;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import ru.terramain.microprocessor.MicroProcessorMod;
import ru.terramain.microprocessor.block.MicroProcessorBlockEntity;
import ru.terramain.microprocessor.js.JsFuture;
import ru.terramain.microprocessor.logic.MicroProcessorException;
import ru.terramain.microprocessor.logic.MicroProcessorWorker;
import ru.terramain.microprocessor.plate.*;

public class PlateKinetic extends Plate<PlateKinetic.Data> {
    public static final String TYPE = "kinetic";

    protected PlateKinetic() {
        super(TYPE, DataCodec.INSTANCE, Item.instance, new Renderer(), jsoGenerator);
    }

    public static class Data implements PlateData {
        public boolean isLocked;
        public boolean isReversed;
        public float speedModifier;

        public Data(boolean isLocked, boolean isReversed, float speedModifier) {
            this.isLocked = isLocked;
            this.isReversed = isReversed;
            this.speedModifier = speedModifier;
        }

        @Override
        public <D extends PlateData> D copy() {
            return (D) new Data(isLocked, isReversed, speedModifier);
        }
    }
    public static class DataCodec implements PlateDataCodec<Data> {
        public static final DataCodec INSTANCE = new DataCodec();
        public static final Codec<Data> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.BOOL.optionalFieldOf("is_locked", false).forGetter(o -> o.isLocked),
                Codec.BOOL.optionalFieldOf("is_reversed", false).forGetter(o -> o.isReversed),
                Codec.FLOAT.optionalFieldOf("speed_modifier", 1.0f).forGetter(o -> o.speedModifier)
        ).apply(instance, Data::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, Data> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, o -> o.isLocked,
                ByteBufCodecs.BOOL, o -> o.isReversed,
                ByteBufCodecs.FLOAT, o -> o.speedModifier,
                Data::new
        );

        @Override
        public Data defaultData() {
            return new Data(false, false, 1.0f);
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
    public static class Item extends AbstractPlateItem<Data, PlateKinetic> {
        public static final DeferredItem<AbstractPlateItem<Data, ?>> instance = MicroProcessorMod.ITEMS.register("plate_create_" + TYPE, () -> new Item());

        public Item(Properties properties) {
            super(properties);
        }
        public Item() {
            this(new Properties());
        }

        @Override
        public PlateKinetic getPlate() {
            return PlateKinetic.instance();
        }
    }
    public static class Renderer implements TexturePlateRenderer {
        public static final ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(MicroProcessorMod.MODID, "block/microprocessor_create_kinetic");

        @Override
        public TextureAtlasSprite sprite(PlateActionContext<?> context) {
            return new Material(InventoryMenu.BLOCK_ATLAS, texture).sprite();
        }

        @Override
        public void renderPlate(PlateActionContext<?> plateContext, PlateRendererContext rendererContext) {
            renderBase(plateContext, rendererContext);
            TexturePlateRenderer.renderPlateTextureDepth(sprite(plateContext), rendererContext);
            TexturePlateRenderer.renderPlateWallsForDepth(base(), rendererContext);
            renderShaft(plateContext, rendererContext);
        }

        public void renderShaft(PlateActionContext<?> plateContext, PlateRendererContext rendererContext) {
            MicroProcessorBlockEntity be = plateContext.context.be;
            PlateKinetic plateKinetic = (PlateKinetic) plateContext.plateState.plate;

//            // for fly-wheel
//            Level level = be.getLevel();
//            if (level != null && VisualizationManager.supportsVisualization(level)) {
//                return;
//            }

            float speed = plateKinetic.getFaceRotationSpeed(plateContext);
            Direction.Axis axis = rendererContext.direction.getAxis();
            BlockState shaftState = KineticBlockEntityRenderer.shaft(axis);

            SuperByteBuffer buffer = CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, shaftState, rendererContext.direction);

            float angle = AnimationTickHolder.getRenderTime(be.getLevel()) * speed * 3f / 10f % 360f;
            if (speed != 0f && be.hasSource()) {
                angle += KineticBlockEntityRenderer.getRotationOffsetForPosition(be, be.getBlockPos(), axis);
            }
            angle = angle / 180f * (float) Math.PI;

            KineticBlockEntityRenderer.kineticRotationTransform(buffer, be, axis, angle, rendererContext.packedLight);
            buffer.renderInto(rendererContext.poseStack, rendererContext.bufferSource.getBuffer(RenderType.solid()));
        }
    }
    public static class Jso extends AbstractJsoPlate {
        @HostAccess.Export public boolean isLocked;
        @HostAccess.Export public boolean isReversed;
        @HostAccess.Export public float speedModifier;

        public Jso(MicroProcessorWorker worker, Direction direction, PlateState<?, ?> plateState) {
            super(worker, PlateKinetic.instance(), direction, plateState);
            this.update(plateState);
        }
        public void update(PlateState<?, ?> plateState1) {
            PlateState<Data, PlateKinetic> plateState = (PlateState<Data, PlateKinetic>) plateState1;
            this.isLocked = plateState.data.isLocked;
            this.isReversed = plateState.data.isReversed;
            this.speedModifier = plateState.data.speedModifier;
        }

        @HostAccess.Export
        public JsFuture<Boolean> setLocked(Value isLocked) {
            if (isLocked.isBoolean()) {
                return setLocked(isLocked.asBoolean());
            }
            else throw new MicroProcessorException("isLocked is not a boolean");
        }
        public JsFuture<Boolean> setLocked(boolean isLocked) {
            return this.worker.dataPool.waitAnswerForW2SRequest(
                    worker, new SetLockedRequestMessage(
                            this.worker.nextId.getAndIncrement(),
                            this.direction,
                            this.plate,
                            isLocked
                    )
            );
        }

        @HostAccess.Export
        public JsFuture<Boolean> setReversed(Value isReversed) {
            if (isReversed.isBoolean()) {
                return setReversed(isReversed.asBoolean());
            }
            else throw new MicroProcessorException("isReversed is not a boolean");
        }
        public JsFuture<Boolean> setReversed(boolean isReversed) {
            return this.worker.dataPool.waitAnswerForW2SRequest(
                    worker, new SetReversedRequestMessage(
                            this.worker.nextId.getAndIncrement(),
                            this.direction,
                            this.plate,
                            isReversed
                    )
            );
        }


        @HostAccess.Export
        public JsFuture<Float> setSpeedModifier(Value speedModifier) {
            if (speedModifier.isNumber()) {
                return setSpeedModifier((float) speedModifier.asDouble());
            }
            else throw new MicroProcessorException("speedModifier is not a number");
        }
        public JsFuture<Float> setSpeedModifier(float speedModifier) {
            return this.worker.dataPool.waitAnswerForW2SRequest(
                    worker, new SetSpeedModifierRequestMessage(
                            this.worker.nextId.getAndIncrement(),
                            this.direction,
                            this.plate,
                            speedModifier
                    )
            );
        }

    }
    public static final AbstractJsoPlateGenerator<Jso> jsoGenerator = Jso::new;


    public static class SetLockedRequestMessage extends MicroProcessorWorker.RequestPlateW2SMessage {
        public boolean isLocked;
        public SetLockedRequestMessage(long id, Direction direction, Plate<?> plate, boolean isLocked) {
            super(id, direction, plate);
            this.isLocked = isLocked;
        }
    }
    public static class SetReversedRequestMessage extends MicroProcessorWorker.RequestPlateW2SMessage {
        public boolean isReversed;
        public SetReversedRequestMessage(long id, Direction direction, Plate<?> plate, boolean isReversed) {
            super(id, direction, plate);
            this.isReversed = isReversed;
        }
    }
    public static class SetSpeedModifierRequestMessage extends MicroProcessorWorker.RequestPlateW2SMessage {
        public float speedModifier;
        public SetSpeedModifierRequestMessage(long id, Direction direction, Plate<?> plate, float speedModifier) {
            super(id, direction, plate);
            this.speedModifier = speedModifier;
        }
    }

    @Override
    public MicroProcessorWorker.AnswerS2WMessage request(MicroProcessorWorker.RequestPlateW2SMessage request, PlateActionContext<?> context) {
        if (request instanceof SetLockedRequestMessage setLockedRequest) {
            Data data = (Data) context.plateState.data;
            data.isLocked = setLockedRequest.isLocked;
            context.setChanged();
            context.context.be.setKineticChanged();
            return new MicroProcessorWorker.AnswerS2WMessage(request.id, false, setLockedRequest.isLocked);
        }
        else if (request instanceof SetReversedRequestMessage setReversedRequest) {
            Data data = (Data) context.plateState.data;
            data.isReversed = setReversedRequest.isReversed;
            context.setChanged();
            context.context.be.setKineticChanged();
            return new MicroProcessorWorker.AnswerS2WMessage(request.id, false, setReversedRequest.isReversed);
        }
        else if (request instanceof SetSpeedModifierRequestMessage setSpeedModifierRequest) {
            Data data = (Data) context.plateState.data;
            // TODO: check mp for contains speedModifier module
            data.speedModifier = setSpeedModifierRequest.speedModifier;
            context.setChanged();
            context.context.be.setKineticChanged();
            return new MicroProcessorWorker.AnswerS2WMessage(request.id, false, setSpeedModifierRequest.speedModifier);
        }
        return super.request(request, context);
    }

    public float getFaceRotationSpeed(PlateActionContext<?> context) {
        Data data = (Data) context.plateState.data;
        if (data.isLocked) return 0f;

        MicroProcessorBlockEntity be = context.context.be;
        float speed = be.getSpeed();
        if (speed == 0f || !be.hasSource()) return 0f;

        speed *= data.speedModifier;
        if (data.isReversed) speed *= -1f;
        return speed;
    }

    @Override public boolean hasShaft(PlateActionContext<?> context) {
        Data data = (Data) context.plateState.data;
        return !data.isLocked;
    }
    @Override public void onPlatePlacement(PlateActionContext<?> context, Player player) {
        super.onPlatePlacement(context, player);
        context.context.be.setKineticChanged();
    }
    @Override public void afterPlateRemoval(PlateActionContext<?> context, PlateState<?, ?> newPlateState, Player player) {
        super.onPlateRemoval(context, newPlateState, player);
        context.context.be.setKineticChanged();
    }

    private static PlateKinetic inst = null;
    public static PlateKinetic instance() {
        if (inst == null) {
            inst = PlateRegister.register(new PlateKinetic());
        }
        return inst;
    }
}
