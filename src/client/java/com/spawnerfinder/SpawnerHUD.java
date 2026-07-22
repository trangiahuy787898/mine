package com.spawnerfinder;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import java.util.List;

public class SpawnerHUD implements HudRenderCallback {
    @Override
    public void onHudRender(DrawContext context, RenderTickCounter tickCounter) {
        if (!SpawnerFinderMod.enabled && !SpawnerFinderMod.scanExtra) return;

        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null) return;

        int screenWidth = context.getScaledWindowWidth();
        int screenHeight = context.getScaledWindowHeight();
        Vec3d playerPos = new Vec3d(player.getX(), player.getY(), player.getZ());

        try {
            renderCenterArrow(context, player, playerPos, screenWidth, screenHeight);
        } catch (Exception e) {
            renderText(context, client.textRenderer, "§c[SF Error] " + e.getMessage(), 4, 4);
        }

        if (!SpawnerFinderMod.showHUD) return;

        int y = 4;
        boolean any = false;
        y = renderSection(context, client, playerPos, y, SpawnerFinderMod.foundSpawners, SpawnerFinderMod.showSpawner, "§a§l--- Spawner ---", "§a");
        if (y > 4) any = true;
        y = renderSection(context, client, playerPos, y, SpawnerFinderMod.foundShulkers, SpawnerFinderMod.showShulker, "§b§l--- Shulker ---", "§b");
        if (y > 4) any = true;
        if (SpawnerFinderMod.scanExtra) {
            y = renderSection(context, client, playerPos, y, SpawnerFinderMod.foundChests, SpawnerFinderMod.showChest, "§c§l--- Rương ---", "§c");
            if (y > 4) any = true;
            y = renderSection(context, client, playerPos, y, SpawnerFinderMod.foundEnderChests, SpawnerFinderMod.showEnderChest, "§5§l--- Rương Ender ---", "§5");
            if (y > 4) any = true;
            y = renderSection(context, client, playerPos, y, SpawnerFinderMod.foundPillagers, SpawnerFinderMod.showPillager, "§e§l--- Pillager ---", "§e");
            if (y > 4) any = true;
            y = renderSection(context, client, playerPos, y, SpawnerFinderMod.foundStructures, SpawnerFinderMod.showStructure, "§d§l--- Cấu trúc ---", "§d");
        }

        if (!any) {
            renderText(context, client.textRenderer, "§7Không tìm thấy mục tiêu", 4, 4);
            return;
        }

