package com.spawnerfinder;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;

import java.io.FileReader;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class InventoryUI {
    private static final int BTN_SIZE = 12;
    private static final int BTN_GAP = 2;

    static int OFFSET_C_S_X = 0, OFFSET_C_S_Y = 0;
    static int OFFSET_C_T_X = 0, OFFSET_C_T_Y = 0;
    static int OFFSET_C_M_X = 0, OFFSET_C_M_Y = 0;
    static int OFFSET_P_S_X = 0, OFFSET_P_S_Y = 0;
    static int OFFSET_P_T_X = 0, OFFSET_P_T_Y = 0;
    static int OFFSET_P_M_X = 0, OFFSET_P_M_Y = 0;

    public static void loadConfig() {
        Path path = Paths.get("config", "spawnerfinder.json");
        java.io.File file = path.toFile();
        if (!file.exists()) { saveConfig(); return; }
        try (FileReader reader = new FileReader(file)) {
            Config cfg = new Gson().fromJson(reader, Config.class);
            if (cfg == null) return;
            OFFSET_C_S_X = cfg.cs_x; OFFSET_C_S_Y = cfg.cs_y;
            OFFSET_C_T_X = cfg.ct_x; OFFSET_C_T_Y = cfg.ct_y;
            OFFSET_C_M_X = cfg.cm_x; OFFSET_C_M_Y = cfg.cm_y;
            OFFSET_P_S_X = cfg.ps_x; OFFSET_P_S_Y = cfg.ps_y;
            OFFSET_P_T_X = cfg.pt_x; OFFSET_P_T_Y = cfg.pt_y;
            OFFSET_P_M_X = cfg.pm_x; OFFSET_P_M_Y = cfg.pm_y;
        } catch (Exception e) { e.printStackTrace(); }
    }

    public static void saveConfig() {
        Path path = Paths.get("config", "spawnerfinder.json");
        path.getParent().toFile().mkdirs();
        Config cfg = new Config();
        cfg.cs_x = OFFSET_C_S_X; cfg.cs_y = OFFSET_C_S_Y;
        cfg.ct_x = OFFSET_C_T_X; cfg.ct_y = OFFSET_C_T_Y;
        cfg.cm_x = OFFSET_C_M_X; cfg.cm_y = OFFSET_C_M_Y;
        cfg.ps_x = OFFSET_P_S_X; cfg.ps_y = OFFSET_P_S_Y;
        cfg.pt_x = OFFSET_P_T_X; cfg.pt_y = OFFSET_P_T_Y;
        cfg.pm_x = OFFSET_P_M_X; cfg.pm_y = OFFSET_P_M_Y;
        try (java.io.FileWriter writer = new java.io.FileWriter(path.toFile())) {
            new GsonBuilder().setPrettyPrinting().create().toJson(cfg, writer);
        } catch (Exception e) { e.printStackTrace(); }
    }

    static { loadConfig(); }

    public static void onRender(HandledScreen<?> screen, int x, int y, int backgroundWidth, int backgroundHeight, DrawContext context, int mouseX, int mouseY, float delta) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null) return;
        ScreenHandler handler = screen.getScreenHandler();
        boolean hasContainer = hasContainerSlots(handler, player);
        if (hasContainer)
            drawButtons(context, client, handler, player, x, y, true, mouseX, mouseY);
        drawButtons(context, client, handler, player, x, y, false, mouseX, mouseY);
        String tip = null;
        if (hasContainer) { var r = getTooltip(x, y, handler, player, true, mouseX, mouseY); if (r != null) tip = r.name; }
        if (tip == null) { var r = getTooltip(x, y, handler, player, false, mouseX, mouseY); if (r != null) tip = r.name; }
        if (tip != null) {
            Text text = Text.literal(tip);
            int tw = client.textRenderer.getWidth(text);
            int tx = Math.min(mouseX + 8, context.getScaledWindowWidth() - tw - 4);
            int ty = mouseY - 12;
            context.fill(tx - 2, ty - 2, tx + tw + 2, ty + 10 + 2, 0xC0000000);
            context.drawText(client.textRenderer, text, tx, ty, 0xFFFFFF, true);
        }
    }

    public static void onMouseClicked(int x, int y, int backgroundWidth, int backgroundHeight, double mouseX, double mouseY, int button) {
        if (button != 0) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (!(client.currentScreen instanceof HandledScreen<?> screen)) return;
        if (client.player == null) return;
        ScreenHandler handler = screen.getScreenHandler();
        boolean hasContainer = hasContainerSlots(handler, client.player);

        if (hasContainer) {
            String hit = hitTest(x, y, handler, client.player, true, mouseX, mouseY);
            if (hit != null) {
                executeAction(client, handler, hit, true);
                return;
            }
        }
        String hit = hitTest(x, y, handler, client.player, false, mouseX, mouseY);
        if (hit != null) {
            executeAction(client, handler, hit, false);
        }
    }

    private static void executeAction(MinecraftClient client, ScreenHandler handler, String action, boolean targetContainer) {
        InventoryHelper.Target t = targetContainer ? InventoryHelper.Target.CONTAINER : InventoryHelper.Target.PLAYER;
        switch (action) {
            case "S" -> InventoryHelper.sortInventory(client, t);
            case "T" -> InventoryHelper.throwAll(client, t);
            case "M" -> InventoryHelper.transferAll(client, t);
        }
    }

    private record TooltipInfo(String name, int color) {}
    private static TooltipInfo getTooltip(int screenX, int screenY, ScreenHandler handler, ClientPlayerEntity player, boolean container, double mx, double my) {
        String hit = hitTest(screenX, screenY, handler, player, container, mx, my);
        if (hit == null) return null;
        return switch (hit) {
            case "S" -> new TooltipInfo("Sắp xếp", 0xFFAA00);
            case "T" -> new TooltipInfo("Ném bỏ", 0xFF5555);
            case "M" -> new TooltipInfo("Chuyển", 0x55FFFF);
            default -> null;
        };
    }

    private static String hitTest(int screenX, int screenY, ScreenHandler handler, ClientPlayerEntity player, boolean container, double mx, double my) {
        int[] bounds = getGroupBounds(handler, player, container);
        if (bounds == null) return null;
        String[] labels = {"S", "T", "M"};
        int[][] offsets = container
            ? new int[][]{{OFFSET_C_S_X, OFFSET_C_S_Y}, {OFFSET_C_T_X, OFFSET_C_T_Y}, {OFFSET_C_M_X, OFFSET_C_M_Y}}
            : new int[][]{{OFFSET_P_S_X, OFFSET_P_S_Y}, {OFFSET_P_T_X, OFFSET_P_T_Y}, {OFFSET_P_M_X, OFFSET_P_M_Y}};
        int baseX = screenX + bounds[2] - BTN_SIZE * 3 - BTN_GAP * 2;
        int baseY = screenY + bounds[1] - BTN_SIZE - 2;
        for (int i = 0; i < 3; i++) {
            int bx = baseX + (BTN_SIZE + BTN_GAP) * i + offsets[i][0];
            int by = baseY + offsets[i][1];
            if (isInside(mx, my, bx, by, BTN_SIZE, BTN_SIZE)) return labels[i];
        }
        return null;
    }

    private static boolean isInside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
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
        int baseX = screenX + bounds[2] - BTN_SIZE * 3 - BTN_GAP * 2;
        int baseY = screenY + bounds[1] - BTN_SIZE - 2;
        int osX, osY;
        osX = container ? OFFSET_C_S_X : OFFSET_P_S_X;
        osY = container ? OFFSET_C_S_Y : OFFSET_P_S_Y;
        drawButton(context, client, baseX + osX, baseY + osY, "S", isInside(mouseX, mouseY, baseX + osX, baseY + osY, BTN_SIZE, BTN_SIZE));
        osX = container ? OFFSET_C_T_X : OFFSET_P_T_X;
        osY = container ? OFFSET_C_T_Y : OFFSET_P_T_Y;
        drawButton(context, client, baseX + BTN_SIZE + BTN_GAP + osX, baseY + osY, "T", isInside(mouseX, mouseY, baseX + BTN_SIZE + BTN_GAP + osX, baseY + osY, BTN_SIZE, BTN_SIZE));
        osX = container ? OFFSET_C_M_X : OFFSET_P_M_X;
        osY = container ? OFFSET_C_M_Y : OFFSET_P_M_Y;
        drawButton(context, client, baseX + (BTN_SIZE + BTN_GAP) * 2 + osX, baseY + osY, "M", isInside(mouseX, mouseY, baseX + (BTN_SIZE + BTN_GAP) * 2 + osX, baseY + osY, BTN_SIZE, BTN_SIZE));
    }

    private static void drawButton(DrawContext context, MinecraftClient client, int x, int y, String label, boolean hovered) {
        int bg = hovered ? 0xC0FFFFFF : 0xC0000000;
        context.fill(x, y, x + BTN_SIZE, y + BTN_SIZE, bg);
        int color = hovered ? 0x000000 : 0xAAAAAA;
        context.drawText(client.textRenderer, Text.literal(label), x + 5, y + 4, color, false);
    }

    static class Config {
        int cs_x = 0, cs_y = 0;
        int ct_x = 0, ct_y = 0;
        int cm_x = 0, cm_y = 0;
        int ps_x = 0, ps_y = 0;
        int pt_x = 0, pt_y = 0;
        int pm_x = 0, pm_y = 0;
    }
}
