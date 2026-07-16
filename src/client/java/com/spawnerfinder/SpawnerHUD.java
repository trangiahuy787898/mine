package com.spawnerfinder;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import java.util.List;

public class SpawnerHUD implements HudRenderCallback {
    @Override
    public void onHudRender(DrawContext context, RenderTickCounter tickCounter) {
        if (!SpawnerFinderMod.enabled) return;

        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null) return;

        int screenWidth = context.getScaledWindowWidth();
        int screenHeight = context.getScaledWindowHeight();
        Vec3d playerPos = new Vec3d(player.getX(), player.getY(), player.getZ());

        // === CENTER DIRECTION TEXT (always shows when enabled) ===
        try {
            renderCenterArrow(context, player, playerPos, screenWidth, screenHeight);
        } catch (Exception e) {
            renderText(context, client.textRenderer, "§c[SF Error] " + e.getMessage(), 4, 4);
        }

        // === HUD LIST ===
        if (!SpawnerFinderMod.showHUD) return;

        List<SpawnerInfo> spawners = SpawnerFinderMod.foundSpawners;
        List<SpawnerInfo> shulkers = SpawnerFinderMod.foundShulkers;
        List<SpawnerInfo> chests = SpawnerFinderMod.foundChests;
        List<SpawnerInfo> enderChests = SpawnerFinderMod.foundEnderChests;
        List<SpawnerInfo> pillagers = SpawnerFinderMod.foundPillagers;
        List<SpawnerInfo> structures = SpawnerFinderMod.foundStructures;

        boolean hasChests = SpawnerFinderMod.showChest && !chests.isEmpty();
        boolean hasEnderChests = SpawnerFinderMod.showEnderChest && !enderChests.isEmpty();
        boolean hasPillagers = SpawnerFinderMod.showPillager && !pillagers.isEmpty();
        boolean hasStructures = SpawnerFinderMod.showStructure && !structures.isEmpty();

        if (spawners.isEmpty() && shulkers.isEmpty() && !hasChests && !hasEnderChests && !hasPillagers && !hasStructures) {
            renderText(context, client.textRenderer, "§7Không tìm thấy mục tiêu", 4, 4);
            return;
        }

        int y = 4;
        renderText(context, client.textRenderer, "§6§l=== Spawner Finder ===", 4, y);
        y += 10;
        renderText(context, client.textRenderer, String.format("§7Chests: %d | Ender: %d | Pillager: %d", chests.size(), enderChests.size(), pillagers.size()), 4, y);
        y += 10;

        if (SpawnerFinderMod.showSpawner) {
            for (SpawnerInfo info : spawners) {
                y = renderInfo(context, client, playerPos, y, info, "§a");
            }
        }

        if (SpawnerFinderMod.showChest) {
            renderText(context, client.textRenderer, "§c§l--- Chest Finder ---", 4, y);
            y += 10;
            for (SpawnerInfo info : chests) {
                y = renderInfo(context, client, playerPos, y, info, "§c");
            }
        }

        if (SpawnerFinderMod.showEnderChest) {
            renderText(context, client.textRenderer, "§5§l--- Ender Chest ---", 4, y);
            y += 10;
            for (SpawnerInfo info : enderChests) {
                y = renderInfo(context, client, playerPos, y, info, "§5");
            }
        }

        if (SpawnerFinderMod.showPillager) {
            renderText(context, client.textRenderer, "§e§l--- Pillager ---", 4, y);
            y += 10;
            for (SpawnerInfo info : pillagers) {
                y = renderInfo(context, client, playerPos, y, info, "§e");
            }
        }

        if (SpawnerFinderMod.showStructure) {
            renderText(context, client.textRenderer, "§d§l--- Structure Finder ---", 4, y);
            y += 10;
            for (SpawnerInfo info : structures) {
                y = renderInfo(context, client, playerPos, y, info, "§d");
            }
        }

        if (SpawnerFinderMod.showShulker) {
            renderText(context, client.textRenderer, "§b§l--- Shulker Box ---", 4, y);
            y += 10;
            for (SpawnerInfo info : shulkers) {
                y = renderInfo(context, client, playerPos, y, info, "§b");
            }
        }

