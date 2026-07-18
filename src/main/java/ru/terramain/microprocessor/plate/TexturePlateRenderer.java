package ru.terramain.microprocessor.plate;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public interface TexturePlateRenderer extends PlateRenderer {
    float ONE_PIXEL = 1f / 16f;

    TextureAtlasSprite sprite(PlateActionContext<?> context);

    default void renderPlate(PlateActionContext<?> plateContext, PlateRendererContext rendererContext) {
        renderBase(plateContext, rendererContext);
        renderPlateTexture(sprite(plateContext), rendererContext);
    }

    static void renderPlateTexture(TextureAtlasSprite sprite, PlateRendererContext ctx) {
        renderPlateTextureDepth(sprite, ctx, 0f);
    }

    static void renderPlateTexture(TextureAtlasSprite sprite, PoseStack poseStack, VertexConsumer vertexBuilder,
                                   Direction face, BlockPos pos, int packedLight, int packedOverlay) {
        renderPlateTexture(sprite, contextFrom(poseStack, vertexBuilder, face, pos, packedLight, packedOverlay));
    }

    static void renderPlateTextureDepth(TextureAtlasSprite sprite, PlateRendererContext ctx) {
        renderPlateTextureDepth(sprite, ctx, ONE_PIXEL);
    }

    static void renderPlateTextureDepth(TextureAtlasSprite sprite, PoseStack poseStack, VertexConsumer vertexBuilder,
                                        Direction face, BlockPos pos, int packedLight, int packedOverlay) {
        renderPlateTextureDepth(sprite, contextFrom(poseStack, vertexBuilder, face, pos, packedLight, packedOverlay));
    }

    static void renderPlateTextureDepth(TextureAtlasSprite sprite, PlateRendererContext ctx, float depth) {
        ctx.poseStack.pushPose();

        FaceCoordinates coords = faceCoordinates(ctx.direction, depth);
        addQuad(ctx, coords, sprite.getU0(), sprite.getV0(), sprite.getU1(), sprite.getV1());

        ctx.poseStack.popPose();
    }

    static void renderPlateTextureDepth(TextureAtlasSprite sprite, PoseStack poseStack, VertexConsumer vertexBuilder,
                                        Direction face, BlockPos pos, float depth,
                                        int packedLight, int packedOverlay) {
        renderPlateTextureDepth(sprite, contextFrom(poseStack, vertexBuilder, face, pos, packedLight, packedOverlay), depth);
    }

    float HOLE_MIN = 4f / 16f;
    float HOLE_MAX = 12f / 16f;

    static void renderPlateWallsForDepth(TextureAtlasSprite sprite, PlateRendererContext ctx) {
        renderPlateWallsForDepth(sprite, ctx, ONE_PIXEL);
    }

    static void renderPlateWallsForDepth(TextureAtlasSprite sprite, PoseStack poseStack, VertexConsumer vertexBuilder,
                                       Direction face, BlockPos pos, int packedLight, int packedOverlay) {
        renderPlateWallsForDepth(sprite, contextFrom(poseStack, vertexBuilder, face, pos, packedLight, packedOverlay));
    }

    static void renderPlateWallsForDepth(TextureAtlasSprite sprite, PlateRendererContext ctx, float depth) {
        if (depth <= 0f) {
            return;
        }

        ctx.poseStack.pushPose();
        HoleUv uv = holeUv(sprite);

        switch (ctx.direction) {
            case NORTH -> renderZAxisHoleWalls(ctx, uv, 0f, depth, false);
            case SOUTH -> renderZAxisHoleWalls(ctx, uv, 1f, 1f - depth, true);
            case UP -> renderYAxisHoleWalls(ctx, uv, 1f, 1f - depth, false);
            case DOWN -> renderYAxisHoleWalls(ctx, uv, 0f, depth, true);
            case WEST -> renderXAxisHoleWalls(ctx, uv, 0f, depth, true);
            case EAST -> renderXAxisHoleWalls(ctx, uv, 1f, 1f - depth, false);
        }

        ctx.poseStack.popPose();
    }

    static void renderPlateWallsForDepth(TextureAtlasSprite sprite, PoseStack poseStack, VertexConsumer vertexBuilder,
                                       Direction face, BlockPos pos, float depth,
                                       int packedLight, int packedOverlay) {
        renderPlateWallsForDepth(sprite, contextFrom(poseStack, vertexBuilder, face, pos, packedLight, packedOverlay), depth);
    }

    private static PlateRendererContext contextFrom(PoseStack poseStack, VertexConsumer vertexBuilder, Direction face,
                                                    BlockPos pos, int packedLight, int packedOverlay) {
        return new PlateRendererContext(0f, poseStack, null, vertexBuilder, face, pos, packedLight, packedOverlay);
    }

    private static void renderZAxisHoleWalls(PlateRendererContext ctx, HoleUv uv, float outerZ, float innerZ, boolean flipWinding) {
        renderHoleVEdgeWalls(ctx, HoleDepthAxis.Z, uv, outerZ, innerZ, flipWinding);
        renderHoleUEdgeWalls(ctx, HoleDepthAxis.Z, uv, outerZ, innerZ, flipWinding);
    }

    private static void renderYAxisHoleWalls(PlateRendererContext ctx, HoleUv uv, float outerY, float innerY, boolean flipWinding) {
        renderHoleVEdgeWalls(ctx, HoleDepthAxis.Y, uv, outerY, innerY, flipWinding);
        renderHoleUEdgeWalls(ctx, HoleDepthAxis.Y, uv, outerY, innerY, flipWinding);
    }

    private static void renderXAxisHoleWalls(PlateRendererContext ctx, HoleUv uv, float outerX, float innerX, boolean flipWinding) {
        renderHoleVEdgeWalls(ctx, HoleDepthAxis.X, uv, outerX, innerX, flipWinding);
        renderHoleUEdgeWalls(ctx, HoleDepthAxis.X, uv, outerX, innerX, flipWinding);
    }

    enum HoleDepthAxis {
        Z(Direction.UP, Direction.DOWN, Direction.EAST, Direction.WEST),
        Y(Direction.SOUTH, Direction.NORTH, Direction.EAST, Direction.WEST),
        X(Direction.SOUTH, Direction.NORTH, Direction.UP, Direction.DOWN);

        private final Direction vMinNormal;
        private final Direction vMaxNormal;
        private final Direction uMinNormal;
        private final Direction uMaxNormal;

        HoleDepthAxis(Direction vMinNormal, Direction vMaxNormal, Direction uMinNormal, Direction uMaxNormal) {
            this.vMinNormal = vMinNormal;
            this.vMaxNormal = vMaxNormal;
            this.uMinNormal = uMinNormal;
            this.uMaxNormal = uMaxNormal;
        }
    }

    private static void renderHoleVEdgeWalls(PlateRendererContext ctx, HoleDepthAxis axis, HoleUv uv,
                                             float outer, float inner, boolean flipWinding) {
        addMappedWallQuad(ctx, axis.vMinNormal, axis, flipWinding,
                HOLE_MIN, HOLE_MIN, inner, HOLE_MAX, HOLE_MIN, inner, HOLE_MAX, HOLE_MIN, outer, HOLE_MIN, HOLE_MIN, outer,
                uv.uMin, uv.vFrameMin, uv.uMax, uv.vFrameMin, uv.uMax, uv.vMin, uv.uMin, uv.vMin);
        addMappedWallQuad(ctx, axis.vMaxNormal, axis, flipWinding,
                HOLE_MIN, HOLE_MAX, outer, HOLE_MAX, HOLE_MAX, outer, HOLE_MAX, HOLE_MAX, inner, HOLE_MIN, HOLE_MAX, inner,
                uv.uMin, uv.vMax, uv.uMax, uv.vMax, uv.uMax, uv.vFrameMax, uv.uMin, uv.vFrameMax);
    }

    private static void renderHoleUEdgeWalls(PlateRendererContext ctx, HoleDepthAxis axis, HoleUv uv,
                                            float outer, float inner, boolean flipWinding) {
        addMappedWallQuad(ctx, axis.uMinNormal, axis, flipWinding,
                HOLE_MIN, HOLE_MIN, outer, HOLE_MIN, HOLE_MAX, outer, HOLE_MIN, HOLE_MAX, inner, HOLE_MIN, HOLE_MIN, inner,
                uv.uFrameMin, uv.vMin, uv.uFrameMin, uv.vMax, uv.uMin, uv.vMax, uv.uMin, uv.vMin);
        addMappedWallQuad(ctx, axis.uMaxNormal, axis, flipWinding,
                HOLE_MAX, HOLE_MIN, inner, HOLE_MAX, HOLE_MAX, inner, HOLE_MAX, HOLE_MAX, outer, HOLE_MAX, HOLE_MIN, outer,
                uv.uMax, uv.vMin, uv.uMax, uv.vMax, uv.uFrameMax, uv.vMax, uv.uFrameMax, uv.vMin);
    }

    private static void addMappedWallQuad(
            PlateRendererContext ctx,
            Direction normal,
            HoleDepthAxis axis,
            boolean flipWinding,
            float u1, float v1, float d1,
            float u2, float v2, float d2,
            float u3, float v3, float d3,
            float u4, float v4, float d4,
            float uv1u, float uv1v,
            float uv2u, float uv2v,
            float uv3u, float uv3v,
            float uv4u, float uv4v
    ) {
        if (flipWinding) {
            addWallQuad(ctx, normal,
                    mapCoord(axis, 0, u4, v4, d4), mapCoord(axis, 1, u4, v4, d4), mapCoord(axis, 2, u4, v4, d4),
                    mapCoord(axis, 0, u3, v3, d3), mapCoord(axis, 1, u3, v3, d3), mapCoord(axis, 2, u3, v3, d3),
                    mapCoord(axis, 0, u2, v2, d2), mapCoord(axis, 1, u2, v2, d2), mapCoord(axis, 2, u2, v2, d2),
                    mapCoord(axis, 0, u1, v1, d1), mapCoord(axis, 1, u1, v1, d1), mapCoord(axis, 2, u1, v1, d1),
                    uv4u, uv4v, uv3u, uv3v, uv2u, uv2v, uv1u, uv1v);
            return;
        }
        addWallQuad(ctx, normal,
                mapCoord(axis, 0, u1, v1, d1), mapCoord(axis, 1, u1, v1, d1), mapCoord(axis, 2, u1, v1, d1),
                mapCoord(axis, 0, u2, v2, d2), mapCoord(axis, 1, u2, v2, d2), mapCoord(axis, 2, u2, v2, d2),
                mapCoord(axis, 0, u3, v3, d3), mapCoord(axis, 1, u3, v3, d3), mapCoord(axis, 2, u3, v3, d3),
                mapCoord(axis, 0, u4, v4, d4), mapCoord(axis, 1, u4, v4, d4), mapCoord(axis, 2, u4, v4, d4),
                uv1u, uv1v, uv2u, uv2v, uv3u, uv3v, uv4u, uv4v);
    }

    private static float mapCoord(HoleDepthAxis axis, int blockAxis, float u, float v, float depth) {
        int holeAxis = switch (axis) {
            case Z -> blockAxis;
            case Y -> switch (blockAxis) {
                case 0 -> 0;
                case 1 -> 2;
                case 2 -> 1;
                default -> throw new IllegalStateException();
            };
            case X -> switch (blockAxis) {
                case 0 -> 2;
                case 1 -> 1;
                case 2 -> 0;
                default -> throw new IllegalStateException();
            };
        };
        return switch (holeAxis) {
            case 0 -> u;
            case 1 -> v;
            case 2 -> depth;
            default -> throw new IllegalStateException();
        };
    }

    record HoleUv(
            float uMin, float uMax, float vMin, float vMax,
            float uFrameMin, float uFrameMax, float vFrameMin, float vFrameMax
    ) { }

    private static HoleUv holeUv(TextureAtlasSprite sprite) {
        float u0 = sprite.getU0();
        float v0 = sprite.getV0();
        float spanU = sprite.getU1() - u0;
        float spanV = sprite.getV1() - v0;
        float pixelU = spanU / 16f;
        float pixelV = spanV / 16f;
        float uMin = u0 + spanU * HOLE_MIN;
        float uMax = u0 + spanU * HOLE_MAX;
        float vMin = v0 + spanV * HOLE_MIN;
        float vMax = v0 + spanV * HOLE_MAX;
        return new HoleUv(uMin, uMax, vMin, vMax, uMin - pixelU, uMax + pixelU, vMin - pixelV, vMax + pixelV);
    }

    private static void addWallQuad(
            PlateRendererContext ctx,
            Direction normal,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3,
            float x4, float y4, float z4,
            float u1, float v1,
            float u2, float v2,
            float u3, float v3,
            float u4, float v4
    ) {
        PoseStack.Pose pose = ctx.poseStack.last();
        float nx = normal.getStepX();
        float ny = normal.getStepY();
        float nz = normal.getStepZ();

        addWallVertex(ctx.vertexBuilder, pose, x1, y1, z1, u1, v1, nx, ny, nz, ctx.packedLight, ctx.packedOverlay);
        addWallVertex(ctx.vertexBuilder, pose, x2, y2, z2, u2, v2, nx, ny, nz, ctx.packedLight, ctx.packedOverlay);
        addWallVertex(ctx.vertexBuilder, pose, x3, y3, z3, u3, v3, nx, ny, nz, ctx.packedLight, ctx.packedOverlay);
        addWallVertex(ctx.vertexBuilder, pose, x4, y4, z4, u4, v4, nx, ny, nz, ctx.packedLight, ctx.packedOverlay);
    }

    private static void addWallVertex(
            VertexConsumer builder,
            PoseStack.Pose pose,
            float x, float y, float z,
            float u, float v,
            float nx, float ny, float nz,
            int light,
            int overlay
    ) {
        builder.addVertex(pose, x, y, z)
                .setColor(-1)
                .setUv(u, v)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(pose, nx, ny, nz);
    }

    record FaceCoordinates(float x1, float y1, float z1, float x2, float y2, float z2) { }

    private static FaceCoordinates faceCoordinates(Direction face, float depth) {
        return switch (face) {
            case NORTH -> new FaceCoordinates(0, 0, depth, 1, 1, depth);
            case SOUTH -> new FaceCoordinates(0, 0, 1f - depth, 1, 1, 1f - depth);
            case UP -> new FaceCoordinates(0, 1f - depth, 0, 1, 1f - depth, 1);
            case DOWN -> new FaceCoordinates(0, depth, 0, 1, depth, 1);
            case WEST -> new FaceCoordinates(depth, 0, 0, depth, 1, 1);
            case EAST -> new FaceCoordinates(1f - depth, 0, 0, 1f - depth, 1, 1);
        };
    }

    private static void addQuad(PlateRendererContext ctx, FaceCoordinates coords,
                              float u1, float v1, float u2, float v2) {
        PoseStack.Pose pose = ctx.poseStack.last();
        Direction face = ctx.direction;

        float nx = face.getStepX();
        float ny = face.getStepY();
        float nz = face.getStepZ();

        int light = ctx.packedLight;
        int overlay = ctx.packedOverlay;
        VertexConsumer builder = ctx.vertexBuilder;

        switch (face) {
            case DOWN -> {
                addVertex(builder, pose, coords.x1, coords.y1, coords.z1, u1, v1, nx, ny, nz, light, overlay);
                addVertex(builder, pose, coords.x2, coords.y1, coords.z1, u2, v1, nx, ny, nz, light, overlay);
                addVertex(builder, pose, coords.x2, coords.y1, coords.z2, u2, v2, nx, ny, nz, light, overlay);
                addVertex(builder, pose, coords.x1, coords.y1, coords.z2, u1, v2, nx, ny, nz, light, overlay);
            }
            case UP -> {
                addVertex(builder, pose, coords.x1, coords.y1, coords.z2, u1, v2, nx, ny, nz, light, overlay);
                addVertex(builder, pose, coords.x2, coords.y1, coords.z2, u2, v2, nx, ny, nz, light, overlay);
                addVertex(builder, pose, coords.x2, coords.y1, coords.z1, u2, v1, nx, ny, nz, light, overlay);
                addVertex(builder, pose, coords.x1, coords.y1, coords.z1, u1, v1, nx, ny, nz, light, overlay);
            }
            case NORTH -> {
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
