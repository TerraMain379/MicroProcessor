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
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
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

import java.util.Arrays;

public class MicroProcessorPistonHeadBlock extends DirectionalBlock {
    public static final MapCodec<MicroProcessorPistonHeadBlock> CODEC = simpleCodec(MicroProcessorPistonHeadBlock::new);
    public static final EnumProperty<PistonType> TYPE;
    public static final BooleanProperty SHORT;
    public static final float PLATFORM = 4.0F;
    protected static final VoxelShape EAST_AABB;
    protected static final VoxelShape WEST_AABB;
    protected static final VoxelShape SOUTH_AABB;
    protected static final VoxelShape NORTH_AABB;
    protected static final VoxelShape UP_AABB;
    protected static final VoxelShape DOWN_AABB;
    protected static final float AABB_OFFSET = 2.0F;
    protected static final float EDGE_MIN = 6.0F;
    protected static final float EDGE_MAX = 10.0F;
    protected static final VoxelShape UP_ARM_AABB;
    protected static final VoxelShape DOWN_ARM_AABB;
    protected static final VoxelShape SOUTH_ARM_AABB;
    protected static final VoxelShape NORTH_ARM_AABB;
    protected static final VoxelShape EAST_ARM_AABB;
    protected static final VoxelShape WEST_ARM_AABB;
    protected static final VoxelShape SHORT_UP_ARM_AABB;
    protected static final VoxelShape SHORT_DOWN_ARM_AABB;
    protected static final VoxelShape SHORT_SOUTH_ARM_AABB;
    protected static final VoxelShape SHORT_NORTH_ARM_AABB;
    protected static final VoxelShape SHORT_EAST_ARM_AABB;
    protected static final VoxelShape SHORT_WEST_ARM_AABB;
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


    private static VoxelShape[] makeShapes(boolean p_60313_) {
        return Arrays.stream(Direction.values()).map(direction -> calculateShape(direction, p_60313_)).toArray(VoxelShape[]::new);
    }

    private static VoxelShape calculateShape(Direction p_60310_, boolean p_60311_) {
        return switch (p_60310_) {
            case UP -> Shapes.or(UP_AABB, p_60311_ ? SHORT_UP_ARM_AABB : UP_ARM_AABB);
            case NORTH -> Shapes.or(NORTH_AABB, p_60311_ ? SHORT_NORTH_ARM_AABB : NORTH_ARM_AABB);
            case SOUTH -> Shapes.or(SOUTH_AABB, p_60311_ ? SHORT_SOUTH_ARM_AABB : SOUTH_ARM_AABB);
            case WEST -> Shapes.or(WEST_AABB, p_60311_ ? SHORT_WEST_ARM_AABB : WEST_ARM_AABB);
            case EAST -> Shapes.or(EAST_AABB, p_60311_ ? SHORT_EAST_ARM_AABB : EAST_ARM_AABB);
            default -> Shapes.or(DOWN_AABB, p_60311_ ? SHORT_DOWN_ARM_AABB : DOWN_ARM_AABB);
        };
    }

