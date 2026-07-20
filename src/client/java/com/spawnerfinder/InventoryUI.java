package com.spawnerfinder;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

import java.io.FileReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class InventoryUI {

    public static int OFFSET_S_X = 0;
    public static int OFFSET_S_Y = 0;
    public static int OFFSET_C_X = 0;
    public static int OFFSET_C_Y = 0;
    public static int OFFSET_T_X = 0;
    public static int OFFSET_T_Y = 0;

    public static final Map<ButtonWidget, String> BUTTON_TYPES = new HashMap<>();
    static ButtonWidget dragging = null;
    static boolean dragMoved = false;
    static double dragStartX, dragStartY;

    public static void loadConfig() {
        Path path = Paths.get("config", "spawnerfinder.json");
        java.io.File file = path.toFile();
        if (!file.exists()) {
            saveConfig();
            return;
        }
        try (FileReader reader = new FileReader(file)) {
            InventoryUIConfig cfg = new Gson().fromJson(reader, InventoryUIConfig.class);
            if (cfg != null) {
                OFFSET_S_X = cfg.s_x;
                OFFSET_S_Y = cfg.s_y;
                OFFSET_C_X = cfg.c_x;
                OFFSET_C_Y = cfg.c_y;
                OFFSET_T_X = cfg.t_x;
                OFFSET_T_Y = cfg.t_y;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void saveConfig() {
        Path path = Paths.get("config", "spawnerfinder.json");
        path.getParent().toFile().mkdirs();
        InventoryUIConfig cfg = new InventoryUIConfig();
        cfg.s_x = OFFSET_S_X; cfg.s_y = OFFSET_S_Y;
        cfg.c_x = OFFSET_C_X; cfg.c_y = OFFSET_C_Y;
        cfg.t_x = OFFSET_T_X; cfg.t_y = OFFSET_T_Y;
        try (java.io.FileWriter writer = new java.io.FileWriter(path.toFile())) {
            new GsonBuilder().setPrettyPrinting().create().toJson(cfg, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static {
        loadConfig();
    }

    public static List<ButtonWidget> createButtons(HandledScreen<?> screen, int scaledWidth, int scaledHeight) {
        List<ButtonWidget> buttons = new ArrayList<>();
        if (!(screen instanceof GenericContainerScreen || screen instanceof InventoryScreen || screen instanceof ShulkerBoxScreen)) return buttons;
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null) return buttons;
        ScreenHandler handler = screen.getScreenHandler();

        boolean hasContainer = hasContainerSlots(handler, player);

        if (hasContainer) {
            int[] cBounds = getGroupBounds(handler, player, true);
            if (cBounds != null) {
                addHorizontalBtnGroup(buttons, cBounds[2], cBounds[3] + 4, 14, 4,
                    b -> InventoryHelper.sortInventory(client, InventoryHelper.Target.CONTAINER),
                    b -> InventoryHelper.transferAll(client, InventoryHelper.Target.CONTAINER),
                    b -> InventoryHelper.throwAll(client, InventoryHelper.Target.CONTAINER));
            }

            int[] pBounds = getGroupBounds(handler, player, false);
            if (pBounds != null) {
                addHorizontalBtnGroup(buttons, pBounds[2], pBounds[3] + 4, 14, 4,
                    b -> InventoryHelper.sortInventory(client, InventoryHelper.Target.PLAYER),
                    b -> InventoryHelper.transferAll(client, InventoryHelper.Target.PLAYER),
                    b -> InventoryHelper.throwAll(client, InventoryHelper.Target.PLAYER));
            }
        } else {
            int[] bounds = getGroupBounds(handler, player, false);
            if (bounds != null) {
                addHorizontalBtnGroup(buttons, bounds[2], bounds[3] + 4, 14, 4,
                    b -> InventoryHelper.sortInventory(client, InventoryHelper.Target.PLAYER),
                    b -> InventoryHelper.transferAll(client, InventoryHelper.Target.PLAYER),
                    b -> InventoryHelper.throwAll(client, InventoryHelper.Target.PLAYER));
            }
        }

        return buttons;
    }

    private static void addHorizontalBtnGroup(List<ButtonWidget> buttons, int rightX, int y, int size, int gap,
                                               Consumer<ButtonWidget> sort, Consumer<ButtonWidget> transfer, Consumer<ButtonWidget> drop) {
        int totalW = size * 3 + gap * 2;
        int x = rightX - totalW - 4;
        addBtn(buttons, x + OFFSET_S_X, y + OFFSET_S_Y, size, size, "S", sort);
        addBtn(buttons, x + size + gap + OFFSET_C_X, y + OFFSET_C_Y, size, size, "C", transfer);
        addBtn(buttons, x + (size + gap) * 2 + OFFSET_T_X, y + OFFSET_T_Y, size, size, "T", drop);
    }

    private static void addBtn(List<ButtonWidget> buttons, int x, int y, int w, int h, String text, Consumer<ButtonWidget> onClick) {
        ButtonWidget btn = ButtonWidget.builder(Text.literal(text), onClick::accept)
            .dimensions(x, y, w, h).build();
        BUTTON_TYPES.put(btn, text);
        buttons.add(btn);
    }

    public static void clearButtons() {
        BUTTON_TYPES.clear();
        dragging = null;
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

    static class InventoryUIConfig {
        int s_x = 0, s_y = 0;
        int c_x = 0, c_y = 0;
        int t_x = 0, t_y = 0;
    }
}
