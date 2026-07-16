package com.spawnerfinder;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.mob.ShulkerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;
import java.util.HashSet;
import java.util.Set;

public class SpawnerTracker {
    private static final int CHUNK_RADIUS = 64;
    private static final int BLOCK_RADIUS = CHUNK_RADIUS * 16;
    private static final Set<String> TARGET_MOBS = new HashSet<>(Set.of(
        "blaze", "zombie", "skeleton", "spider", "cave_spider",
        "silverfish", "magma_cube"
    ));
    private static int tickCounter = 0;

    public static void scan(MinecraftClient client) {
        tickCounter++;
        if (tickCounter % 10 != 0) return;

        ClientWorld world = client.world;
        if (world == null) return;

        var player = client.player;
        if (player == null) return;

        ChunkPos centerChunk = player.getChunkPos();

        SpawnerFinderMod.foundSpawners.clear();
        SpawnerFinderMod.foundShulkers.clear();

        for (int dx = -CHUNK_RADIUS; dx <= CHUNK_RADIUS; dx++) {
            for (int dz = -CHUNK_RADIUS; dz <= CHUNK_RADIUS; dz++) {
                ChunkPos cp = new ChunkPos(centerChunk.x + dx, centerChunk.z + dz);
                if (!world.isChunkLoaded(cp.x, cp.z)) continue;

                WorldChunk chunk = world.getChunk(cp.x, cp.z);
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (be instanceof MobSpawnerBlockEntity spawner) {
                        String mobType = getSpawnerMobType(spawner, world);
                        if (mobType != null && TARGET_MOBS.contains(mobType)) {
                            SpawnerFinderMod.foundSpawners.add(
                                new SpawnerInfo(spawner.getPos(), mobType)
                            );
                        }
                    } else if (be instanceof ShulkerBoxBlockEntity shulker) {
                        SpawnerFinderMod.foundShulkers.add(
                            new SpawnerInfo(shulker.getPos(), "shulker_box")
                        );
                    }
                }
            }
        }

        scanShulkerEntities(world, player.getX(), player.getZ());
    }

    private static void scanShulkerEntities(ClientWorld world, double px, double pz) {
        Box searchBox = new Box(
            px - BLOCK_RADIUS, world.getBottomY(), pz - BLOCK_RADIUS,
            px + BLOCK_RADIUS, 320, pz + BLOCK_RADIUS
        );
        for (ShulkerEntity shulker : world.getEntitiesByClass(ShulkerEntity.class, searchBox, e -> true)) {
            SpawnerFinderMod.foundShulkers.add(
                new SpawnerInfo(shulker.getBlockPos(), "shulker")
            );
        }
    }

    private static String getSpawnerMobType(MobSpawnerBlockEntity spawner, ClientWorld world) {
        try {
            var nbt = spawner.createNbt(world.getRegistryManager());
            if (nbt == null) return null;

            var spawnData = nbt.getCompoundOrEmpty("SpawnData");
            if (spawnData.isEmpty()) return null;

            var entityTag = spawnData.getCompoundOrEmpty("entity");
            if (entityTag.isEmpty()) return null;

            if (!entityTag.contains("id")) return null;

            String id = entityTag.getString("id", "");
            if (id.isEmpty()) return null;
            return id.substring(id.lastIndexOf(':') + 1);
        } catch (Exception e) {
            return null;
        }
    }
}
