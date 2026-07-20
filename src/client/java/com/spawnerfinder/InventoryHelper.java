package com.spawnerfinder;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class InventoryHelper {
    public enum Target { CONTAINER, PLAYER }

    private static int cooldown = 0;

    public static void tick(MinecraftClient client) {
        if (cooldown > 0) cooldown--;
    }

    public static boolean isOnCooldown() { return cooldown > 0; }

    public static boolean sortInventory(MinecraftClient client, Target target) {
        if (cooldown > 0) return false;
        cooldown = 10;
        ClientPlayerEntity player = client.player;
        if (player == null) return false;
        ScreenHandler handler = player.currentScreenHandler;
        if (handler == null) return false;

        List<Slot> targetSlots = getSlotsForTarget(handler, player, target);
        if (targetSlots.isEmpty()) return false;

        List<ItemStack> items = new ArrayList<>();
        for (Slot slot : targetSlots) {
            ItemStack stack = slot.getStack();
            if (!stack.isEmpty()) items.add(stack.copy());
        }
        if (items.isEmpty()) return false;

        items.sort(Comparator.comparing(s -> s.getItem().toString()));

        for (Slot slot : targetSlots) {
            if (!slot.getStack().isEmpty())
                handler.onSlotClick(slot.id, 0, SlotActionType.QUICK_MOVE, player);
        }
        handler.onSlotClick(-999, 0, SlotActionType.PICKUP, player);

        for (ItemStack desired : items) {
            for (Slot slot : handler.slots) {
                boolean isOtherSlot = (target == Target.CONTAINER)
                    ? (slot.inventory == player.getInventory() && slot.getIndex() < 36)
                    : (slot.inventory != player.getInventory());
                if (isOtherSlot && !slot.getStack().isEmpty() && slot.getStack().getItem() == desired.getItem()) {
                    handler.onSlotClick(slot.id, 0, SlotActionType.QUICK_MOVE, player);
                    break;
                }
            }
        }
        handler.onSlotClick(-999, 0, SlotActionType.PICKUP, player);
        return true;
    }

    public static boolean throwAll(MinecraftClient client, Target target) {
        if (cooldown > 0) return false;
        cooldown = 10;
        ClientPlayerEntity player = client.player;
        if (player == null) return false;
        ScreenHandler handler = player.currentScreenHandler;
        if (handler == null) return false;

        List<Slot> targetSlots = getSlotsForTarget(handler, player, target);
        for (Slot slot : targetSlots) {
            if (!slot.getStack().isEmpty())
                handler.onSlotClick(slot.id, 1, SlotActionType.THROW, player);
        }
        return true;
    }

    public static boolean transferAll(MinecraftClient client, Target target) {
        if (cooldown > 0) return false;
        cooldown = 10;
        ClientPlayerEntity player = client.player;
        if (player == null) return false;
        ScreenHandler handler = player.currentScreenHandler;
        if (handler == null) return false;

        List<Slot> targetSlots = getSlotsForTarget(handler, player, target);
        for (Slot slot : targetSlots) {
            if (!slot.getStack().isEmpty())
                handler.onSlotClick(slot.id, 0, SlotActionType.QUICK_MOVE, player);
        }
        return true;
    }

    private static List<Slot> getSlotsForTarget(ScreenHandler handler, ClientPlayerEntity player, Target target) {
        List<Slot> result = new ArrayList<>();
        for (Slot slot : handler.slots) {
            if (target == Target.CONTAINER) {
                if (slot.inventory != player.getInventory())
                    result.add(slot);
            } else {
                if (slot.inventory == player.getInventory() && slot.getIndex() < 36)
                    result.add(slot);
            }
        }
        return result;
    }
}
