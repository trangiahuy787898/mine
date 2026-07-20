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

    public static void onRender(HandledScreen<?> screen, int x, int y, int backgroundWidth, int backgroundHeight, DrawContext context, int mouseX, int mouseY, float delta) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null) return;

        ScreenHandler handler = screen.getScreenHandler();
        boolean hasContainer = hasContainerSlots(handler, player);

        if (hasContainer)
            drawButtons(context, client, handler, player, x, y, true, mouseX, mouseY);
        drawButtons(context, client, handler, player, x, y, false, mouseX, mouseY);
    }

    public static boolean onMouseClicked(int x, int y, int backgroundWidth, int backgroundHeight, double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        MinecraftClient client = MinecraftClient.getInstance();
        if (!(client.currentScreen instanceof HandledScreen<?> screen)) return false;
        if (client.player == null) return false;

        ScreenHandler handler = screen.getScreenHandler();
        boolean hasContainer = hasContainerSlots(handler, client.player);

        if (hasContainer) {
            int[] bounds = getGroupBounds(handler, client.player, true);
            if (bounds != null) {
                int btnX = x + bounds[2] - BTN_SIZE * 3 - BTN_GAP * 2;
                int btnY = y + bounds[1] - BTN_SIZE - 2;
                if (isInside(mouseX, mouseY, btnX, btnY, BTN_SIZE, BTN_SIZE))
                    return InventoryHelper.sortInventory(client, InventoryHelper.Target.CONTAINER);
                if (isInside(mouseX, mouseY, btnX + BTN_SIZE + BTN_GAP, btnY, BTN_SIZE, BTN_SIZE))
                    return InventoryHelper.throwAll(client, InventoryHelper.Target.CONTAINER);
                if (isInside(mouseX, mouseY, btnX + (BTN_SIZE + BTN_GAP) * 2, btnY, BTN_SIZE, BTN_SIZE))
                    return InventoryHelper.transferAll(client, InventoryHelper.Target.CONTAINER);
            }
        }

        int[] bounds = getGroupBounds(handler, client.player, false);
        if (bounds != null) {
            int btnX = x + bounds[2] - BTN_SIZE * 3 - BTN_GAP * 2;
            int btnY = y + bounds[1] - BTN_SIZE - 2;
            if (isInside(mouseX, mouseY, btnX, btnY, BTN_SIZE, BTN_SIZE))
                return InventoryHelper.sortInventory(client, InventoryHelper.Target.PLAYER);
            if (isInside(mouseX, mouseY, btnX + BTN_SIZE + BTN_GAP, btnY, BTN_SIZE, BTN_SIZE))
                return InventoryHelper.throwAll(client, InventoryHelper.Target.PLAYER);
            if (isInside(mouseX, mouseY, btnX + (BTN_SIZE + BTN_GAP) * 2, btnY, BTN_SIZE, BTN_SIZE))
                return InventoryHelper.transferAll(client, InventoryHelper.Target.PLAYER);
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

    private static void drawButtons(DrawContext context, MinecraftClient client, ScreenHandler handler, ClientPlayerEntity player, int screenX, int screenY, boolean container, int mouseX, int mouseY) {
        int[] bounds = getGroupBounds(handler, player, container);
        if (bounds == null) return;

        int btnX = screenX + bounds[2] - BTN_SIZE * 3 - BTN_GAP * 2;
        int btnY = screenY + bounds[1] - BTN_SIZE - 2;

        drawButton(context, client, btnX, btnY, "S", isInside(mouseX, mouseY, btnX, btnY, BTN_SIZE, BTN_SIZE));
        drawButton(context, client, btnX + BTN_SIZE + BTN_GAP, btnY, "T", isInside(mouseX, mouseY, btnX + BTN_SIZE + BTN_GAP, btnY, BTN_SIZE, BTN_SIZE));
        drawButton(context, client, btnX + (BTN_SIZE + BTN_GAP) * 2, btnY, "M", isInside(mouseX, mouseY, btnX + (BTN_SIZE + BTN_GAP) * 2, btnY, BTN_SIZE, BTN_SIZE));
    }

    private static boolean isInside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private static void drawButton(DrawContext context, MinecraftClient client, int x, int y, String label, boolean hovered) {
        int bg = hovered ? 0xC0FFFFFF : 0xC0000000;
        context.fill(x, y, x + BTN_SIZE, y + BTN_SIZE, bg);
        int color = hovered ? 0xFFFFFF : 0xAAAAAA;
        context.drawText(client.textRenderer, Text.literal(label), x + 5, y + 4, color, false);
    }
}
