package com.spawnerfinder;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.SelectMerchantTradeC2SPacket;
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

    public static boolean toggle() {
        if (autoTrading) stop(); else start();
        return autoTrading;
    }

    public static void start() {
        autoTrading = true;
        tradeCooldown = 0;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null)
            client.player.sendMessage(Text.literal("§6[SpawnerFinder] §aAuto Trade: BẬT"), true);
    }

    public static void stop() {
        autoTrading = false;
        tradeCooldown = 0;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null)
            client.player.sendMessage(Text.literal("§6[SpawnerFinder] §cAuto Trade: TẮT"), true);
    }

    public static boolean isActive() { return autoTrading; }

    public static void tick(MinecraftClient client) {
        if (!autoTrading) return;
        if (tradeCooldown > 0) { tradeCooldown--; return; }

        ClientPlayerEntity player = client.player;
        if (player == null) return;
        if (!(player.currentScreenHandler instanceof MerchantScreenHandler handler)) return;
        if (handler.getRecipes() == null) return;

        TradeOfferList offers = handler.getRecipes();
        int ironCount = 0;
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack s = player.getInventory().getStack(i);
            if (s.isOf(Items.IRON_INGOT)) ironCount += s.getCount() * 9;
            else if (s.isOf(Items.IRON_NUGGET)) ironCount += s.getCount();
        }

        if (ironCount <= 0) {
            player.closeScreen();
            return;
        }

        for (int i = 0; i < offers.size(); i++) {
            TradeOffer offer = offers.get(i);
            if (offer.isDisabled()) continue;
            if (offer.getMaxUses() <= offer.getUses()) continue;
            if (!offer.getSellItem().isOf(Items.EMERALD)) continue;

            TradedItem firstBuy = offer.getFirstBuyItem();
            Optional<TradedItem> secondBuy = offer.getSecondBuyItem();

            int ironNeed = 0;
            Item buy1 = firstBuy.item().value();
            if (buy1 == Items.IRON_INGOT) ironNeed += firstBuy.count() * 9;
            else if (buy1 == Items.IRON_NUGGET) ironNeed += firstBuy.count();
            else continue;

            if (secondBuy.isPresent()) {
                TradedItem s = secondBuy.get();
                Item buy2 = s.item().value();
                if (buy2 == Items.IRON_INGOT) ironNeed += s.count() * 9;
                else if (buy2 == Items.IRON_NUGGET) ironNeed += s.count();
                else continue;
            }

            if (ironCount < ironNeed) continue;

            handler.setRecipeIndex(i);
            client.getNetworkHandler().sendPacket(new SelectMerchantTradeC2SPacket(i));
            client.interactionManager.clickSlot(handler.syncId, 2, 0, SlotActionType.QUICK_MOVE, player);
            tradeCooldown = 3;
            return;
        }

        player.closeScreen();
    }
}
