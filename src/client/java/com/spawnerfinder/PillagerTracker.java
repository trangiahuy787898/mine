package com.spawnerfinder;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.mob.PillagerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class PillagerTracker {
    private static final int CHUNK_RADIUS = 64;
    private static int tickCounter = 0;

    public static void scan(MinecraftClient client) {
        tickCounter++;
        if (tickCounter % 10 != 0) return;

        ClientWorld world = client.world;
        if (world == null) return;

        var player = client.player;
        if (player == null) return;

        SpawnerFinderMod.foundPillagers.clear();

        double px = player.getX();
        double py = player.getY();
        double pz = player.getZ();
        int blockRadius = CHUNK_RADIUS * 16;
        Box searchBox = new Box(
            px - blockRadius, world.getBottomY(), pz - blockRadius,
            px + blockRadius, 320, pz + blockRadius
        );

        for (PillagerEntity e : world.getEntitiesByClass(PillagerEntity.class, searchBox, e -> true)) {
            SpawnerFinderMod.foundPillagers.add(new SpawnerInfo(e.getBlockPos(), "pillager", new Vec3d(e.getX(), e.getY(), e.getZ())));
        }
    }
}
