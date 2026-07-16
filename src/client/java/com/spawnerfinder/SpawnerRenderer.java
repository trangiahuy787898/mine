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
        RenderPipeline pipeline = RenderPipelines.register(
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
            for (SpawnerInfo info : SpawnerFinderMod.foundSpawners) {
                Vec3d center = info.centerPos();
                if (SpawnerFinderMod.showBeam) drawBeam(vc, positionMatrix, center, cameraPos, 0f, 1f, 0.2f);
                if (SpawnerFinderMod.showESP) drawESPBoxCentered(vc, positionMatrix, center, cameraPos, 0f, 1f, 0.2f);
            }
        }

        if (SpawnerFinderMod.showChest) {
            for (SpawnerInfo info : SpawnerFinderMod.foundChests) {
                Vec3d center = info.centerPos();
                if (SpawnerFinderMod.showBeam) drawBeam(vc, positionMatrix, center, cameraPos, 1f, 0.4f, 0f);
                if (SpawnerFinderMod.showESP) drawESPBoxCentered(vc, positionMatrix, center, cameraPos, 1f, 0.4f, 0f);
            }
        }

        if (SpawnerFinderMod.showEnderChest) {
            for (SpawnerInfo info : SpawnerFinderMod.foundEnderChests) {
                Vec3d center = info.centerPos();
                if (SpawnerFinderMod.showBeam) drawBeam(vc, positionMatrix, center, cameraPos, 0.6f, 0.1f, 0.9f);
                if (SpawnerFinderMod.showESP) drawESPBoxCentered(vc, positionMatrix, center, cameraPos, 0.6f, 0.1f, 0.9f);
            }
        }

        if (SpawnerFinderMod.showPillager) {
            for (SpawnerInfo info : SpawnerFinderMod.foundPillagers) {
                Vec3d center = info.centerPos();
                if (SpawnerFinderMod.showBeam) drawBeam(vc, positionMatrix, center, cameraPos, 1.0f, 0.2f, 0.2f);
                if (SpawnerFinderMod.showESP) drawESPBoxCentered(vc, positionMatrix, center, cameraPos, 1.0f, 0.2f, 0.2f);
            }
        }

        if (SpawnerFinderMod.showStructure) {
            for (SpawnerInfo info : SpawnerFinderMod.foundStructures) {
                Vec3d center = info.centerPos();
                if (SpawnerFinderMod.showBeam) drawBeam(vc, positionMatrix, center, cameraPos, 0.6f, 0.0f, 1.0f);
                if (SpawnerFinderMod.showESP) drawESPBoxCentered(vc, positionMatrix, center, cameraPos, 0.6f, 0.0f, 1.0f);
            }
        }

        if (SpawnerFinderMod.showShulker) {
            for (SpawnerInfo info : SpawnerFinderMod.foundShulkers) {
                Vec3d center = info.centerPos();
                if (SpawnerFinderMod.showBeam) drawBeam(vc, positionMatrix, center, cameraPos, 0f, 0.8f, 1f);
                if (SpawnerFinderMod.showESP) drawESPBoxCentered(vc, positionMatrix, center, cameraPos, 0f, 0.8f, 1f);
            }
        }
    }

    private void drawBeam(VertexConsumer vc, Matrix4f matrix, Vec3d center, Vec3d cameraPos, float r, float g, float b) {
        float x = (float)(center.x - cameraPos.x);
        float y = (float)(center.y - cameraPos.y);
        float z = (float)(center.z - cameraPos.z);

        float beamTop = y + BEAM_HEIGHT;

        // Outer ring (vòng ngoài to, đậm)
        double outerRadius = 0.55;
        for (int i = 0; i < BEAM_SEGMENTS; i++) {
            double angle = (i / (double) BEAM_SEGMENTS) * Math.PI * 2;
            double nextAngle = ((i + 1) / (double) BEAM_SEGMENTS) * Math.PI * 2;

            float px1 = (float)(Math.cos(angle) * outerRadius);
            float pz1 = (float)(Math.sin(angle) * outerRadius);
            float px2 = (float)(Math.cos(nextAngle) * outerRadius);
            float pz2 = (float)(Math.sin(nextAngle) * outerRadius);

            // Đáy vòng ngoài
            vc.vertex(matrix, x + px1, y, z + pz1).color(r, g, b, 1.0f).normal(0f, 1f, 0f).lineWidth(6.0f);
            vc.vertex(matrix, x + px2, y, z + pz2).color(r, g, b, 1.0f).normal(0f, 1f, 0f).lineWidth(6.0f);

            // Đỉnh vòng ngoài (mờ dần)
            vc.vertex(matrix, x + px1, beamTop, z + pz1).color(r, g, b, 0.5f).normal(0f, 1f, 0f).lineWidth(4.0f);
            vc.vertex(matrix, x + px2, beamTop, z + pz2).color(r, g, b, 0.5f).normal(0f, 1f, 0f).lineWidth(4.0f);

            // Đường dọc từ đáy lên đỉnh
            vc.vertex(matrix, x + px1, y, z + pz1).color(r, g, b, 0.9f).normal(0f, 1f, 0f).lineWidth(6.0f);
            vc.vertex(matrix, x + px1, beamTop, z + pz1).color(r, g, b, 0.3f).normal(0f, 1f, 0f).lineWidth(4.0f);
        }

        // Inner glow ring (vòng trong nhỏ, sáng - tạo hiệu ứng lõi sáng)
        double innerRadius = 0.18;
        for (int i = 0; i < BEAM_SEGMENTS; i++) {
            double angle = (i / (double) BEAM_SEGMENTS) * Math.PI * 2;
            double nextAngle = ((i + 1) / (double) BEAM_SEGMENTS) * Math.PI * 2;

            float px1 = (float)(Math.cos(angle) * innerRadius);
            float pz1 = (float)(Math.sin(angle) * innerRadius);
            float px2 = (float)(Math.cos(nextAngle) * innerRadius);
            float pz2 = (float)(Math.sin(nextAngle) * innerRadius);

            // Lõi đáy
            vc.vertex(matrix, x + px1, y, z + pz1).color(1f, 1f, 1f, 0.9f).normal(0f, 1f, 0f).lineWidth(3.0f);
            vc.vertex(matrix, x + px2, y, z + pz2).color(1f, 1f, 1f, 0.9f).normal(0f, 1f, 0f).lineWidth(3.0f);

            // Lõi đỉnh
            vc.vertex(matrix, x + px1, beamTop, z + pz1).color(r, g, b, 0.7f).normal(0f, 1f, 0f).lineWidth(2.0f);
            vc.vertex(matrix, x + px2, beamTop, z + pz2).color(r, g, b, 0.7f).normal(0f, 1f, 0f).lineWidth(2.0f);
        }
    }

    private void drawESPBoxCentered(VertexConsumer vc, Matrix4f matrix, Vec3d center, Vec3d cameraPos, float r, float g, float b) {
        float x = (float)(center.x - cameraPos.x);
        float y = (float)(center.y - cameraPos.y);
        float z = (float)(center.z - cameraPos.z);
        // Mở rộng box ra 0.05 mỗi phía để dễ nhìn hơn
        float hs = 0.55f;

        float x1 = x - hs;
        float y1 = y - hs;
        float z1 = z - hs;
        float x2 = x + hs;
        float y2 = y + hs;
        float z2 = z + hs;

        // Vẽ mỗi cạnh với lineWidth 6.0f và alpha 1.0 để đậm, rõ
        // Mặt đáy
        vc.vertex(matrix, x1, y1, z1).color(r, g, b, 1.0f).normal(0f, 1f, 0f).lineWidth(6.0f);
        vc.vertex(matrix, x2, y1, z1).color(r, g, b, 1.0f).normal(0f, 1f, 0f).lineWidth(6.0f);
        vc.vertex(matrix, x2, y1, z1).color(r, g, b, 1.0f).normal(0f, 1f, 0f).lineWidth(6.0f);
        vc.vertex(matrix, x2, y1, z2).color(r, g, b, 1.0f).normal(0f, 1f, 0f).lineWidth(6.0f);
        vc.vertex(matrix, x2, y1, z2).color(r, g, b, 1.0f).normal(0f, 1f, 0f).lineWidth(6.0f);
        vc.vertex(matrix, x1, y1, z2).color(r, g, b, 1.0f).normal(0f, 1f, 0f).lineWidth(6.0f);
        vc.vertex(matrix, x1, y1, z2).color(r, g, b, 1.0f).normal(0f, 1f, 0f).lineWidth(6.0f);
        vc.vertex(matrix, x1, y1, z1).color(r, g, b, 1.0f).normal(0f, 1f, 0f).lineWidth(6.0f);

        // Mặt đỉnh
        vc.vertex(matrix, x1, y2, z1).color(r, g, b, 1.0f).normal(0f, 1f, 0f).lineWidth(6.0f);
        vc.vertex(matrix, x2, y2, z1).color(r, g, b, 1.0f).normal(0f, 1f, 0f).lineWidth(6.0f);
        vc.vertex(matrix, x2, y2, z1).color(r, g, b, 1.0f).normal(0f, 1f, 0f).lineWidth(6.0f);
        vc.vertex(matrix, x2, y2, z2).color(r, g, b, 1.0f).normal(0f, 1f, 0f).lineWidth(6.0f);
        vc.vertex(matrix, x2, y2, z2).color(r, g, b, 1.0f).normal(0f, 1f, 0f).lineWidth(6.0f);
        vc.vertex(matrix, x1, y2, z2).color(r, g, b, 1.0f).normal(0f, 1f, 0f).lineWidth(6.0f);
        vc.vertex(matrix, x1, y2, z2).color(r, g, b, 1.0f).normal(0f, 1f, 0f).lineWidth(6.0f);
        vc.vertex(matrix, x1, y2, z1).color(r, g, b, 1.0f).normal(0f, 1f, 0f).lineWidth(6.0f);

        // 4 cạnh dọc nối đáy và đỉnh
        vc.vertex(matrix, x1, y1, z1).color(r, g, b, 1.0f).normal(0f, 1f, 0f).lineWidth(6.0f);
        vc.vertex(matrix, x1, y2, z1).color(r, g, b, 1.0f).normal(0f, 1f, 0f).lineWidth(6.0f);
        vc.vertex(matrix, x2, y1, z1).color(r, g, b, 1.0f).normal(0f, 1f, 0f).lineWidth(6.0f);
        vc.vertex(matrix, x2, y2, z1).color(r, g, b, 1.0f).normal(0f, 1f, 0f).lineWidth(6.0f);
        vc.vertex(matrix, x2, y1, z2).color(r, g, b, 1.0f).normal(0f, 1f, 0f).lineWidth(6.0f);
        vc.vertex(matrix, x2, y2, z2).color(r, g, b, 1.0f).normal(0f, 1f, 0f).lineWidth(6.0f);
        vc.vertex(matrix, x1, y1, z2).color(r, g, b, 1.0f).normal(0f, 1f, 0f).lineWidth(6.0f);
        vc.vertex(matrix, x1, y2, z2).color(r, g, b, 1.0f).normal(0f, 1f, 0f).lineWidth(6.0f);
    }
}
