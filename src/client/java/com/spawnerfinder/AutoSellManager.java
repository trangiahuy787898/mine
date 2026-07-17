package com.spawnerfinder;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.*;

public class AutoSellManager {
    private static boolean active = false;
    private static State state = State.IDLE;
    private static int delay = 0;
    private static int failCount = 0;

    private static List<List<BlockPos>> allStacks = new ArrayList<>();
    private static int stackIndex = 0;
    private static List<BlockPos> currentStack = null;
    private static int chestIndex = 0;

    private static final double REACH_SQ = 4.5 * 4.5;

    public enum State {
        IDLE,
        SCAN_CHESTS,
        WALK_TO_CHEST,
        OPEN_CHEST,
        WAIT_CHEST_SCREEN,
        TAKE_CHEST_ITEMS,
        CLOSE_CHEST,
        SEND_SELL,
        WAIT_SELL_SCREEN,
        FILL_SHOP,
        CLOSE_SELL,
        CHECK_CONFIRM,
        NEXT_STACK,
        DONE
    }

    public static void toggle() {
        if (active) { stop(); } else { start(); }
    }

    public static boolean isActive() { return active; }
    public static State getCurrentState() { return state; }

    private static void start() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        active = true;
        state = State.SCAN_CHESTS;
        delay = 0;
        stackIndex = 0;
        chestIndex = 0;
        allStacks.clear();
        currentStack = null;
        failCount = 0;
        releaseKeys(client);
        msg("§aBắt đầu AutoSell...");
    }

    public static void stop() {
        active = false;
        state = State.IDLE;
        allStacks.clear();
        currentStack = null;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            releaseKeys(client);
            msg("§cĐã dừng AutoSell.");
        }
    }

    private static void msg(String text) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(Text.literal("§6[AutoSell] " + text), true);
        }
    }

    private static void releaseKeys(MinecraftClient client) {
        client.options.forwardKey.setPressed(false);
        client.options.jumpKey.setPressed(false);
        client.options.sneakKey.setPressed(false);
    }

    public static void tick(MinecraftClient client) {
        if (!active) return;
        if (client.player == null || client.world == null) {
            stop();
            return;
        }

        if (delay > 0) { delay--; return; }

        try {
            switch (state) {
                case SCAN_CHESTS -> scanChestStacks(client);
                case WALK_TO_CHEST -> walkToChest(client);
                case OPEN_CHEST -> openChest(client);
                case WAIT_CHEST_SCREEN -> waitChestScreen(client);
                case TAKE_CHEST_ITEMS -> takeChestItems(client);
                case CLOSE_CHEST -> closeChest(client);
                case SEND_SELL -> sendSell(client);
                case WAIT_SELL_SCREEN -> waitSellScreen(client);
                case FILL_SHOP -> fillShop(client);
                case CLOSE_SELL -> closeSell(client);
                case CHECK_CONFIRM -> checkConfirm(client);
                case NEXT_STACK -> nextStack(client);
                case DONE -> finish(client);
            }
        } catch (Exception e) {
            msg("§cLỗi: " + e.getMessage());
            stop();
        }
    }

    // ────────────── SCAN ──────────────

    private static void scanChestStacks(MinecraftClient client) {
        ClientWorld world = client.world;
        ClientPlayerEntity player = client.player;
        BlockPos playerPos = player.getBlockPos();
        allStacks.clear();

        // Group chests by (x,z)
        Map<String, List<BlockPos>> grouped = new HashMap<>();
        for (BlockEntity be : world.getBlockEntities()) {
            if (be instanceof ChestBlockEntity) {
                BlockPos p = be.getPos();
                double d = player.squaredDistanceTo(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5);
                if (d <= 400) { // 20 block radius
                    grouped.computeIfAbsent(p.getX() + "," + p.getZ(), k -> new ArrayList<>()).add(p);
                }
            }
        }

        for (List<BlockPos> group : grouped.values()) {
            if (group.size() < 4) continue;
            group.sort(Comparator.comparingInt(BlockPos::getY));
            // Find consecutive vertical stack
            List<BlockPos> stack = new ArrayList<>();
            for (BlockPos p : group) {
                if (stack.isEmpty() || p.getY() == stack.getLast().getY() + 1) {
                    stack.add(p);
                    if (stack.size() == 4) break;
                } else {
                    stack.clear();
                    stack.add(p);
                }
            }
            if (stack.size() == 4) {
                allStacks.add(stack);
            }
        }

        allStacks.sort(Comparator.comparingDouble(s -> s.getFirst().getSquaredDistance(playerPos)));

        if (allStacks.isEmpty()) {
            msg("§cKhông tìm thấy dãy rương nào trong bán kính!");
            state = State.DONE;
            return;
        }

        msg("§aTìm thấy " + allStacks.size() + " dãy rương.");
        stackIndex = 0;
        chestIndex = 0;
        currentStack = allStacks.getFirst();
        state = State.WALK_TO_CHEST;
    }

    // ────────────── WALK ──────────────

    private static void walkToChest(MinecraftClient client) {
        if (currentStack == null || chestIndex >= currentStack.size()) {
            state = State.NEXT_STACK;
            return;
        }

        BlockPos target = currentStack.get(chestIndex);
        ClientPlayerEntity player = client.player;
        Vec3d ppos = new Vec3d(player.getX(), player.getY(), player.getZ());
        Vec3d tpos = Vec3d.ofBottomCenter(target);

        if (ppos.squaredDistanceTo(tpos) <= REACH_SQ) {
            releaseKeys(client);
            delay = 5;
            state = State.OPEN_CHEST;
            return;
        }

        // Face target
        double dx = tpos.x - ppos.x;
        double dz = tpos.z - ppos.z;
        double dy = (tpos.y + 0.5) - (ppos.y + 1.6);
        player.setYaw((float) Math.toDegrees(Math.atan2(dz, dx)) - 90);
        player.setPitch((float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz))));

        // Move
        client.options.forwardKey.setPressed(true);
        client.options.jumpKey.setPressed(dy > 0.8 && player.isOnGround());
    }

    // ────────────── OPEN CHEST ──────────────

    private static void openChest(MinecraftClient client) {
        if (currentStack == null || chestIndex >= currentStack.size()) {
            state = State.NEXT_STACK;
            return;
        }

        BlockPos target = currentStack.get(chestIndex);
        ClientPlayerEntity player = client.player;
        Vec3d ppos = new Vec3d(player.getX(), player.getY(), player.getZ());
        Vec3d tc = Vec3d.ofCenter(target);

        double dx = tc.x - ppos.x;
        double dz = tc.z - ppos.z;
        double dy = tc.y - (ppos.y + 1.6);
        player.setYaw((float) Math.toDegrees(Math.atan2(dz, dx)) - 90);
        player.setPitch((float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz))));

        client.interactionManager.interactBlock(player, Hand.MAIN_HAND,
            new BlockHitResult(tc, Direction.UP, target, false));

        delay = 10;
        state = State.WAIT_CHEST_SCREEN;
    }

    private static void waitChestScreen(MinecraftClient client) {
        if (client.currentScreen instanceof HandledScreen) {
            delay = 5;
            state = State.TAKE_CHEST_ITEMS;
        } else {
            failCount++;
            if (failCount > 5) {
                msg("§eKhông mở được rương, bỏ qua...");
                failCount = 0;
                state = State.CLOSE_CHEST;
            } else {
                state = State.OPEN_CHEST;
            }
        }
    }

    // ────────────── TAKE ITEMS FROM CHEST (THROW) ──────────────

    private static void takeChestItems(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null || !(client.currentScreen instanceof HandledScreen)) return;

        InventoryHelper.throwAll(client, InventoryHelper.Target.CONTAINER);

        delay = 10;
        state = State.CLOSE_CHEST;
    }

    // ────────────── CLOSE CHEST ──────────────

    private static void closeChest(MinecraftClient client) {
        if (client.currentScreen != null) {
            client.player.closeHandledScreen();
        }
        delay = 5;
        failCount = 0;

        chestIndex++;
        if (currentStack != null && chestIndex < currentStack.size()) {
            state = State.WALK_TO_CHEST;
        } else {
            msg("§aĐã lấy hết items từ dãy rương, mở shop...");
            chestIndex = 0;
            delay = 10;
            state = State.SEND_SELL;
        }
    }

    // ────────────── SELL ──────────────

    private static void sendSell(MinecraftClient client) {
        if (client.player != null && client.player.networkHandler != null) {
            client.player.networkHandler.sendChatCommand("sellgui");
        }
        delay = 15;
        state = State.WAIT_SELL_SCREEN;
    }

    private static void waitSellScreen(MinecraftClient client) {
        if (client.currentScreen instanceof HandledScreen) {
            delay = 5;
            state = State.FILL_SHOP;
        } else {
            delay = 5;
        }
    }

    private static void fillShop(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null || !(client.currentScreen instanceof HandledScreen screen)) {
            state = State.SEND_SELL;
            return;
        }

        ScreenHandler handler = screen.getScreenHandler();
        boolean hasItems = false;

        for (Slot slot : handler.slots) {
            if (slot.inventory == player.getInventory() && slot.getIndex() < 36 && !slot.getStack().isEmpty()) {
                client.interactionManager.clickSlot(handler.syncId, slot.id, 0, SlotActionType.QUICK_MOVE, player);
                hasItems = true;
                delay = 2;
                break;
            }
        }

        if (!hasItems) {
            delay = 5;
            state = State.CLOSE_SELL;
        }
    }

    private static void closeSell(MinecraftClient client) {
        if (client.currentScreen != null) {
            client.player.closeHandledScreen();
        }
        delay = 10;
        state = State.CHECK_CONFIRM;
    }

    private static void checkConfirm(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null) return;

        // Check if a confirmation dialog appeared after closing shop
        if (client.currentScreen instanceof HandledScreen confirmScreen) {
            ScreenHandler h = confirmScreen.getScreenHandler();
            for (Slot slot : h.slots) {
                ItemStack stack = slot.getStack();
                if (!stack.isEmpty() && slot.inventory != player.getInventory()) {
                    String name = stack.getName().getString().toLowerCase();
                    if (name.contains("đồng ý") || name.contains("confirm") || name.contains("bán")) {
                        client.interactionManager.clickSlot(h.syncId, slot.id, 0, SlotActionType.PICKUP, player);
                        delay = 10;
                        checkAndContinue(client);
                        return;
                    }
                }
            }
        }

        // No confirmation dialog, proceed
        checkAndContinue(client);
    }

    private static void checkAndContinue(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null) return;

        // Small delay for the sale to process
        delay = 10;

        // Check if inventory still has items
        boolean hasItems = false;
        for (int i = 0; i < 36; i++) {
            if (!player.getInventory().getStack(i).isEmpty()) {
                hasItems = true;
                break;
            }
        }

        if (hasItems) {
            msg("§aCòn items trong người, bán tiếp...");
            delay = 10;
            state = State.SEND_SELL;
        } else {
            state = State.NEXT_STACK;
        }
    }

    // ────────────── NEXT / DONE ──────────────

    private static void nextStack(MinecraftClient client) {
        releaseKeys(client);
        chestIndex = 0;
        stackIndex++;
        if (stackIndex < allStacks.size()) {
            currentStack = allStacks.get(stackIndex);
            msg("§aChuyển sang dãy rương " + (stackIndex + 1) + "/" + allStacks.size());
            delay = 10;
            state = State.WALK_TO_CHEST;
        } else {
            state = State.DONE;
        }
    }

    private static void finish(MinecraftClient client) {
        msg("§a§lHoàn tất! Đã bán hết items.");
        active = false;
        state = State.IDLE;
        releaseKeys(client);
        allStacks.clear();
        currentStack = null;
    }

    // ────────────── HUD TEXT ──────────────

    public static String getStatusText() {
        if (!active) return null;
        String stateName = switch (state) {
            case SCAN_CHESTS -> "§7Đang quét rương...";
            case WALK_TO_CHEST -> "§eĐang di chuyển đến rương...";
            case OPEN_CHEST -> "§eĐang mở rương...";
            case WAIT_CHEST_SCREEN -> "§eĐợi mở rương...";
            case TAKE_CHEST_ITEMS -> "§aLấy items từ rương...";
            case CLOSE_CHEST -> "§eĐóng rương...";
            case SEND_SELL -> "§eMở shop...";
            case WAIT_SELL_SCREEN -> "§eĐợi shop...";
            case FILL_SHOP -> "§aĐang fill shop...";
            case CLOSE_SELL -> "§eĐóng shop...";
            case CHECK_CONFIRM -> "§6Xác nhận bán...";
            case NEXT_STACK -> "§eChuyển dãy rương...";
            case DONE -> "§a§lHOÀN TẤT!";
            default -> "";
        };
        String stackInfo = allStacks.isEmpty() ? "" :
            " §7[" + (stackIndex + 1) + "/" + allStacks.size() + "] §8Rương " + (chestIndex + 1) + "/4";
        return "§6[AutoSell] " + stateName + stackInfo;
    }
}
