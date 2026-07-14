package ru.terramain.microprocessor.plate;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public interface TexturePlateRenderer extends PlateRenderer {
    TextureAtlasSprite sprite(PlateActionContext<?> context);

    default void renderPlate(PlateActionContext<?> context, PoseStack poseStack, VertexConsumer vertexBuilder,
                             Direction face, BlockPos pos, int packedLight, int packedOverlay) {
        renderBase(poseStack, vertexBuilder, face, pos, packedLight, packedOverlay);
        renderPlateTexture(sprite(context), poseStack, vertexBuilder, face, pos, packedLight, packedOverlay);
    }

    public static void  renderPlateTexture(TextureAtlasSprite sprite, PoseStack poseStack, VertexConsumer vertexBuilder,
                                           Direction face, BlockPos pos, int packedLight, int packedOverlay) {
        poseStack.pushPose();

        // Вычисляем координаты квадрата
        FaceCoordinates coords = calculateFaceCoordinates(face);

        // Получаем UV-координаты текстуры
        float u1 = sprite.getU0();
        float v1 = sprite.getV0();
        float u2 = sprite.getU1();
        float v2 = sprite.getV1();

        addQuad(vertexBuilder, poseStack, coords, u1, v1, u2, v2, face, packedLight, packedOverlay);

        poseStack.popPose();
    }

    record FaceCoordinates(float x1, float y1, float z1, float x2, float y2, float z2) { }

    private static FaceCoordinates calculateFaceCoordinates(Direction face) {
        return switch (face) {
            case DOWN -> new FaceCoordinates(0, 0, 0, 1, 0, 1);
            case UP -> new FaceCoordinates(0, 1, 0, 1, 1, 1);
            case NORTH -> new FaceCoordinates(0, 0, 0, 1, 1, 0);
            case SOUTH -> new FaceCoordinates(0, 0, 1, 1, 1, 1);
            case WEST -> new FaceCoordinates(0, 0, 0, 0, 1, 1);
            case EAST -> new FaceCoordinates(1, 0, 0, 1, 1, 1);
        };
    }

    private static void addQuad(VertexConsumer builder, PoseStack poseStack, FaceCoordinates coords,
                         float u1, float v1, float u2, float v2, Direction face, int light, int overlay) {
        PoseStack.Pose pose = poseStack.last();

        // Нормаль грани
        float nx = face.getStepX();
        float ny = face.getStepY();
        float nz = face.getStepZ();

        // Добавляем вершины против часовой стрелки (если смотреть снаружи)
        switch (face) {
            case DOWN -> {
                addVertex(builder, pose, coords.x1, coords.y1, coords.z1, u1, v1, nx, ny, nz, light, overlay);
                addVertex(builder, pose, coords.x2, coords.y1, coords.z1, u2, v1, nx, ny, nz, light, overlay);
                addVertex(builder, pose, coords.x2, coords.y1, coords.z2, u2, v2, nx, ny, nz, light, overlay);
                addVertex(builder, pose, coords.x1, coords.y1, coords.z2, u1, v2, nx, ny, nz, light, overlay);
            }
            case UP -> {
                // Обратный порядок для правильной ориентации
                addVertex(builder, pose, coords.x1, coords.y1, coords.z2, u1, v2, nx, ny, nz, light, overlay);
                addVertex(builder, pose, coords.x2, coords.y1, coords.z2, u2, v2, nx, ny, nz, light, overlay);
                addVertex(builder, pose, coords.x2, coords.y1, coords.z1, u2, v1, nx, ny, nz, light, overlay);
                addVertex(builder, pose, coords.x1, coords.y1, coords.z1, u1, v1, nx, ny, nz, light, overlay);
            }
            case NORTH -> {
                // Обратный порядок для правильной ориентации
                addVertex(builder, pose, coords.x1, coords.y2, coords.z1, u1, v1, nx, ny, nz, light, overlay);
                addVertex(builder, pose, coords.x2, coords.y2, coords.z1, u2, v1, nx, ny, nz, light, overlay);
                addVertex(builder, pose, coords.x2, coords.y1, coords.z1, u2, v2, nx, ny, nz, light, overlay);
                addVertex(builder, pose, coords.x1, coords.y1, coords.z1, u1, v2, nx, ny, nz, light, overlay);
            }
            case SOUTH -> {
                addVertex(builder, pose, coords.x1, coords.y1, coords.z1, u1, v2, nx, ny, nz, light, overlay);
                addVertex(builder, pose, coords.x2, coords.y1, coords.z1, u2, v2, nx, ny, nz, light, overlay);
                addVertex(builder, pose, coords.x2, coords.y2, coords.z1, u2, v1, nx, ny, nz, light, overlay);
                addVertex(builder, pose, coords.x1, coords.y2, coords.z1, u1, v1, nx, ny, nz, light, overlay);
            }
            case WEST -> {
                // Обратный порядок для правильной ориентации
                addVertex(builder, pose, coords.x1, coords.y1, coords.z2, u2, v2, nx, ny, nz, light, overlay);
                addVertex(builder, pose, coords.x1, coords.y2, coords.z2, u2, v1, nx, ny, nz, light, overlay);
                addVertex(builder, pose, coords.x1, coords.y2, coords.z1, u1, v1, nx, ny, nz, light, overlay);
                addVertex(builder, pose, coords.x1, coords.y1, coords.z1, u1, v2, nx, ny, nz, light, overlay);
            }
            case EAST -> {
                addVertex(builder, pose, coords.x1, coords.y1, coords.z1, u1, v2, nx, ny, nz, light, overlay);
                addVertex(builder, pose, coords.x1, coords.y2, coords.z1, u1, v1, nx, ny, nz, light, overlay);
                addVertex(builder, pose, coords.x1, coords.y2, coords.z2, u2, v1, nx, ny, nz, light, overlay);
                addVertex(builder, pose, coords.x1, coords.y1, coords.z2, u2, v2, nx, ny, nz, light, overlay);
            }
        }
    }

    private static void addVertex(VertexConsumer builder, PoseStack.Pose pose, float x, float y, float z,
                           float u, float v, float nx, float ny, float nz, int light, int overlay) {
        builder.addVertex(pose, x, y, z)
                .setColor(-1)
                .setUv(u, v)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(pose, nx, ny, nz);
    }
}