    protected boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return ((Boolean)state.getValue(SHORT) ? SHAPES_SHORT : SHAPES_LONG)[((Direction)state.getValue(FACING)).ordinal()];
    }

    private boolean isFittingBase(BlockState baseState, BlockState extendedState) {
        Block block = baseState.getValue(TYPE) == PistonType.DEFAULT ? Blocks.PISTON : Blocks.STICKY_PISTON;
        return extendedState.is(block) && (Boolean)extendedState.getValue(PistonBaseBlock.EXTENDED) && extendedState.getValue(FACING) == baseState.getValue(FACING);
    }

    public BlockState playerWillDestroy(Level p_60265_, BlockPos p_60266_, BlockState p_60267_, Player p_60268_) {
        if (!p_60265_.isClientSide && p_60268_.getAbilities().instabuild) {
            BlockPos blockpos = p_60266_.relative(((Direction)p_60267_.getValue(FACING)).getOpposite());
            if (this.isFittingBase(p_60267_, p_60265_.getBlockState(blockpos))) {
                p_60265_.destroyBlock(blockpos, false);
            }
        }

        return super.playerWillDestroy(p_60265_, p_60266_, p_60267_, p_60268_);
    }

    protected void onRemove(BlockState p_60282_, Level p_60283_, BlockPos p_60284_, BlockState p_60285_, boolean p_60286_) {
        if (!p_60282_.is(p_60285_.getBlock())) {
            super.onRemove(p_60282_, p_60283_, p_60284_, p_60285_, p_60286_);
            BlockPos blockpos = p_60284_.relative(((Direction)p_60282_.getValue(FACING)).getOpposite());
            if (this.isFittingBase(p_60282_, p_60283_.getBlockState(blockpos))) {
                p_60283_.destroyBlock(blockpos, true);
            }
        }

    }

    protected BlockState updateShape(BlockState p_60301_, Direction p_60302_, BlockState p_60303_, LevelAccessor p_60304_, BlockPos p_60305_, BlockPos p_60306_) {
        return p_60302_.getOpposite() == p_60301_.getValue(FACING) && !p_60301_.canSurvive(p_60304_, p_60305_) ? Blocks.AIR.defaultBlockState() : super.updateShape(p_60301_, p_60302_, p_60303_, p_60304_, p_60305_, p_60306_);
    }

    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockState blockstate = level.getBlockState(pos.relative(((Direction)state.getValue(FACING)).getOpposite()));
        return this.isFittingBase(state, blockstate) || blockstate.is(Blocks.MOVING_PISTON) && blockstate.getValue(FACING) == state.getValue(FACING);
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
        builder.add(new Property[]{FACING, TYPE, SHORT});
    }

    protected boolean isPathfindable(BlockState p_60270_, PathComputationType p_60273_) {
        return false;
    }

    static {
        TYPE = BlockStateProperties.PISTON_TYPE;
        SHORT = BlockStateProperties.SHORT;
        EAST_AABB = Block.box(12.0F, 0.0F, 0.0F, 16.0F, 16.0F, 16.0F);
        WEST_AABB = Block.box(0.0F, 0.0F, 0.0F, 4.0F, 16.0F, 16.0F);
        SOUTH_AABB = Block.box(0.0F, 0.0F, 12.0F, 16.0F, 16.0F, 16.0F);
        NORTH_AABB = Block.box(0.0F, 0.0F, 0.0F, 16.0F, 16.0F, 4.0F);
        UP_AABB = Block.box(0.0F, 12.0F, 0.0F, 16.0F, 16.0F, 16.0F);
        DOWN_AABB = Block.box(0.0F, 0.0F, 0.0F, 16.0F, 4.0F, 16.0F);
        UP_ARM_AABB = Block.box(6.0F, -4.0F, 6.0F, 10.0F, 12.0F, 10.0F);
        DOWN_ARM_AABB = Block.box(6.0F, 4.0F, 6.0F, 10.0F, 20.0F, 10.0F);
        SOUTH_ARM_AABB = Block.box(6.0F, 6.0F, -4.0F, 10.0F, 10.0F, 12.0F);
        NORTH_ARM_AABB = Block.box(6.0F, 6.0F, 4.0F, 10.0F, 10.0F, 20.0F);
        EAST_ARM_AABB = Block.box(-4.0F, 6.0F, 6.0F, 12.0F, 10.0F, 10.0F);
        WEST_ARM_AABB = Block.box(4.0F, 6.0F, 6.0F, 20.0F, 10.0F, 10.0F);
        SHORT_UP_ARM_AABB = Block.box(6.0F, 0.0F, 6.0F, 10.0F, 12.0F, 10.0F);
        SHORT_DOWN_ARM_AABB = Block.box(6.0F, 4.0F, 6.0F, 10.0F, 16.0F, 10.0F);
        SHORT_SOUTH_ARM_AABB = Block.box(6.0F, 6.0F, 0.0F, 10.0F, 10.0F, 12.0F);
        SHORT_NORTH_ARM_AABB = Block.box(6.0F, 6.0F, 4.0F, 10.0F, 10.0F, 16.0F);
        SHORT_EAST_ARM_AABB = Block.box(0.0F, 6.0F, 6.0F, 12.0F, 10.0F, 10.0F);
        SHORT_WEST_ARM_AABB = Block.box(4.0F, 6.0F, 6.0F, 16.0F, 10.0F, 10.0F);
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
