package ru.terramain.microprocessor.plate.plates.piston;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.MovingPistonBlock;
import net.minecraft.world.level.block.state.BlockState;
import ru.terramain.microprocessor.block.MicroProcessorBlock;
import ru.terramain.microprocessor.block.MicroProcessorBlockEntity;
import ru.terramain.microprocessor.client.MPClientHooks;
import ru.terramain.microprocessor.logic.MicroProcessorContext;
import ru.terramain.microprocessor.plate.PlateActionContext;
import ru.terramain.microprocessor.plate.PlateState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.registries.DeferredBlock;
import ru.terramain.microprocessor.MicroProcessorMod;

public class MicroProcessorPistonHeadBlock extends DirectionalBlock {
    public static final MapCodec<MicroProcessorPistonHeadBlock> CODEC = simpleCodec(MicroProcessorPistonHeadBlock::new);
    public static final EnumProperty<PistonType> TYPE;
    public static final BooleanProperty SHORT;
    private static final VoxelShape[] SHAPES_SHORT;
    private static final VoxelShape[] SHAPES_LONG;

    public MicroProcessorPistonHeadBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(TYPE, PistonType.DEFAULT).setValue(SHORT, false));
    }
    public MicroProcessorPistonHeadBlock() {
        this(Properties.of()
                .mapColor(MapColor.STONE)
                .strength(1.5F)
                .noLootTable()
                .pushReaction(PushReaction.BLOCK)
        );
    }

    @Override protected MapCodec<? extends DirectionalBlock> codec() {
        return CODEC;
    }


    private static VoxelShape[] makeShapes(boolean shortArm) {
        VoxelShape[] shapes = new VoxelShape[Direction.values().length];
        for (Direction facing : Direction.values()) {
            shapes[facing.ordinal()] = makeShape(facing, shortArm);
        }
        return shapes;
    }
    private static VoxelShape makeShape(Direction facing, boolean shortArm) {
        VoxelShape platform = MPClientHooks.box(facing, 4, 4, 0, 12, 12, 2);
        VoxelShape arm      = MPClientHooks.box(facing, 7, 7, 2, 9, 9, 10);
        VoxelShape longArm  = MPClientHooks.box(facing, 6.5, 6.5, 10, 9.5, 9.5, 18);
        VoxelShape shape = Shapes.or(platform, arm);
        if (shortArm) shape = Shapes.or(shape, longArm);
        return shape;
    }

    protected boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return (state.getValue(SHORT) ? SHAPES_SHORT : SHAPES_LONG)[state.getValue(FACING).ordinal()];
    }

    private boolean isMicroProcessorSpreadBase(BlockState headState, LevelReader level, BlockPos headPos) {
        Direction facing = headState.getValue(FACING);
        BlockPos basePos = headPos.relative(facing.getOpposite());
        BlockState baseState = level.getBlockState(basePos);
        if (!baseState.is(MicroProcessorBlock.instance().get())) {
            return false;
        }
        BlockEntity blockEntity = level.getBlockEntity(basePos);
        if (!(blockEntity instanceof MicroProcessorBlockEntity be)) {
            return false;
        }
        PlateState<?, ?> plateState = be.getPlateState(facing);
        if (plateState == null || !(plateState.plate instanceof AbstractPlatePiston plate)) {
            return false;
        }
        if (headState.getValue(TYPE) == PistonType.STICKY != plate.isSticky) {
            return false;
        }
        AbstractPlatePiston.Data data = (AbstractPlatePiston.Data) plateState.data;
        return data.isSpread;
    }

    private boolean canAttachToBase(BlockState headState, LevelReader level, BlockPos headPos) {
        Direction facing = headState.getValue(FACING);
        BlockState baseState = level.getBlockState(headPos.relative(facing.getOpposite()));
        return this.isMicroProcessorSpreadBase(headState, level, headPos);
    }

    @Override public BlockState playerWillDestroy(Level level, BlockPos blockPos, BlockState blockState, Player player) {
        if (!level.isClientSide && player.getAbilities().instabuild) {
            onRemoveSpread(blockState, level, blockPos);
        }
        return super.playerWillDestroy(level, blockPos, blockState, player);
    }
    @Override protected void onRemove(BlockState blockState, Level level, BlockPos blockPos, BlockState newBlockState, boolean isMoving) {
        super.onRemove(blockState, level, blockPos, newBlockState, isMoving);
        if (!blockState.is(newBlockState.getBlock()) && !isMoving) {
            onRemoveSpread(blockState, level, blockPos);
        }
    }
    public void onRemoveSpread(BlockState blockState, Level level, BlockPos blockPos) {
        if (isMicroProcessorSpreadBase(blockState, level, blockPos)) {
            Direction facing = blockState.getValue(FACING);
            BlockPos basePos = blockPos.relative(facing.getOpposite());
            MicroProcessorBlockEntity be = (MicroProcessorBlockEntity) level.getBlockEntity(basePos);
            PlateState<?, ?> plateState = be.getPlateState(facing);
            if (plateState != null && plateState.plate instanceof AbstractPlatePiston pistonPlate) {
                MicroProcessorContext context1 = new MicroProcessorContext(be);
                PlateActionContext<?> context2 = new PlateActionContext<>(plateState, facing, context1);
                pistonPlate.onDestroyHead(context2);
            }
        }
    }

    protected BlockState updateShape(BlockState p_60301_, Direction p_60302_, BlockState p_60303_, LevelAccessor p_60304_, BlockPos p_60305_, BlockPos p_60306_) {
        return p_60302_.getOpposite() == p_60301_.getValue(FACING) && !p_60301_.canSurvive(p_60304_, p_60305_) ? Blocks.AIR.defaultBlockState() : super.updateShape(p_60301_, p_60302_, p_60303_, p_60304_, p_60305_, p_60306_);
    }

    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockState behind = level.getBlockState(pos.relative(facing.getOpposite()));
        return this.canAttachToBase(state, level, pos)
                || behind.is(Blocks.MOVING_PISTON) && behind.getValue(MovingPistonBlock.FACING) == facing;
    }

    protected void neighborChanged(BlockState p_60275_, Level p_60276_, BlockPos p_60277_, Block p_60278_, BlockPos p_60279_, boolean p_60280_) {
        if (p_60275_.canSurvive(p_60276_, p_60277_)) {
            p_60276_.neighborChanged(p_60277_.relative(((Direction)p_60275_.getValue(FACING)).getOpposite()), p_60278_, p_60279_);
        }

    }

    public ItemStack getCloneItemStack(LevelReader p_304638_, BlockPos p_60262_, BlockState p_60263_) {
        return new ItemStack(p_60263_.getValue(TYPE) == PistonType.STICKY ? Blocks.STICKY_PISTON : Blocks.PISTON);
    }

    protected BlockState rotate(BlockState state, Rotation rot) {
        return (BlockState)state.setValue(FACING, rot.rotate((Direction)state.getValue(FACING)));
    }

    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation((Direction)state.getValue(FACING)));
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, TYPE, SHORT);
    }

    protected boolean isPathfindable(BlockState p_60270_, PathComputationType p_60273_) {
        return false;
    }

    static {
        TYPE = BlockStateProperties.PISTON_TYPE;
        SHORT = BlockStateProperties.SHORT;
        SHAPES_SHORT = makeShapes(true);
        SHAPES_LONG = makeShapes(false);
    }

    private static DeferredBlock<MicroProcessorPistonHeadBlock> inst = null;
    public static DeferredBlock<MicroProcessorPistonHeadBlock> instance() {
        if (inst == null) {
            inst = MicroProcessorMod.BLOCKS.register("microprocessor_piston_head", () -> new MicroProcessorPistonHeadBlock());
        }
        return inst;
    }
}
