package com.spawnerfinder;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

public class InventoryUI {
    private static final int BTN_SIZE = 12;
    private static final int BTN_GAP = 2;

    public static void register() {
    }

    public static void onRender(HandledScreen screen, int x, int y, int backgroundWidth, int backgroundHeight, DrawContext context, int mouseX, int mouseY, float delta) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null) return;

        ScreenHandler handler = screen.getScreenHandler();
        boolean hasContainer = hasContainerSlots(handler, player);

        if (hasContainer) {
            drawButtonPair(context, client, handler, player, x, y, true, mouseX, mouseY);
        }
        drawButtonPair(context, client, handler, player, x, y, false, mouseX, mouseY);

        if (SpawnerFinderMod.enabled) {
            drawCenterArrow(context, client, player);
        }
    }

    public static boolean onMouseClicked(int x, int y, int backgroundWidth, int backgroundHeight, double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        MinecraftClient client = MinecraftClient.getInstance();
        if (!(client.currentScreen instanceof HandledScreen screen)) return false;
        if (client.player == null) return false;

        ScreenHandler handler = screen.getScreenHandler();
        boolean hasContainer = hasContainerSlots(handler, client.player);

        if (hasContainer) {
            int[] bounds = getGroupBounds(handler, client.player, true);
            if (bounds != null) {
                int btnX = x + bounds[2] - BTN_SIZE * 2 - BTN_GAP;
                int btnY = y + bounds[1] - BTN_SIZE - 2;
                if (isInside(mouseX, mouseY, btnX, btnY, BTN_SIZE, BTN_SIZE)) {
                    InventoryHelper.sortInventory(client, InventoryHelper.Target.CONTAINER);
                    return true;
                }
                if (isInside(mouseX, mouseY, btnX + BTN_SIZE + BTN_GAP, btnY, BTN_SIZE, BTN_SIZE)) {
                    InventoryHelper.throwAll(client, InventoryHelper.Target.CONTAINER);
                    return true;
                }
            }
        }

        {
            int[] bounds = getGroupBounds(handler, client.player, false);
            if (bounds != null) {
                int btnX = x + bounds[2] - BTN_SIZE * 2 - BTN_GAP;
                int btnY = y + bounds[1] - BTN_SIZE - 2;
                if (isInside(mouseX, mouseY, btnX, btnY, BTN_SIZE, BTN_SIZE)) {
                    InventoryHelper.sortInventory(client, InventoryHelper.Target.PLAYER);
                    return true;
                }
                if (isInside(mouseX, mouseY, btnX + BTN_SIZE + BTN_GAP, btnY, BTN_SIZE, BTN_SIZE)) {
                    InventoryHelper.throwAll(client, InventoryHelper.Target.PLAYER);
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean hasContainerSlots(ScreenHandler handler, ClientPlayerEntity player) {
        for (Slot s : handler.slots) {
            if (s.inventory != player.getInventory()) return true;
        }
        return false;
    }

    private static int[] getGroupBounds(ScreenHandler handler, ClientPlayerEntity player, boolean container) {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
        boolean found = false;
        for (Slot slot : handler.slots) {
            boolean matches = container
                ? slot.inventory != player.getInventory()
                : (slot.inventory == player.getInventory() && slot.getIndex() < 36);
            if (matches) {
                minX = Math.min(minX, slot.x);
                minY = Math.min(minY, slot.y);
                maxX = Math.max(maxX, slot.x + 18);
                maxY = Math.max(maxY, slot.y + 18);
                found = true;
            }
        }
        return found ? new int[]{minX, minY, maxX, maxY} : null;
    }

    private static void drawButtonPair(DrawContext context, MinecraftClient client, ScreenHandler handler, ClientPlayerEntity player, int screenX, int screenY, boolean container, int mouseX, int mouseY) {
        int[] bounds = getGroupBounds(handler, player, container);
        if (bounds == null) return;

        int btnX = screenX + bounds[2] - BTN_SIZE * 2 - BTN_GAP;
        int btnY = screenY + bounds[1] - BTN_SIZE - 2;

        String sortTooltip = container ? "§aSắp xếp rương" : "§aSắp xếp hành trang";
        String dropTooltip = container ? "§cVứt rương" : "§cVứt hành trang (trừ đồ đang mặc)";

        drawButton(context, client, btnX, btnY, "S", sortTooltip, isInside(mouseX, mouseY, btnX, btnY, BTN_SIZE, BTN_SIZE));
        drawButton(context, client, btnX + BTN_SIZE + BTN_GAP, btnY, "D", dropTooltip, isInside(mouseX, mouseY, btnX + BTN_SIZE + BTN_GAP, btnY, BTN_SIZE, BTN_SIZE));
    }

    private static void drawCenterArrow(DrawContext context, MinecraftClient client, ClientPlayerEntity player) {
        Vec3d playerPos = new Vec3d(player.getX(), player.getY(), player.getZ());
        int sw = context.getScaledWindowWidth();
        int sh = context.getScaledWindowHeight();

        SpawnerInfo target = null;
        double nearestDist = Double.MAX_VALUE;

        if (SpawnerFinderMod.showSpawner) {
            for (SpawnerInfo info : SpawnerFinderMod.foundSpawners) {
                double dist = player.squaredDistanceTo(info.centerPos());
                if (dist < nearestDist) { nearestDist = dist; target = info; }
            }
        }
        if (SpawnerFinderMod.showShulker) {
            for (SpawnerInfo info : SpawnerFinderMod.foundShulkers) {
                double dist = player.squaredDistanceTo(info.centerPos());
                if (dist < nearestDist) { nearestDist = dist; target = info; }
            }
        }
        if (SpawnerFinderMod.showChest) {
            for (SpawnerInfo info : SpawnerFinderMod.foundChests) {
                double dist = player.squaredDistanceTo(info.centerPos());
                if (dist < nearestDist) { nearestDist = dist; target = info; }
            }
        }
        if (SpawnerFinderMod.showEnderChest) {
            for (SpawnerInfo info : SpawnerFinderMod.foundEnderChests) {
                double dist = player.squaredDistanceTo(info.centerPos());
                if (dist < nearestDist) { nearestDist = dist; target = info; }
            }
        }
        if (SpawnerFinderMod.showPillager) {
            for (SpawnerInfo info : SpawnerFinderMod.foundPillagers) {
                double dist = player.squaredDistanceTo(info.centerPos());
                if (dist < nearestDist) { nearestDist = dist; target = info; }
            }
        }
        if (SpawnerFinderMod.showStructure) {
            for (SpawnerInfo info : SpawnerFinderMod.foundStructures) {
                double dist = player.squaredDistanceTo(info.centerPos());
                if (dist < nearestDist) { nearestDist = dist; target = info; }
            }
        }

        int cx = sw / 2;
        int cy = sh / 2 - 20;

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
            case "shulker_box", "shulker" -> { name = target.mobType().equals("shulker_box") ? "SHULKER BOX" : "SHULKER"; colorPrefix = "§b"; }
            case "single_chest" -> { name = "RƯƠNG ĐƠN"; colorPrefix = "§c"; }
            case "double_chest" -> { name = "RƯƠNG ĐÔI"; colorPrefix = "§c"; }
            case "single_trapped_chest" -> { name = "RƯƠNG BẪY ĐƠN"; colorPrefix = "§c"; }
            case "double_trapped_chest" -> { name = "RƯƠNG BẪY ĐÔI"; colorPrefix = "§c"; }
            case "ender_chest" -> { name = "RƯƠNG ENDER"; colorPrefix = "§5"; }
            case "pillager" -> { name = "PILLAGER"; colorPrefix = "§e"; }
            default -> { name = target.mobType().toUpperCase().replace('_', ' '); colorPrefix = "§d"; }
        }

        String line1 = String.format("§l%s  %s%s  §e%.0fm", arrow, colorPrefix, name, dist);
        String line2 = String.format("§8%s", target.pos().toShortString());

        context.drawCenteredTextWithShadow(client.textRenderer, Text.literal(line1), cx, cy, 0xFFFFFF);
        context.drawCenteredTextWithShadow(client.textRenderer, Text.literal(line2), cx, cy + 11, 0xAAAAAA);
    }

    private static boolean isInside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private static void drawButton(DrawContext context, MinecraftClient client, int x, int y, String label, String tooltip, boolean hovered) {
        int bg = hovered ? 0xC0FFFFFF : 0xC0000000;
        context.fill(x, y, x + BTN_SIZE, y + BTN_SIZE, bg);
        int color = hovered ? 0xFFFFFF : 0xAAAAAA;
        context.drawText(client.textRenderer, Text.literal(label), x + 5, y + 4, color, false);
        if (hovered) {
            context.drawTooltip(client.textRenderer, Text.literal(tooltip), x + BTN_SIZE + 2, y);
        }
    }
}