        renderCompass(context, player.getYaw(), spawners, shulkers, chests, enderChests, pillagers, structures, playerPos, screenWidth, screenHeight);
    }

    private SpawnerInfo findNearestTarget(ClientPlayerEntity player) {
        SpawnerInfo nearest = null;
        double nearestDist = Double.MAX_VALUE;

        if (SpawnerFinderMod.showSpawner) {
            for (SpawnerInfo info : SpawnerFinderMod.foundSpawners) {
                double dist = player.squaredDistanceTo(info.centerPos());
                if (dist < nearestDist) { nearestDist = dist; nearest = info; }
            }
        }
        if (SpawnerFinderMod.showShulker) {
            for (SpawnerInfo info : SpawnerFinderMod.foundShulkers) {
                double dist = player.squaredDistanceTo(info.centerPos());
                if (dist < nearestDist) { nearestDist = dist; nearest = info; }
            }
        }
        if (SpawnerFinderMod.showChest) {
            for (SpawnerInfo info : SpawnerFinderMod.foundChests) {
                double dist = player.squaredDistanceTo(info.centerPos());
                if (dist < nearestDist) { nearestDist = dist; nearest = info; }
            }
        }
        if (SpawnerFinderMod.showEnderChest) {
            for (SpawnerInfo info : SpawnerFinderMod.foundEnderChests) {
                double dist = player.squaredDistanceTo(info.centerPos());
                if (dist < nearestDist) { nearestDist = dist; nearest = info; }
            }
        }
        if (SpawnerFinderMod.showPillager) {
            for (SpawnerInfo info : SpawnerFinderMod.foundPillagers) {
                double dist = player.squaredDistanceTo(info.centerPos());
                if (dist < nearestDist) { nearestDist = dist; nearest = info; }
            }
        }
        if (SpawnerFinderMod.showStructure) {
            for (SpawnerInfo info : SpawnerFinderMod.foundStructures) {
                double dist = player.squaredDistanceTo(info.centerPos());
                if (dist < nearestDist) { nearestDist = dist; nearest = info; }
            }
        }
        return nearest;
    }

    private void renderCenterArrow(DrawContext context, ClientPlayerEntity player, Vec3d playerPos,
                                    int screenWidth, int screenHeight) {
        MinecraftClient client = MinecraftClient.getInstance();
        SpawnerInfo target = findNearestTarget(player);

        int cx = screenWidth / 2;
        int cy = screenHeight / 2 - 20;

        if (target == null) {
            context.drawCenteredTextWithShadow(client.textRenderer,
                Text.literal("§8[ §7SF Đang quét... §8]"), cx, cy, 0xFFFFFF);
            return;
        }

        Vec3d diff = target.centerPos().subtract(playerPos);
        double angleToTarget = Math.toDegrees(Math.atan2(diff.z, diff.x));
        double angleDiff = angleToTarget - player.getYaw();
        while (angleDiff < -180) angleDiff += 360;
        while (angleDiff > 180) angleDiff -= 360;

        double dist = Math.sqrt(diff.x * diff.x + diff.y * diff.y + diff.z * diff.z);

        String arrow;
        if (angleDiff > -22.5 && angleDiff <= 22.5) arrow = "↑";
        else if (angleDiff > 22.5 && angleDiff <= 67.5) arrow = "↗";
        else if (angleDiff > 67.5 && angleDiff <= 112.5) arrow = "→";
        else if (angleDiff > 112.5 && angleDiff <= 157.5) arrow = "↘";
        else if (angleDiff > -67.5 && angleDiff <= -22.5) arrow = "↖";
        else if (angleDiff > -112.5 && angleDiff <= -67.5) arrow = "←";
        else if (angleDiff > -157.5 && angleDiff <= -112.5) arrow = "↙";
        else arrow = "↓";

        String name;
        String colorPrefix;
        switch (target.mobType()) {
            case "shulker_box", "shulker" -> {
                name = target.mobType().equals("shulker_box") ? "SHULKER BOX" : "SHULKER";
                colorPrefix = "§b";
            }
            case "single_chest" -> { name = "RƯƠNG ĐƠN"; colorPrefix = "§c"; }
            case "double_chest" -> { name = "RƯƠNG ĐÔI"; colorPrefix = "§c"; }
            case "single_trapped_chest" -> { name = "RƯƠNG BẪY ĐƠN"; colorPrefix = "§c"; }
            case "double_trapped_chest" -> { name = "RƯƠNG BẪY ĐÔI"; colorPrefix = "§c"; }
            case "ender_chest" -> { name = "RƯƠNG ENDER"; colorPrefix = "§5"; }
            case "pillager" -> { name = "PILLAGER"; colorPrefix = "§e"; }
            default -> {
                name = formatStructureName(target.mobType());
                colorPrefix = "§d";
            }
        }

        String line1 = String.format("§l%s  %s%s  §e%.0fm", arrow, colorPrefix, name, dist);
        String line2 = String.format("§8%s", target.pos().toShortString());

        int textColor = switch (target.mobType()) {
            case "shulker_box", "shulker" -> 0xFF00CCFF;
            case "single_chest", "double_chest", "single_trapped_chest", "double_trapped_chest" -> 0xFFFF6600;
            case "ender_chest" -> 0xFFAA00FF;
            case "pillager" -> 0xFFFFCC00;
            default -> 0xFFAAFF;
        };
        context.drawCenteredTextWithShadow(client.textRenderer, Text.literal(line1), cx, cy, textColor);
        context.drawCenteredTextWithShadow(client.textRenderer, Text.literal(line2), cx, cy + 11, 0xAAAAAA);
    }

    private int renderInfo(DrawContext context, MinecraftClient client, Vec3d playerPos, int y, SpawnerInfo info, String colorCode) {
        BlockPos pos = info.pos();
        Vec3d targetPos = Vec3d.ofCenter(pos);
        Vec3d diff = targetPos.subtract(playerPos);

        double distance = Math.sqrt(diff.x * diff.x + diff.z * diff.z);
        int distBlocks = (int) Math.round(distance);

        double angleToTarget = Math.toDegrees(Math.atan2(diff.z, diff.x));
        double angleDiff = angleToTarget - client.player.getYaw();
        while (angleDiff < -180) angleDiff += 360;
        while (angleDiff > 180) angleDiff -= 360;

        String dirArrow;
        if (angleDiff > -22.5 && angleDiff <= 22.5) dirArrow = "↑";
        else if (angleDiff > 22.5 && angleDiff <= 67.5) dirArrow = "↗";
        else if (angleDiff > 67.5 && angleDiff <= 112.5) dirArrow = "→";
        else if (angleDiff > 112.5 && angleDiff <= 157.5) dirArrow = "↘";
        else if (angleDiff > -67.5 && angleDiff <= -22.5) dirArrow = "↖";
        else if (angleDiff > -112.5 && angleDiff <= -67.5) dirArrow = "←";
        else if (angleDiff > -157.5 && angleDiff <= -112.5) dirArrow = "↙";
        else dirArrow = "↓";

        String name = switch (info.mobType()) {
            case "shulker_box" -> "SHULKER BOX";
            case "shulker" -> "SHULKER";
            case "single_chest" -> "RƯƠNG ĐƠN";
            case "double_chest" -> "RƯƠNG ĐÔI";
            case "single_trapped_chest" -> "RƯƠNG BẪY ĐƠN";
            case "double_trapped_chest" -> "RƯƠNG BẪY ĐÔI";
            case "ender_chest" -> "RƯƠNG ENDER";
            case "pillager" -> "PILLAGER";
            default -> formatStructureName(info.mobType());
        };

        String text = String.format("%s%s %s §r§f| §e%s§fm %s %s",
            colorCode, name, dirArrow,
            distBlocks, pos.toShortString(),
            info.pos().getY() < playerPos.y ? "§b↓" : "§c↑");

        renderText(context, client.textRenderer, text, 4, y);
        return y + 10;
    }

    private int renderOreInfo(DrawContext context, MinecraftClient client, Vec3d playerPos, int y, BlockPos pos) {
        Vec3d targetPos = Vec3d.ofCenter(pos);
        Vec3d diff = targetPos.subtract(playerPos);

        double distance = Math.sqrt(diff.x * diff.x + diff.z * diff.z);
        int distBlocks = (int) Math.round(distance);

        double angleToTarget = Math.toDegrees(Math.atan2(diff.z, diff.x));
        double angleDiff = angleToTarget - client.player.getYaw();
        while (angleDiff < -180) angleDiff += 360;
        while (angleDiff > 180) angleDiff -= 360;

        String dirArrow;
        if (angleDiff > -22.5 && angleDiff <= 22.5) dirArrow = "↑";
        else if (angleDiff > 22.5 && angleDiff <= 67.5) dirArrow = "↗";
        else if (angleDiff > 67.5 && angleDiff <= 112.5) dirArrow = "→";
        else if (angleDiff > 112.5 && angleDiff <= 157.5) dirArrow = "↘";
        else if (angleDiff > -67.5 && angleDiff <= -22.5) dirArrow = "↖";
        else if (angleDiff > -112.5 && angleDiff <= -67.5) dirArrow = "←";
        else if (angleDiff > -157.5 && angleDiff <= -112.5) dirArrow = "↙";
        else dirArrow = "↓";

        Block block = client.world.getBlockState(pos).getBlock();
        String name = block.getName().getString();

        String text = String.format("%s%s %s §r§f| §e%s§fm %s %s",
            "§e", name, dirArrow,
            distBlocks, pos.toShortString(),
            pos.getY() < playerPos.y ? "§b↓" : "§c↑");

        renderText(context, client.textRenderer, text, 4, y);
        return y + 10;
    }

    private void renderCompass(DrawContext context, float playerYaw, List<SpawnerInfo> spawners,
                                List<SpawnerInfo> shulkers, List<SpawnerInfo> chests,
                                List<SpawnerInfo> enderChests, List<SpawnerInfo> pillagers,
                                List<SpawnerInfo> structures,
                                Vec3d playerPos, int width, int height) {
        int cx = width / 2;
        int cy = height - 30;
        int compassRadius = 50;

        drawCompassCircle(context, cx - compassRadius, cy - compassRadius, compassRadius * 2, compassRadius * 2);
        context.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, "N", cx, cy - compassRadius - 8, 0xFFFFFF);
        context.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, "S", cx, cy + compassRadius + 2, 0xFFFFFF);
        context.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, "W", cx - compassRadius - 8, cy - 4, 0xFFFFFF);
        context.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, "E", cx + compassRadius + 2, cy - 4, 0xFFFFFF);

        if (SpawnerFinderMod.showSpawner) {
            for (SpawnerInfo info : spawners) {
                drawCompassDot(context, info, playerPos, playerYaw, cx, cy, compassRadius, 0x00FF00, "S");
            }
        }

        if (SpawnerFinderMod.showChest) {
            for (SpawnerInfo info : chests) {
                drawCompassDot(context, info, playerPos, playerYaw, cx, cy, compassRadius, 0xFF6600, "C");
            }
        }

        if (SpawnerFinderMod.showEnderChest) {
            for (SpawnerInfo info : enderChests) {
                drawCompassDot(context, info, playerPos, playerYaw, cx, cy, compassRadius, 0xAA00FF, "E");
            }
        }

        if (SpawnerFinderMod.showPillager) {
            for (SpawnerInfo info : pillagers) {
                drawCompassDot(context, info, playerPos, playerYaw, cx, cy, compassRadius, 0xFFFF00, "P");
            }
        }

        if (SpawnerFinderMod.showStructure) {
            for (SpawnerInfo info : structures) {
                drawCompassDot(context, info, playerPos, playerYaw, cx, cy, compassRadius, 0xFF80DF, "T");
            }
        }

        if (SpawnerFinderMod.showShulker) {
            for (SpawnerInfo info : shulkers) {
                drawCompassDot(context, info, playerPos, playerYaw, cx, cy, compassRadius, 0x00CCFF, "B");
            }
        }

        drawDot(context, cx - 2, cy - 2, 4, 4, 0xFFFFFF);
    }

    private void drawCompassDot(DrawContext context, SpawnerInfo info, Vec3d playerPos, float playerYaw,
                                 int cx, int cy, int compassRadius, int color, String label) {
        Vec3d diff = Vec3d.ofCenter(info.pos()).subtract(playerPos);
        double angle = Math.atan2(diff.z, diff.x);
        double playerAngleRad = Math.toRadians(playerYaw);
        double relativeAngle = angle - playerAngleRad;
        int dotX = (int) (Math.sin(relativeAngle) * compassRadius);
        int dotZ = (int) (-Math.cos(relativeAngle) * compassRadius);

        drawDot(context, cx + dotX - 2, cy + dotZ - 2, 4, 4, color);
        context.drawCenteredTextWithShadow(
            MinecraftClient.getInstance().textRenderer,
            label,
            cx + dotX, cy + dotZ - 8, color
        );
    }

    private void drawOreCompassDot(DrawContext context, BlockPos pos, Vec3d playerPos, float playerYaw,
                                    int cx, int cy, int compassRadius) {
        Vec3d diff = Vec3d.ofCenter(pos).subtract(playerPos);
        double angle = Math.atan2(diff.z, diff.x);
        double playerAngleRad = Math.toRadians(playerYaw);
        double relativeAngle = angle - playerAngleRad;
        int dotX = (int) (Math.sin(relativeAngle) * compassRadius);
        int dotZ = (int) (-Math.cos(relativeAngle) * compassRadius);

        double dist = Math.sqrt(diff.x * diff.x + diff.y * diff.y + diff.z * diff.z);
        if (dist > 64) return;
        drawDot(context, cx + dotX - 1, cy + dotZ - 1, 3, 3, 0xFFFF00);
    }

    private void drawCompassCircle(DrawContext context, int x, int y, int w, int h) {
        context.fill(x, y, x + w, y + h, 0x40FFFFFF);
    }

    private void drawDot(DrawContext context, int x, int y, int w, int h, int color) {
        context.fill(x, y, x + w, y + h, color);
    }

    private void renderText(DrawContext context, TextRenderer renderer, String text, int x, int y) {
        context.drawTextWithShadow(renderer, net.minecraft.text.Text.literal(text), x, y, 0xFFFFFF);
    }

    private static String formatStructureName(String mobType) {
        return switch (mobType) {
            case "desert_pyramid" -> "DESERT TEMPLE";
            case "jungle_pyramid" -> "JUNGLE TEMPLE";
            case "pillager_outpost" -> "PILLAGER OUTPOST";
            case "mineshaft" -> "MINESHAFT";
            case "stronghold" -> "STRONGHOLD";
            case "ancient_city" -> "ANCIENT CITY";
            case "trail_ruins" -> "TRAIL RUINS";
            case "trial_chambers" -> "TRIAL CHAMBERS";
            case "mansion" -> "MANSION";
            case "swamp_hut" -> "SWAMP HUT";
            case "igloo" -> "IGLOO";
            case "buried_treasure" -> "BURIED TREASURE";
            case "suspicious_sand" -> "SUSPICIOUS SAND";
            case "suspicious_gravel" -> "SUSPICIOUS GRAVEL";
            case "underwater_ruin/brick" -> "🌊 UNDERWATER RUIN (BRICK)";
            case "underwater_ruin/big_warm" -> "🌊 UNDERWATER RUIN (WARM)";
            case "pillager" -> "PILLAGER";
            case "vindicator" -> "VINDICATOR";
            case "evoker" -> "EVOKER";
            case "witch" -> "WITCH";
            case "illusioner" -> "ILLUSIONER";
            default -> mobType.toUpperCase().replace('_', ' ');
        };
    }
}
