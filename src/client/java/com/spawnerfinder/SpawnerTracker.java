package com.spawnerfinder;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BrushableBlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.block.entity.TrialSpawnerBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.mob.PillagerEntity;
import net.minecraft.entity.mob.ShulkerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SpawnerTracker {
    public static final int CHUNK_RADIUS = 64;
    private static final int BLOCK_RADIUS = CHUNK_RADIUS * 16;
    private static final Set<String> TARGET_MOBS = new HashSet<>(Set.of(
        "blaze", "zombie", "skeleton", "spider", "cave_spider",
        "silverfish", "magma_cube"
    ));
    private static final int PROBE_WAIT = 2;
    private static final int STEP_SIZE = 80;
    private static final int PROBE_RADIUS = 12;

    private static int tickCounter = 0;
    private static int extraScanTimer = 0;

    // Cache results
    private static final Map<ChunkPos, List<SpawnerInfo>> spawnerCache = new ConcurrentHashMap<>();
    private static final Map<ChunkPos, List<SpawnerInfo>> shulkerCache = new ConcurrentHashMap<>();
    private static final Map<ChunkPos, List<SpawnerInfo>> chestCache = new ConcurrentHashMap<>();
    private static final Map<ChunkPos, List<SpawnerInfo>> enderChestCache = new ConcurrentHashMap<>();
    private static final Map<ChunkPos, List<SpawnerInfo>> pillagerCache = new ConcurrentHashMap<>();
    private static final Map<ChunkPos, List<SpawnerInfo>> structureCache = new ConcurrentHashMap<>();
    private static final Set<ChunkPos> scannedChunks = ConcurrentHashMap.newKeySet();

    // Probe state
    public static volatile boolean positionLocked = false;
    private static boolean probingActive = false;
    // Queue of chunk positions we want to load (outside normal view distance)
    private static final Queue<ChunkPos> probeQueue = new LinkedList<>();
    // Set of chunks currently in the queue (for O(1) lookups)
    private static final Set<ChunkPos> queuedChunks = ConcurrentHashMap.newKeySet();
    private static ChunkPos currentTarget = null;
    private static int waitTicks = 0;
    private static int lastSpawnerCount = 0;
    private static int sortedTimer = 0;

    // Track where SERVER thinks the player is
    private static double serverX, serverZ;

    // ===== MAIN TICK =====

    public static void tick(MinecraftClient client) {
        tickCounter++;
        ClientWorld world = client.world;
        ClientPlayerEntity player = client.player;
        if (world == null || player == null) return;

        ChunkPos center = player.getChunkPos();

        // ==== Spawner + Shulker (F6) ====
        if (SpawnerFinderMod.enabled) {
            scanLoadedInRadius(world, center, PROBE_RADIUS);

            if (tickCounter % 100 == 0) cleanStale(center);
            if (tickCounter % 10 == 0) verifyCachedBlockEntities(world);

            scanShulkerEntities(world, player.getX(), player.getZ());

            if (probingActive) tickProbe(client, world, player, center);
        }

        // ==== Extra scan (F9) — độc lập, không cần F6 ====
        if (SpawnerFinderMod.scanExtra) {
            extraScanTimer++;
            if (extraScanTimer >= 20) {
                extraScanTimer = 0;
                scanExtraLoadedChunks(world, center, PROBE_RADIUS);
            }
            scanPillagerEntities(world, player.getX(), player.getZ());
        }

        rebuildDisplayLists();
    }

    // ===== LOADED CHUNK SCANNING =====

    private static void scanLoadedInRadius(ClientWorld world, ChunkPos center, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                ChunkPos cp = new ChunkPos(center.x + dx, center.z + dz);
                if (scannedChunks.contains(cp)) continue;
                if (!world.isChunkLoaded(cp.x, cp.z)) continue;
                WorldChunk chunk = world.getChunk(cp.x, cp.z);
                scanChunk(chunk, world);
                scannedChunks.add(cp);
                queuedChunks.remove(cp);
            }
        }
    }

    public static void onChunkLoad(WorldChunk chunk, ClientWorld world) {
        ChunkPos cp = chunk.getPos();
        if (SpawnerFinderMod.enabled) {
            if (!scannedChunks.add(cp)) return;
            queuedChunks.remove(cp);
            scanChunk(chunk, world);
        }
        if (SpawnerFinderMod.scanExtra && !SpawnerFinderMod.enabled) {
            for (BlockEntity be : chunk.getBlockEntities().values()) {
                if (be instanceof ChestBlockEntity chest) {
                    chestCache.computeIfAbsent(cp, k -> new ArrayList<>())
                        .add(new SpawnerInfo(chest.getPos(), "single_chest"));
                } else if (be instanceof EnderChestBlockEntity ender) {
                    enderChestCache.computeIfAbsent(cp, k -> new ArrayList<>())
                        .add(new SpawnerInfo(ender.getPos(), "ender_chest"));
                } else if (be instanceof BrushableBlockEntity brush) {
                    structureCache.computeIfAbsent(cp, k -> new ArrayList<>())
                        .add(new SpawnerInfo(brush.getPos(), "suspicious"));
                } else if (be instanceof TrialSpawnerBlockEntity trial) {
                    structureCache.computeIfAbsent(cp, k -> new ArrayList<>())
                        .add(new SpawnerInfo(trial.getPos(), "trial_spawner"));
                }
            }
        }
    }

    public static void scanChunk(WorldChunk chunk, ClientWorld world) {
        for (BlockEntity be : chunk.getBlockEntities().values()) {
            if (be instanceof MobSpawnerBlockEntity spawner) {
                String mobType = getSpawnerMobType(spawner, world);
                if (mobType != null && TARGET_MOBS.contains(mobType)) {
                    spawnerCache.computeIfAbsent(chunk.getPos(), k -> new ArrayList<>())
                        .add(new SpawnerInfo(spawner.getPos(), mobType));
                }
            } else if (be instanceof ShulkerBoxBlockEntity shulker) {
                shulkerCache.computeIfAbsent(chunk.getPos(), k -> new ArrayList<>())
                    .add(new SpawnerInfo(shulker.getPos(), "shulker_box"));
            } else if (SpawnerFinderMod.scanExtra) {
                if (be instanceof ChestBlockEntity chest) {
                    String type = chest.getCachedState().getBlock() instanceof net.minecraft.block.ChestBlock ? "double_chest" : "single_chest";
                    chestCache.computeIfAbsent(chunk.getPos(), k -> new ArrayList<>())
                        .add(new SpawnerInfo(chest.getPos(), type));
                } else if (be instanceof EnderChestBlockEntity ender) {
                    enderChestCache.computeIfAbsent(chunk.getPos(), k -> new ArrayList<>())
                        .add(new SpawnerInfo(ender.getPos(), "ender_chest"));
                } else if (be instanceof BrushableBlockEntity brush) {
                    structureCache.computeIfAbsent(chunk.getPos(), k -> new ArrayList<>())
                        .add(new SpawnerInfo(brush.getPos(), "suspicious"));
                } else if (be instanceof TrialSpawnerBlockEntity trial) {
                    structureCache.computeIfAbsent(chunk.getPos(), k -> new ArrayList<>())
                        .add(new SpawnerInfo(trial.getPos(), "trial_spawner"));
                }
            }
        }
    }

    private static void scanExtraLoadedChunks(ClientWorld world, ChunkPos center, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                ChunkPos cp = new ChunkPos(center.x + dx, center.z + dz);
                if (!world.isChunkLoaded(cp.x, cp.z)) continue;
                WorldChunk chunk = world.getChunk(cp.x, cp.z);
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (be instanceof ChestBlockEntity chest) {
                        chestCache.computeIfAbsent(cp, k -> new ArrayList<>())
                            .add(new SpawnerInfo(chest.getPos(), "single_chest"));
                    } else if (be instanceof EnderChestBlockEntity ender) {
                        enderChestCache.computeIfAbsent(cp, k -> new ArrayList<>())
                            .add(new SpawnerInfo(ender.getPos(), "ender_chest"));
                    } else if (be instanceof BrushableBlockEntity brush) {
                        structureCache.computeIfAbsent(cp, k -> new ArrayList<>())
                            .add(new SpawnerInfo(brush.getPos(), "suspicious"));
                    } else if (be instanceof TrialSpawnerBlockEntity trial) {
                        structureCache.computeIfAbsent(cp, k -> new ArrayList<>())
                            .add(new SpawnerInfo(trial.getPos(), "trial_spawner"));
                    }
                }
            }
        }
    }

    public static void scanPillagerEntities(ClientWorld world, double px, double pz) {
        Box searchBox = new Box(
            px - BLOCK_RADIUS, world.getBottomY(), pz - BLOCK_RADIUS,
            px + BLOCK_RADIUS, 320, pz + BLOCK_RADIUS
        );
        for (PillagerEntity pillager : world.getEntitiesByClass(PillagerEntity.class, searchBox, e -> true)) {
            ChunkPos cp = new ChunkPos(pillager.getBlockPos());
            if (scannedChunks.contains(cp)) continue;
            pillagerCache.computeIfAbsent(cp, k -> new ArrayList<>())
                .add(new SpawnerInfo(pillager.getBlockPos(), "pillager"));
            scannedChunks.add(cp);
            queuedChunks.remove(cp);
        }
    }

    public static void scanShulkerEntities(ClientWorld world, double px, double pz) {
        Box searchBox = new Box(
            px - BLOCK_RADIUS, world.getBottomY(), pz - BLOCK_RADIUS,
            px + BLOCK_RADIUS, 320, pz + BLOCK_RADIUS
        );
        for (ShulkerEntity shulker : world.getEntitiesByClass(ShulkerEntity.class, searchBox, e -> true)) {
            ChunkPos cp = new ChunkPos(shulker.getBlockPos());
            if (scannedChunks.contains(cp)) continue;
            shulkerCache.computeIfAbsent(cp, k -> new ArrayList<>())
                .add(new SpawnerInfo(shulker.getBlockPos(), "shulker"));
            scannedChunks.add(cp);
            queuedChunks.remove(cp);
        }
    }

    private static void rebuildDisplayLists() {
        synchronized (SpawnerFinderMod.foundSpawners) {
            SpawnerFinderMod.foundSpawners.clear();
            for (List<SpawnerInfo> list : spawnerCache.values()) {
                SpawnerFinderMod.foundSpawners.addAll(list);
            }
            lastSpawnerCount = SpawnerFinderMod.foundSpawners.size();
        }
        synchronized (SpawnerFinderMod.foundShulkers) {
            SpawnerFinderMod.foundShulkers.clear();
            for (List<SpawnerInfo> list : shulkerCache.values()) {
                SpawnerFinderMod.foundShulkers.addAll(list);
            }
        }
        synchronized (SpawnerFinderMod.foundChests) {
            SpawnerFinderMod.foundChests.clear();
            for (List<SpawnerInfo> list : chestCache.values()) {
                SpawnerFinderMod.foundChests.addAll(list);
            }
        }
        synchronized (SpawnerFinderMod.foundEnderChests) {
            SpawnerFinderMod.foundEnderChests.clear();
            for (List<SpawnerInfo> list : enderChestCache.values()) {
                SpawnerFinderMod.foundEnderChests.addAll(list);
            }
        }
        synchronized (SpawnerFinderMod.foundPillagers) {
            SpawnerFinderMod.foundPillagers.clear();
            for (List<SpawnerInfo> list : pillagerCache.values()) {
                SpawnerFinderMod.foundPillagers.addAll(list);
            }
        }
        synchronized (SpawnerFinderMod.foundStructures) {
            SpawnerFinderMod.foundStructures.clear();
            for (List<SpawnerInfo> list : structureCache.values()) {
                SpawnerFinderMod.foundStructures.addAll(list);
            }
        }
    }

    private static void cleanStale(ChunkPos center) {
        Iterator<ChunkPos> it = scannedChunks.iterator();
        while (it.hasNext()) {
            ChunkPos cp = it.next();
            int dx = cp.x - center.x;
            int dz = cp.z - center.z;
            if (dx * dx + dz * dz > CHUNK_RADIUS * CHUNK_RADIUS) {
                it.remove();
                spawnerCache.remove(cp);
                shulkerCache.remove(cp);
                chestCache.remove(cp);
                enderChestCache.remove(cp);
                pillagerCache.remove(cp);
                structureCache.remove(cp);
            }
        }
    }

    private static void verifyCachedBlockEntities(ClientWorld world) {
        Set<ChunkPos> loaded = new HashSet<>();
        for (ChunkPos cp : scannedChunks) {
            if (!world.isChunkLoaded(cp.x, cp.z)) continue;
            loaded.add(cp);
        }
        for (ChunkPos cp : loaded) {
            WorldChunk chunk = world.getChunk(cp.x, cp.z);
            List<SpawnerInfo> spawners = spawnerCache.get(cp);
            if (spawners != null) {
                spawners.removeIf(info -> {
                    if (info.mobType().equals("blaze") || info.mobType().equals("zombie") || info.mobType().equals("skeleton") ||
                        info.mobType().equals("spider") || info.mobType().equals("cave_spider") || info.mobType().equals("silverfish") || info.mobType().equals("magma_cube")) {
                        return !(chunk.getBlockEntity(info.pos()) instanceof MobSpawnerBlockEntity);
                    }
                    return false;
                });
                if (spawners.isEmpty()) spawnerCache.remove(cp);
            }
            List<SpawnerInfo> shulkers = shulkerCache.get(cp);
            if (shulkers != null) {
                shulkers.removeIf(info -> !(chunk.getBlockEntity(info.pos()) instanceof ShulkerBoxBlockEntity));
                if (shulkers.isEmpty()) shulkerCache.remove(cp);
            }
            if (SpawnerFinderMod.scanExtra) {
                List<SpawnerInfo> chests = chestCache.get(cp);
                if (chests != null) {
                    chests.removeIf(info -> !(chunk.getBlockEntity(info.pos()) instanceof ChestBlockEntity));
                    if (chests.isEmpty()) chestCache.remove(cp);
                }
                List<SpawnerInfo> enders = enderChestCache.get(cp);
                if (enders != null) {
                    enders.removeIf(info -> !(chunk.getBlockEntity(info.pos()) instanceof EnderChestBlockEntity));
                    if (enders.isEmpty()) enderChestCache.remove(cp);
                }
                List<SpawnerInfo> structures = structureCache.get(cp);
                if (structures != null) {
                    structures.removeIf(info -> {
                        String t = info.mobType();
                        if ("suspicious".equals(t)) return !(chunk.getBlockEntity(info.pos()) instanceof BrushableBlockEntity);
                        if ("trial_spawner".equals(t)) return !(chunk.getBlockEntity(info.pos()) instanceof TrialSpawnerBlockEntity);
                        return false;
                    });
                    if (structures.isEmpty()) structureCache.remove(cp);
                }
            }
        }
    }

    // ===== CHUNK PROBING (beyond server view distance) =====

    public static void startProbing() {
        if (probingActive) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        serverX = client.player.getX();
        serverZ = client.player.getZ();

        // Build initial queue: all chunks in PROBE_RADIUS that aren't scanned
        ChunkPos center = client.player.getChunkPos();
        probeQueue.clear();
        queuedChunks.clear();
        enqueueUnsorted(center);

        probingActive = true;
        positionLocked = true;
        currentTarget = null;
        waitTicks = 0;
        sortedTimer = 0;

        client.player.sendMessage(Text.literal(
            "§6[SpawnerFinder] §aScanning §e" + CHUNK_RADIUS + "x" + CHUNK_RADIUS +
            " §achunks" + " §7(probing " + probeQueue.size() + " unloaded chunks)"),
            true
        );
    }

    public static void stopProbing() {
        if (!probingActive) return;
        probingActive = false;
        positionLocked = false;
        currentTarget = null;
        probeQueue.clear();
        queuedChunks.clear();
        waitTicks = 0;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(Text.literal(
                "§6[SpawnerFinder] §cStopped. Found §e" + lastSpawnerCount + " §cspawners."),
                true
            );
        }
    }

    private static void enqueueUnsorted(ChunkPos center) {
        for (int dx = -CHUNK_RADIUS; dx <= CHUNK_RADIUS; dx++) {
            for (int dz = -CHUNK_RADIUS; dz <= CHUNK_RADIUS; dz++) {
                ChunkPos cp = new ChunkPos(center.x + dx, center.z + dz);
                if (scannedChunks.contains(cp)) continue;
                if (queuedChunks.add(cp)) {
                    probeQueue.add(cp);
                }
            }
        }
    }

    private static void tickProbe(MinecraftClient client, ClientWorld world, ClientPlayerEntity player, ChunkPos center) {
        // Periodically replenish queue as player moves
        sortedTimer++;
        if (sortedTimer >= 100) {
            sortedTimer = 0;
            enqueueUnsorted(center);
        }

        // Waiting for a previous probe target to arrive
        if (waitTicks > 0) {
            waitTicks--;
            if (currentTarget != null && world.isChunkLoaded(currentTarget.x, currentTarget.z)) {
                WorldChunk chunk = world.getChunk(currentTarget.x, currentTarget.z);
                scanChunk(chunk, world);
                scannedChunks.add(currentTarget);
                queuedChunks.remove(currentTarget);
                currentTarget = null;
                waitTicks = 0;
            }
            return;
        }

        // Pull the next chunk to probe, skipping already handled ones
        ChunkPos target = null;
        while (!probeQueue.isEmpty()) {
            target = probeQueue.poll();
            if (target == null) break;
            if (scannedChunks.contains(target)) {
                queuedChunks.remove(target);
                continue;
            }
            if (world.isChunkLoaded(target.x, target.z)) {
                WorldChunk chunk = world.getChunk(target.x, target.z);
                scanChunk(chunk, world);
                scannedChunks.add(target);
                queuedChunks.remove(target);
                continue;
            }
            break;
        }

        if (target == null || scannedChunks.contains(target)) {
            // Queue empty or nothing left to do
            return;
        }

        queuedChunks.remove(target);

        // Compute position: center of the target chunk at player's height
        double targetX = target.x * 16L + 8;
        double targetZ = target.z * 16L + 8;
        double targetY = Math.max(world.getBottomY() + 2, Math.min(318, player.getY()));

        // If the jump from server's current position is too large, take an intermediate step
        double dx = targetX - serverX;
        double dz = targetZ - serverZ;
        double dist = Math.sqrt(dx * dx + dz * dz);

        if (dist > STEP_SIZE) {
            double ratio = STEP_SIZE / dist;
            targetX = serverX + dx * ratio;
            targetZ = serverZ + dz * ratio;
            // Re-queue the real target for later
            if (queuedChunks.add(target)) {
                probeQueue.add(target);
            }
        }

        // Send a fake movement packet so the server loads chunks at that location
        player.networkHandler.sendPacket(
            new PlayerMoveC2SPacket.PositionAndOnGround(targetX, targetY, targetZ, true, false)
        );

        serverX = targetX;
        serverZ = targetZ;
        currentTarget = target;
        waitTicks = PROBE_WAIT;

        // Show progress every 3 seconds (60 ticks)
        if (tickCounter % 60 == 0) {
            int pending = probeQueue.size();
            int done = scannedChunks.size();
            int total = pending + done;
            int pct = total > 0 ? done * 100 / total : 100;
            player.sendMessage(Text.literal(
                "§6[SF] §a" + pct + "% §7(" + done + "/" + total +
                " §espawners: " + lastSpawnerCount + ")"),
                true
            );
        }
    }

    public static boolean isProbingActive() { return probingActive; }
    public static int getProbeProgress() {
        int total = probeQueue.size() + scannedChunks.size();
        return total > 0 ? scannedChunks.size() * 100 / total : 100;
    }
    public static int getPendingCount() { return probeQueue.size(); }
    public static int getScannedCount() { return scannedChunks.size(); }

    // ===== RESET =====

    public static void clearExtraCaches() {
        chestCache.clear();
        enderChestCache.clear();
        pillagerCache.clear();
        structureCache.clear();
    }

    public static void resetCaches() {
        spawnerCache.clear();
        shulkerCache.clear();
        chestCache.clear();
        enderChestCache.clear();
        pillagerCache.clear();
        structureCache.clear();
        scannedChunks.clear();
        probeQueue.clear();
        queuedChunks.clear();
        probingActive = false;
        positionLocked = false;
        currentTarget = null;
        waitTicks = 0;
        rebuildDisplayLists();
    }

    // ===== UTILITY =====

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
