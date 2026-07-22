package ru.terramain.microprocessor.client;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class MPClientHooks {

    public static VoxelShape box(Direction facing, double x0, double y0, double z0, double x1, double y1, double z1) {
        return rotate(facing, Block.box(x0, y0, z0, x1, y1, z1));
    }
    public static VoxelShape rotate(Direction facing, VoxelShape shape) {
        return switch (facing) {
            case NORTH -> shape;
            case SOUTH -> rotateY(shape, 180);
            case EAST  -> rotateY(shape, 90);
            case WEST  -> rotateY(shape, 270);
            case DOWN  -> rotateX(shape, 90);
            case UP    -> rotateX(shape, 270);
        };
    }

    private static VoxelShape rotateY(VoxelShape shape, int degrees) {
        VoxelShape result = Shapes.empty();
        for (AABB aabb : shape.toAabbs()) {
            result = Shapes.or(result, Shapes.create(rotateY(aabb, degrees)));
        }
        return result.optimize();
    }
    private static VoxelShape rotateX(VoxelShape shape, int degrees) {
        VoxelShape result = Shapes.empty();
        for (AABB aabb : shape.toAabbs()) {
            result = Shapes.or(result, Shapes.create(rotateX(aabb, degrees)));
        }
        return result.optimize();
    }
    private static AABB rotateY(AABB box, int degrees) {
        return switch (degrees % 360) {
            case 0   -> box;
            case 90  -> new AABB(1 - box.maxZ, box.minY, box.minX, 1 - box.minZ, box.maxY, box.maxX);
            case 180 -> new AABB(1 - box.maxX, box.minY, 1 - box.maxZ, 1 - box.minX, box.maxY, 1 - box.minZ);
            case 270 -> new AABB(box.minZ, box.minY, 1 - box.maxX, box.maxZ, box.maxY, 1 - box.minX);
            default -> throw new RuntimeException("degrees % 90 != null");
        };
    }
    private static AABB rotateX(AABB box, int degrees) {
        return switch (degrees % 360) {
            case 0   -> box;
            case 90  -> new AABB(box.minX, 1 - box.maxZ, box.minY, box.maxX, 1 - box.minZ, box.maxY);
            case 270 -> new AABB(box.minX, box.minZ, 1 - box.maxY, box.maxX, box.maxZ, 1 - box.minY);
            case 180 -> new AABB(box.minX, 1 - box.maxY, 1 - box.maxZ, box.maxX, 1 - box.minY, 1 - box.minZ);
            default -> throw new RuntimeException("degrees % 90 != null");
        };
    }

}