        renderCompass(context, player.getYaw(), playerPos, screenWidth, screenHeight);
    }

    private int renderSection(DrawContext context, MinecraftClient client, Vec3d playerPos,
                               int y, List<SpawnerInfo> list, boolean visible, String header, String color) {
        if (!visible || list.isEmpty()) return y;
        renderText(context, client.textRenderer, header, 4, y);
        y += 10;
        for (SpawnerInfo info : list) {
            y = renderInfo(context, client, playerPos, y, info, color);
        }
        return y;
    }

    private void renderCenterArrow(DrawContext context, ClientPlayerEntity player, Vec3d playerPos,
                                    int screenWidth, int screenHeight) {
        MinecraftClient client = MinecraftClient.getInstance();
        SpawnerInfo target = findNearestAny();
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

        String name = formatName(target);
        String colorPrefix = getColor(target);
        String line1 = String.format("§l%s  %s%s  §e%.0fm", arrow, colorPrefix, name, dist);
        String line2 = String.format("§8%s", target.pos().toShortString());

        int textColor = getTextColor(target);
        context.drawCenteredTextWithShadow(client.textRenderer, Text.literal(line1), cx, cy, textColor);
        context.drawCenteredTextWithShadow(client.textRenderer, Text.literal(line2), cx, cy + 11, 0xAAAAAA);
    }

    private SpawnerInfo findNearestAny() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return null;
        SpawnerInfo nearest = null;
        double nearestDist = Double.MAX_VALUE;
        nearest = findNearestIn(SpawnerFinderMod.foundSpawners, SpawnerFinderMod.showSpawner, nearest, nearestDist, client).nearest;
        var r = findNearestIn(SpawnerFinderMod.foundShulkers, SpawnerFinderMod.showShulker, nearest, nearestDist, client);
        nearest = r.nearest; nearestDist = r.dist;
        if (SpawnerFinderMod.scanExtra) {
            r = findNearestIn(SpawnerFinderMod.foundChests, SpawnerFinderMod.showChest, nearest, nearestDist, client);
            nearest = r.nearest; nearestDist = r.dist;
            r = findNearestIn(SpawnerFinderMod.foundEnderChests, SpawnerFinderMod.showEnderChest, nearest, nearestDist, client);
            nearest = r.nearest; nearestDist = r.dist;
            r = findNearestIn(SpawnerFinderMod.foundPillagers, SpawnerFinderMod.showPillager, nearest, nearestDist, client);
            nearest = r.nearest; nearestDist = r.dist;
            r = findNearestIn(SpawnerFinderMod.foundStructures, SpawnerFinderMod.showStructure, nearest, nearestDist, client);
            nearest = r.nearest; nearestDist = r.dist;
        }
        return nearest;
    }

    private record NearestResult(SpawnerInfo nearest, double dist) {}
    private NearestResult findNearestIn(List<SpawnerInfo> list, boolean visible, SpawnerInfo nearest, double nearestDist, MinecraftClient client) {
        if (!visible) return new NearestResult(nearest, nearestDist);
        for (SpawnerInfo info : list) {
            double dist = client.player.squaredDistanceTo(info.centerPos());
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = info;
            }
        }
        return new NearestResult(nearest, nearestDist);
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

        String name = formatName(info);
        String text = String.format("%s%s %s §r§f| §e%s§fm %s %s",
            colorCode, name, dirArrow,
            distBlocks, pos.toShortString(),
            info.pos().getY() < playerPos.y ? "§b↓" : "§c↑");

        renderText(context, client.textRenderer, text, 4, y);
        return y + 10;
    }

    private void renderCompass(DrawContext context, float playerYaw, Vec3d playerPos,
                                int width, int height) {
        int cx = width / 2;
        int cy = height - 30;
        int compassRadius = 50;

        drawCompassCircle(context, cx - compassRadius, cy - compassRadius, compassRadius * 2, compassRadius * 2);
        context.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, "N", cx, cy - compassRadius - 8, 0xFFFFFF);
        context.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, "S", cx, cy + compassRadius + 2, 0xFFFFFF);
        context.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, "W", cx - compassRadius - 8, cy - 4, 0xFFFFFF);
        context.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, "E", cx + compassRadius + 2, cy - 4, 0xFFFFFF);

        if (SpawnerFinderMod.showSpawner) {
            for (SpawnerInfo info : SpawnerFinderMod.foundSpawners)
                drawCompassDot(context, info, playerPos, playerYaw, cx, cy, compassRadius, 0x00FF00, "S");
        }
        if (SpawnerFinderMod.showShulker) {
            for (SpawnerInfo info : SpawnerFinderMod.foundShulkers)
                drawCompassDot(context, info, playerPos, playerYaw, cx, cy, compassRadius, 0x00CCFF, "B");
        }
        if (SpawnerFinderMod.scanExtra) {
            if (SpawnerFinderMod.showChest) {
                for (SpawnerInfo info : SpawnerFinderMod.foundChests)
                    drawCompassDot(context, info, playerPos, playerYaw, cx, cy, compassRadius, 0xFF4444, "C");
            }
            if (SpawnerFinderMod.showEnderChest) {
                for (SpawnerInfo info : SpawnerFinderMod.foundEnderChests)
                    drawCompassDot(context, info, playerPos, playerYaw, cx, cy, compassRadius, 0xAA44FF, "E");
            }
            if (SpawnerFinderMod.showPillager) {
                for (SpawnerInfo info : SpawnerFinderMod.foundPillagers)
                    drawCompassDot(context, info, playerPos, playerYaw, cx, cy, compassRadius, 0xFFAA00, "P");
            }
            if (SpawnerFinderMod.showStructure) {
                for (SpawnerInfo info : SpawnerFinderMod.foundStructures)
                    drawCompassDot(context, info, playerPos, playerYaw, cx, cy, compassRadius, 0xFF66FF, "?");
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

    private void drawCompassCircle(DrawContext context, int x, int y, int w, int h) {
        context.fill(x, y, x + w, y + h, 0x40FFFFFF);
    }

    private void drawDot(DrawContext context, int x, int y, int w, int h, int color) {
        context.fill(x, y, x + w, y + h, color);
    }

    private void renderText(DrawContext context, TextRenderer renderer, String text, int x, int y) {
        context.drawTextWithShadow(renderer, Text.literal(text), x, y, 0xFFFFFF);
    }

    private String formatName(SpawnerInfo info) {
        return switch (info.mobType()) {
            case "shulker_box" -> "SHULKER BOX";
            case "shulker" -> "SHULKER";
            case "single_chest" -> "RƯƠNG ĐƠN";
            case "double_chest" -> "RƯƠNG ĐÔI";
            case "ender_chest" -> "RƯƠNG ENDER";
            case "pillager" -> "PILLAGER";
            case "suspicious" -> "SUSPICIOUS";
            case "trial_spawner" -> "TRIAL SPAWNER";
            default -> info.mobType().toUpperCase();
        };
    }

    private String getColor(SpawnerInfo info) {
        return switch (info.mobType()) {
            case "shulker_box", "shulker" -> "§b";
            case "single_chest", "double_chest" -> "§c";
            case "ender_chest" -> "§5";
            case "pillager" -> "§e";
            case "suspicious", "trial_spawner" -> "§d";
            default -> "§a";
        };
    }

    private int getTextColor(SpawnerInfo info) {
        return switch (info.mobType()) {
            case "shulker_box", "shulker" -> 0xFF00CCFF;
            case "single_chest", "double_chest" -> 0xFFFF4444;
            case "ender_chest" -> 0xFFAA44FF;
            case "pillager" -> 0xFFFFAA00;
            case "suspicious", "trial_spawner" -> 0xFFFF66FF;
            default -> 0xFF00FF00;
        };
    }
}
