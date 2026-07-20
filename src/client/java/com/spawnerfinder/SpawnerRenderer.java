package com.spawnerfinder;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import java.util.List;

public class SpawnerRenderer implements WorldRenderEvents.BeforeEntities {
    private static final float BEAM_HEIGHT = 256f;
    private static final int BEAM_SEGMENTS = 32;

    private static final RenderLayer LINES_THROUGH_WALLS;

    static {
        var pipeline = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.RENDERTYPE_LINES_SNIPPET)
                .withLocation(Identifier.of("spawnerfinder", "lines_through_walls"))
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                .build()
        );
        RenderSetup setup = RenderSetup.builder(pipeline).build();
        LINES_THROUGH_WALLS = RenderLayer.of("spawnerfinder:lines_through_walls", setup);
    }

    @Override
    public void beforeEntities(WorldRenderContext context) {
        if (!SpawnerFinderMod.enabled) return;
        if (!SpawnerFinderMod.showBeam && !SpawnerFinderMod.showESP) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        Vec3d cameraPos = client.gameRenderer.getCamera().getBlockPos().toCenterPos();
        Matrix4f positionMatrix = context.matrices().peek().getPositionMatrix();
        VertexConsumerProvider vcp = context.consumers();
        VertexConsumer vc = vcp.getBuffer(LINES_THROUGH_WALLS);

        if (SpawnerFinderMod.showSpawner) {
            List<SpawnerInfo> spawners = SpawnerFinderMod.foundSpawners;
            synchronized (spawners) {
                for (SpawnerInfo info : spawners) {
                    BlockPos pos = info.pos();
                    Vec3d center = Vec3d.ofCenter(pos);
                    if (SpawnerFinderMod.showBeam) drawBeam(vc, positionMatrix, center, cameraPos, 0f, 1f, 0.2f);
                    if (SpawnerFinderMod.showESP) drawESPBox(vc, positionMatrix, pos, cameraPos, 0f, 1f, 0.2f);
                }
            }
        }

        if (SpawnerFinderMod.showShulker) {
            List<SpawnerInfo> shulkers = SpawnerFinderMod.foundShulkers;
            synchronized (shulkers) {
                for (SpawnerInfo info : shulkers) {
                    BlockPos pos = info.pos();
                    Vec3d center = Vec3d.ofCenter(pos);
                    if (SpawnerFinderMod.showBeam) drawBeam(vc, positionMatrix, center, cameraPos, 0f, 0.8f, 1f);
                    if (SpawnerFinderMod.showESP) drawESPBox(vc, positionMatrix, pos, cameraPos, 0f, 0.8f, 1f);
                }
            }
        }
        if (SpawnerFinderMod.scanExtra) {
            if (SpawnerFinderMod.showChest) {
                List<SpawnerInfo> chests = SpawnerFinderMod.foundChests;
                synchronized (chests) {
                    for (SpawnerInfo info : chests) {
                        BlockPos pos = info.pos();
                        Vec3d center = Vec3d.ofCenter(pos);
                        if (SpawnerFinderMod.showBeam) drawBeam(vc, positionMatrix, center, cameraPos, 1f, 0.3f, 0.3f);
                        if (SpawnerFinderMod.showESP) drawESPBox(vc, positionMatrix, pos, cameraPos, 1f, 0.3f, 0.3f);
                    }
                }
            }
            if (SpawnerFinderMod.showEnderChest) {
                List<SpawnerInfo> enders = SpawnerFinderMod.foundEnderChests;
                synchronized (enders) {
                    for (SpawnerInfo info : enders) {
                        BlockPos pos = info.pos();
                        Vec3d center = Vec3d.ofCenter(pos);
                        if (SpawnerFinderMod.showBeam) drawBeam(vc, positionMatrix, center, cameraPos, 0.6f, 0.2f, 1f);
                        if (SpawnerFinderMod.showESP) drawESPBox(vc, positionMatrix, pos, cameraPos, 0.6f, 0.2f, 1f);
                    }
                }
            }
            if (SpawnerFinderMod.showPillager) {
                List<SpawnerInfo> pillagers = SpawnerFinderMod.foundPillagers;
                synchronized (pillagers) {
                    for (SpawnerInfo info : pillagers) {
                        BlockPos pos = info.pos();
                        Vec3d center = Vec3d.ofCenter(pos);
                        if (SpawnerFinderMod.showBeam) drawBeam(vc, positionMatrix, center, cameraPos, 1f, 0.6f, 0f);
                        if (SpawnerFinderMod.showESP) drawESPBox(vc, positionMatrix, pos, cameraPos, 1f, 0.6f, 0f);
                    }
                }
            }
            if (SpawnerFinderMod.showStructure) {
                List<SpawnerInfo> structures = SpawnerFinderMod.foundStructures;
                synchronized (structures) {
                    for (SpawnerInfo info : structures) {
                        BlockPos pos = info.pos();
                        Vec3d center = Vec3d.ofCenter(pos);
                        if (SpawnerFinderMod.showBeam) drawBeam(vc, positionMatrix, center, cameraPos, 1f, 0.4f, 1f);
                        if (SpawnerFinderMod.showESP) drawESPBox(vc, positionMatrix, pos, cameraPos, 1f, 0.4f, 1f);
                    }
                }
            }
        }
    }

    private void drawBeam(VertexConsumer vc, Matrix4f matrix, Vec3d center, Vec3d cameraPos, float r, float g, float b) {
        float x = (float)(center.x - cameraPos.x);
        float y = (float)(center.y - cameraPos.y);
        float z = (float)(center.z - cameraPos.z);

        float beamTop = y + BEAM_HEIGHT;

        for (int i = 0; i < BEAM_SEGMENTS; i++) {
            double angle = (i / (double) BEAM_SEGMENTS) * Math.PI * 2;
            double nextAngle = ((i + 1) / (double) BEAM_SEGMENTS) * Math.PI * 2;
            double radius = 0.3 + 0.05 * Math.sin(i * 0.5);

            float px1 = (float)(Math.cos(angle) * radius);
            float pz1 = (float)(Math.sin(angle) * radius);
            float px2 = (float)(Math.cos(nextAngle) * radius);
            float pz2 = (float)(Math.sin(nextAngle) * radius);

            vc.vertex(matrix, x + px1, y, z + pz1).color(r, g, b, 0.8f).normal(0f, 1f, 0f).lineWidth(3.0f);
            vc.vertex(matrix, x + px2, y, z + pz2).color(r, g, b, 0.8f).normal(0f, 1f, 0f).lineWidth(3.0f);

            vc.vertex(matrix, x + px1, beamTop, z + pz1).color(r, g, b, 0.4f).normal(0f, 1f, 0f).lineWidth(3.0f);
            vc.vertex(matrix, x + px2, beamTop, z + pz2).color(r, g, b, 0.4f).normal(0f, 1f, 0f).lineWidth(3.0f);

            vc.vertex(matrix, x + px1, y, z + pz1).color(r, g, b, 0.6f).normal(0f, 1f, 0f).lineWidth(3.0f);
            vc.vertex(matrix, x + px1, beamTop, z + pz1).color(r, g, b, 0.3f).normal(0f, 1f, 0f).lineWidth(3.0f);
        }
    }

    private void drawESPBox(VertexConsumer vc, Matrix4f matrix, BlockPos pos, Vec3d cameraPos, float r, float g, float b) {
        float x1 = (float)(pos.getX() - cameraPos.x);
        float y1 = (float)(pos.getY() - cameraPos.y);
        float z1 = (float)(pos.getZ() - cameraPos.z);
        float x2 = x1 + 1f;
        float y2 = y1 + 1f;
        float z2 = z1 + 1f;

        vc.vertex(matrix, x1, y1, z1).color(r, g, b, 1.0f).normal(0f, 1f, 0f).lineWidth(3.0f);
        vc.vertex(matrix, x2, y1, z1).color(r, g, b, 1.0f).normal(0f, 1f, 0f).lineWidth(3.0f);
        vc.vertex(matrix, x2, y1, z1).color(r, g, b, 1.0f).normal(0f, 1f, 0f).lineWidth(3.0f);
        vc.vertex(matrix, x2, y1, z2).color(r, g, b, 1.0f).normal(0f, 1f, 0f).lineWidth(3.0f);
        vc.vertex(matrix, x2, y1, z2).color(r, g, b, 1.0f).normal(0f, 1f, 0f).lineWidth(3.0f);
        vc.vertex(matrix, x1, y1, z2).color(r, g, b, 1.0f).normal(0f, 1f, 0f).lineWidth(3.0f);
        vc.vertex(matrix, x1, y1, z2).color(r, g, b, 1.0f).normal(0f, 1f, 0f).lineWidth(3.0f);
        vc.vertex(matrix, x1, y1, z1).color(r, g, b, 1.0f).normal(0f, 1f, 0f).lineWidth(3.0f);

        vc.vertex(matrix, x1, y2, z1).color(r, g, b, 1.0f).normal(0f, 1f, 0f).lineWidth(3.0f);
        vc.vertex(matrix, x2, y2, z1).color(r, g, b, 1.0f).normal(0f, 1f, 0f).lineWidth(3.0f);
        vc.vertex(matrix, x2, y2, z1).color(r, g, b, 1.0f).normal(0f, 1f, 0f).lineWidth(3.0f);
        vc.vertex(matrix, x2, y2, z2).color(r, g, b, 1.0f).normal(0f, 1f, 0f).lineWidth(3.0f);
        vc.vertex(matrix, x2, y2, z2).color(r, g, b, 1.0f).normal(0f, 1f, 0f).lineWidth(3.0f);
        vc.vertex(matrix, x1, y2, z2).color(r, g, b, 1.0f).normal(0f, 1f, 0f).lineWidth(3.0f);
        vc.vertex(matrix, x1, y2, z2).color(r, g, b, 1.0f).normal(0f, 1f, 0f).lineWidth(3.0f);
        vc.vertex(matrix, x1, y2, z1).color(r, g, b, 1.0f).normal(0f, 1f, 0f).lineWidth(3.0f);

        vc.vertex(matrix, x1, y1, z1).color(r, g, b, 1.0f).normal(0f, 1f, 0f).lineWidth(3.0f);
        vc.vertex(matrix, x1, y2, z1).color(r, g, b, 1.0f).normal(0f, 1f, 0f).lineWidth(3.0f);
        vc.vertex(matrix, x2, y1, z1).color(r, g, b, 1.0f).normal(0f, 1f, 0f).lineWidth(3.0f);
        vc.vertex(matrix, x2, y2, z1).color(r, g, b, 1.0f).normal(0f, 1f, 0f).lineWidth(3.0f);
        vc.vertex(matrix, x2, y1, z2).color(r, g, b, 1.0f).normal(0f, 1f, 0f).lineWidth(3.0f);
        vc.vertex(matrix, x2, y2, z2).color(r, g, b, 1.0f).normal(0f, 1f, 0f).lineWidth(3.0f);
        vc.vertex(matrix, x1, y1, z2).color(r, g, b, 1.0f).normal(0f, 1f, 0f).lineWidth(3.0f);
        vc.vertex(matrix, x1, y2, z2).color(r, g, b, 1.0f).normal(0f, 1f, 0f).lineWidth(3.0f);
    }
}
