package com.spawnerfinder;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class InventoryHelper {
    public enum Target {
        CONTAINER,
        PLAYER
    }

    private static int cooldown = 0;

    public static void tick(MinecraftClient client) {
        if (cooldown > 0) cooldown--;
    }

    public static boolean isOnCooldown() {
        return cooldown > 0;
    }

    private static void clickSlot(MinecraftClient client, ScreenHandler handler, int slotId, int button, SlotActionType action) {
        if (client.interactionManager != null) {
            client.interactionManager.clickSlot(handler.syncId, slotId, button, action, client.player);
        }
    }

    public static void sortInventory(MinecraftClient client, Target target) {
        if (cooldown > 0) return;
        cooldown = 10;
        ClientPlayerEntity player = client.player;
        if (player == null) return;
        ScreenHandler handler = player.currentScreenHandler;
        if (handler == null) return;

        List<Slot> targetSlots = getSlotsForTarget(handler, player, target);
        if (targetSlots.isEmpty()) return;

        List<ItemStack> items = new ArrayList<>();
        for (Slot slot : targetSlots) {
            ItemStack stack = slot.getStack();
            if (!stack.isEmpty()) {
                items.add(stack.copy());
            }
        }

        if (items.isEmpty()) return;

        items.sort(Comparator.comparing(s -> s.getItem().toString()));

        for (Slot slot : targetSlots) {
            if (!slot.getStack().isEmpty()) {
                clickSlot(client, handler, slot.id, 0, SlotActionType.QUICK_MOVE);
            }
        }

        clickSlot(client, handler, -999, 0, SlotActionType.PICKUP);

        for (ItemStack desired : items) {
            for (Slot slot : handler.slots) {
                boolean isOtherSlot = (target == Target.CONTAINER)
                    ? (slot.inventory == player.getInventory() && slot.getIndex() < 36)
                    : (slot.inventory != player.getInventory());
                if (isOtherSlot && !slot.getStack().isEmpty() && slot.getStack().getItem() == desired.getItem()) {
                    clickSlot(client, handler, slot.id, 0, SlotActionType.QUICK_MOVE);
                    break;
                }
            }
        }

        clickSlot(client, handler, -999, 0, SlotActionType.PICKUP);

        player.sendMessage(Text.literal("§6[SpawnerFinder] §aĐã sắp xếp!"), true);
    }

    public static void throwAll(MinecraftClient client, Target target) {
        if (cooldown > 0) return;
        cooldown = 10;
        ClientPlayerEntity player = client.player;
        if (player == null) return;
        ScreenHandler handler = player.currentScreenHandler;
        if (handler == null) return;

        List<Slot> targetSlots = getSlotsForTarget(handler, player, target);
        int count = 0;
        for (Slot slot : targetSlots) {
            ItemStack stack = slot.getStack();
            if (!stack.isEmpty()) {
                clickSlot(client, handler, slot.id, 1, SlotActionType.THROW);
                count += stack.getCount();
            }
        }

        String targetName = (target == Target.CONTAINER) ? "rương" : "hành trang";
        player.sendMessage(Text.literal("§6[SpawnerFinder] §aĐã vứt " + count + " item khỏi " + targetName + "!"), true);
    }

    private static List<Slot> getSlotsForTarget(ScreenHandler handler, ClientPlayerEntity player, Target target) {
        List<Slot> result = new ArrayList<>();
        for (Slot slot : handler.slots) {
            if (target == Target.CONTAINER) {
                if (slot.inventory != player.getInventory()) {
                    result.add(slot);
                }
            } else {
                if (slot.inventory == player.getInventory() && slot.getIndex() < 36) {
                    result.add(slot);
                }
            }
        }
        return result;
    }
}
