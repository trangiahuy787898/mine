package com.spawnerfinder;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public record SpawnerInfo(BlockPos pos, String mobType, Vec3d centerPos) {
    public SpawnerInfo(BlockPos pos, String mobType) {
        this(pos, mobType, Vec3d.ofCenter(pos));
    }
}
