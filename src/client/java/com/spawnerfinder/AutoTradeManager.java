package com.spawnerfinder;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import net.minecraft.village.TradedItem;

import java.util.Optional;

public class AutoTradeManager {
    private static boolean autoTrading = false;
    private static int tradeCooldown = 0;
    private static int idleDelay = 0;

    public static boolean toggle() {
        if (autoTrading) stop(); else start();
        return autoTrading;
    }

    public static void start() {
        autoTrading = true;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null)
            client.player.sendMessage(Text.literal("§6[SpawnerFinder] §aAuto Trade: BẬT"), true);
    }

    public static void stop() {
        autoTrading = false;
        tradeCooldown = 0;
        idleDelay = 0;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null)
            client.player.sendMessage(Text.literal("§6[SpawnerFinder] §cAuto Trade: TẮT"), true);
    }

    public static boolean isActive() { return autoTrading; }

    public static void tick(MinecraftClient client) {
        if (!autoTrading) return;
        if (idleDelay > 0) { idleDelay--; return; }
        if (tradeCooldown > 0) { tradeCooldown--; return; }

        ClientPlayerEntity player = client.player;
        if (player == null) return;
        if (!(player.currentScreenHandler instanceof MerchantScreenHandler handler)) { stop(); return; }
        if (handler.getRecipes() == null) return;

        TradeOfferList offers = handler.getRecipes();
        int ironCount = countItem(player, Items.IRON_INGOT);

        for (int i = 0; i < offers.size(); i++) {
            TradeOffer offer = offers.get(i);
            if (offer.isDisabled()) continue;
            if (offer.getMaxUses() <= offer.getUses()) continue;

            TradedItem firstBuy = offer.getFirstBuyItem();
            Optional<TradedItem> secondBuy = offer.getSecondBuyItem();

            int ironNeed = 0;
            boolean tradesForEmerald = offer.getSellItem().isOf(Items.EMERALD);

            if (firstBuy.item() == Items.IRON_INGOT || firstBuy.item() == Items.IRON_NUGGET) {
                ironNeed += firstBuy.count();
            }
            if (secondBuy.isPresent()) {
                TradedItem s = secondBuy.get();
                if (s.item() == Items.IRON_INGOT || s.item() == Items.IRON_NUGGET) {
                    ironNeed += s.count();
                }
            }

            if (!tradesForEmerald || ironNeed == 0) continue;
            if (ironCount < ironNeed) continue;

            int slot2 = 2;
            handler.setRecipeIndex(i);
            client.interactionManager.clickSlot(
                handler.syncId, slot2, 0, SlotActionType.QUICK_MOVE, player
            );
            tradeCooldown = 5;
            idleDelay = 2;
            return;
        }
    }

    private static int countItem(ClientPlayerEntity player, net.minecraft.item.Item item) {
        int count = 0;
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isOf(item)) count += stack.getCount();
        }
        return count;
    }
}
