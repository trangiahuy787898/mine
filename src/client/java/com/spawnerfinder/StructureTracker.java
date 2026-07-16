package com.spawnerfinder;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BrushableBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;

public class StructureTracker {
    private static int tickCounter = 0;

    public static void scan(MinecraftClient client) {
        tickCounter++;
        if (tickCounter % 10 != 0) return;

        ClientWorld world = client.world;
        if (world == null) return;

        SpawnerFinderMod.foundStructures.clear();

        for (BlockEntity be : world.getBlockEntities()) {
            if (be instanceof BrushableBlockEntity brushable) {
                BlockPos pos = brushable.getPos();
                boolean isSuspiciousGravel = brushable.getCachedState().isOf(net.minecraft.block.Blocks.SUSPICIOUS_GRAVEL);

                // Kiểm tra xem block có đang bị ngập nước không
                FluidState fluidBelow = world.getFluidState(pos);
                FluidState fluidAbove = world.getFluidState(pos.up());
                boolean isUnderwater = fluidBelow.isIn(FluidTags.WATER) || fluidAbove.isIn(FluidTags.WATER);

                String type;
                if (isUnderwater) {
                    // Dưới nước: xác định loại underwater ruin
                    type = isSuspiciousGravel ? "underwater_ruin/brick" : "underwater_ruin/big_warm";
                } else {
                    // Trên cạn: suspicious sand/gravel thông thường
                    type = isSuspiciousGravel ? "suspicious_gravel" : "suspicious_sand";
                }
                SpawnerFinderMod.foundStructures.add(new SpawnerInfo(pos, type));
            }
        }
    }
}
