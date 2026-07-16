package com.spawnerfinder;

import net.minecraft.block.ChestBlock;
import net.minecraft.block.enums.ChestType;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;

public class ChestTracker {
    private static int tickCounter = 0;

    public static void scan(MinecraftClient client) {
        tickCounter++;
        if (tickCounter % 10 != 0) return;

        ClientWorld world = client.world;
        if (world == null) return;

        SpawnerFinderMod.foundChests.clear();
        SpawnerFinderMod.foundEnderChests.clear();

        for (BlockEntity be : world.getBlockEntities()) {
            if (be instanceof ChestBlockEntity chest) {
                BlockPos pos = chest.getPos();
                BlockState state = world.getBlockState(pos);
                if (state.contains(ChestBlock.CHEST_TYPE)) {
                    ChestType type = state.get(ChestBlock.CHEST_TYPE);
                    boolean isDouble = type != ChestType.SINGLE;
                    boolean isTrapped = state.getBlock() == Blocks.TRAPPED_CHEST;
                    String typeStr;
                    if (isTrapped) {
                        typeStr = isDouble ? "double_trapped_chest" : "single_trapped_chest";
                    } else {
                        typeStr = isDouble ? "double_chest" : "single_chest";
                    }
                    SpawnerFinderMod.foundChests.add(new SpawnerInfo(pos, typeStr));
                } else {
                    boolean isTrapped = state.getBlock() == Blocks.TRAPPED_CHEST;
                    SpawnerFinderMod.foundChests.add(new SpawnerInfo(pos, isTrapped ? "single_trapped_chest" : "single_chest"));
                }
            } else if (be instanceof EnderChestBlockEntity ender) {
                SpawnerFinderMod.foundEnderChests.add(new SpawnerInfo(ender.getPos(), "ender_chest"));
            }
        }
    }
}
