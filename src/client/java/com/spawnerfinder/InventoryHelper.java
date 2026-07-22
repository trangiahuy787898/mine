package com.spawnerfinder;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.*;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import java.util.ArrayList;
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
        int syncId = handler.syncId;

        List<Slot> targetSlots = getSlotsForTarget(handler, player, target);
        if (targetSlots.isEmpty()) return false;

        List<ItemStack> stacks = new ArrayList<>();
        for (Slot slot : targetSlots) {
            ItemStack stack = slot.getStack();
            if (!stack.isEmpty()) stacks.add(stack.copy());
        }
        if (stacks.isEmpty()) return false;

        String label = target == Target.CONTAINER ? "§6[Sắp xếp rương]" : "§6[Sắp xếp túi]";
        player.sendMessage(Text.literal(label + " §e" + stacks.size() + " items..."), true);

        stacks.sort((a, b) -> {
            int ca = categoryOrder(a);
            int cb = categoryOrder(b);
            if (ca != cb) return Integer.compare(ca, cb);
            return a.getItem().toString().compareTo(b.getItem().toString());
        });

        for (Slot slot : targetSlots) {
            if (!slot.getStack().isEmpty())
                client.interactionManager.clickSlot(syncId, slot.id, 0, SlotActionType.QUICK_MOVE, player);
        }
        client.interactionManager.clickSlot(syncId, -999, 0, SlotActionType.PICKUP, player);

        for (ItemStack desired : stacks) {
            for (Slot slot : handler.slots) {
                boolean isOther = (target == Target.CONTAINER)
                    ? (slot.inventory == player.getInventory() && slot.getIndex() < 36)
                    : (slot.inventory != player.getInventory());
                if (isOther && !slot.getStack().isEmpty() && ItemStack.areItemsEqual(slot.getStack(), desired)) {
                    client.interactionManager.clickSlot(syncId, slot.id, 0, SlotActionType.QUICK_MOVE, player);
                    break;
                }
            }
        }
        client.interactionManager.clickSlot(syncId, -999, 0, SlotActionType.PICKUP, player);
        return true;
    }

    private static int categoryOrder(ItemStack stack) {
        Item item = stack.getItem();
        if (item instanceof BowItem || item instanceof CrossbowItem || item instanceof TridentItem) return 1;
        if (item instanceof AxeItem) return 2;
        if (item instanceof ShovelItem) return 3;
        if (item instanceof HoeItem) return 4;
        if (stack.contains(DataComponentTypes.TOOL)) return 5;
        if (stack.contains(DataComponentTypes.FOOD)) return 6;
        if (item instanceof BlockItem) return 7;
        if (item instanceof BoatItem || item instanceof MinecartItem) return 8;
        return 9;
    }

    public static boolean throwAll(MinecraftClient client, Target target) {
        if (cooldown > 0) return false;
        cooldown = 10;
        ClientPlayerEntity player = client.player;
        if (player == null) return false;
        ScreenHandler handler = player.currentScreenHandler;
        if (handler == null) return false;
        int syncId = handler.syncId;

        List<Slot> targetSlots = getSlotsForTarget(handler, player, target);
        int count = 0;
        for (Slot slot : targetSlots) {
            if (!slot.getStack().isEmpty()) {
                client.interactionManager.clickSlot(syncId, slot.id, 1, SlotActionType.THROW, player);
                count++;
            }
        }
        if (count > 0) {
            String label = target == Target.CONTAINER ? "§e[Ném rương]" : "§e[Ném túi]";
            player.sendMessage(Text.literal(label + " §c" + count + " stacks"), true);
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
        int syncId = handler.syncId;

        List<Slot> targetSlots = getSlotsForTarget(handler, player, target);
        int count = 0;
        for (Slot slot : targetSlots) {
            if (!slot.getStack().isEmpty()) {
                client.interactionManager.clickSlot(syncId, slot.id, 0, SlotActionType.QUICK_MOVE, player);
                count++;
            }
        }
        if (count > 0) {
            String label = target == Target.CONTAINER ? "§d[Chuyển rương]" : "§d[Chuyển túi]";
            player.sendMessage(Text.literal(label + " §b" + count + " stacks"), true);
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
