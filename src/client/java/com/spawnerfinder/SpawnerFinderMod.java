package com.spawnerfinder;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import java.util.ArrayList;
import java.util.List;

public class SpawnerFinderMod implements ClientModInitializer {
    public static boolean enabled = false;
    public static final List<SpawnerInfo> foundSpawners = new ArrayList<>();
    public static final List<SpawnerInfo> foundShulkers = new ArrayList<>();
    public static final List<SpawnerInfo> foundChests = new ArrayList<>();
    public static final List<SpawnerInfo> foundEnderChests = new ArrayList<>();
    public static final List<SpawnerInfo> foundPillagers = new ArrayList<>();
    public static final List<SpawnerInfo> foundStructures = new ArrayList<>();
    public static boolean showSpawner = true;
    public static boolean showShulker = true;
    public static boolean showChest = true;
    public static boolean showEnderChest = true;
    public static boolean showPillager = true;
    public static boolean showStructure = true;
    public static boolean showESP = true;
    public static boolean showBeam = true;
    public static boolean showHUD = true;
    public static boolean nightVision = false;
    private static double previousGamma = 0.5;
    private static int nightVisionCooldown = 0;

    private static KeyBinding toggleKey;
    private static KeyBinding cycleViewKey;
    private static KeyBinding nightVisionKey;
    private static KeyBinding autoSellKey;

    @Override
    public void onInitializeClient() {
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.spawnerfinder.toggle",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_F6,
            KeyBinding.Category.MISC
        ));

        cycleViewKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.spawnerfinder.cycle",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_F7,
            KeyBinding.Category.MISC
        ));

        nightVisionKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.spawnerfinder.nightvision",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_F8,
            KeyBinding.Category.MISC
        ));

        autoSellKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.spawnerfinder.autosell",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_F9,
            KeyBinding.Category.MISC
        ));

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
        InventoryUI.register();
        HudRenderCallback.EVENT.register(new SpawnerHUD());
        WorldRenderEvents.BEFORE_ENTITIES.register(new SpawnerRenderer());
    }

    private void onTick(MinecraftClient client) {
        if (toggleKey.wasPressed()) {
            enabled = !enabled;
            if (!enabled) {
                foundSpawners.clear();
                foundShulkers.clear();
                foundChests.clear();
                foundEnderChests.clear();
                foundPillagers.clear();
                foundStructures.clear();
            }
            if (client.player != null) {
                client.player.sendMessage(
                    Text.literal("§6[SpawnerFinder] §" + (enabled ? "aBẬT" : "cTẮT")),
                    true
                );
            }
        }

        if (cycleViewKey.wasPressed()) {
            if (showESP && showBeam && showHUD && showSpawner && showShulker && showChest && showEnderChest && showPillager && showStructure) {
                showESP = false;
            } else if (!showESP && showBeam && showHUD && showSpawner && showShulker && showChest && showEnderChest && showPillager && showStructure) {
                showBeam = false;
            } else if (!showESP && !showBeam && showHUD && showSpawner && showShulker && showChest && showEnderChest && showPillager && showStructure) {
                showHUD = false;
            } else if (!showESP && !showBeam && !showHUD && showSpawner && showShulker && showChest && showEnderChest && showPillager && showStructure) {
                showSpawner = false;
            } else if (!showESP && !showBeam && !showHUD && !showSpawner && showShulker && showChest && showEnderChest && showPillager && showStructure) {
                showChest = false;
            } else if (!showESP && !showBeam && !showHUD && !showSpawner && !showChest && showShulker && showEnderChest && showPillager && showStructure) {
                showEnderChest = false;
            } else if (!showESP && !showBeam && !showHUD && !showSpawner && !showChest && !showEnderChest && showShulker && showPillager && showStructure) {
                showPillager = false;
            } else if (!showESP && !showBeam && !showHUD && !showSpawner && !showChest && !showEnderChest && !showPillager && showShulker && showStructure) {
                showStructure = false;
            } else if (!showESP && !showBeam && !showHUD && !showSpawner && !showChest && !showEnderChest && !showPillager && !showStructure && showShulker) {
                showShulker = false;
            } else {
                showESP = true;
                showBeam = true;
                showHUD = true;
                showSpawner = true;
                showShulker = true;
                showChest = true;
                showEnderChest = true;
                showPillager = true;
                showStructure = true;
            }
            if (client.player != null) {
                client.player.sendMessage(
                    Text.literal("§6[SpawnerFinder] §fESP: " + (showESP ? "§a✔" : "§c✘") +
                        " §fBeam: " + (showBeam ? "§a✔" : "§c✘") +
                        " §fHUD: " + (showHUD ? "§a✔" : "§c✘") +
                        " §fSpawner: " + (showSpawner ? "§a✔" : "§c✘") +
                        " §fChest: " + (showChest ? "§a✔" : "§c✘") +
                        " §fEnder: " + (showEnderChest ? "§a✔" : "§c✘") +
                        " §fPillager: " + (showPillager ? "§a✔" : "§c✘") +
                        " §fStructure: " + (showStructure ? "§a✔" : "§c✘") +
                        " §fShulker: " + (showShulker ? "§a✔" : "§c✘")),
                    true
                );
            }
        }

        if (nightVisionCooldown > 0) nightVisionCooldown--;

        if (nightVisionKey.wasPressed() && nightVisionCooldown == 0) {
            nightVision = !nightVision;
            nightVisionCooldown = 10;
            if (nightVision) {
                previousGamma = client.options.getGamma().getValue();
                // Gamma 4.5 = nhìn tối rõ hơn nhưng không bị chói khi ra vùng sáng
                client.options.getGamma().setValue(4.5);
                if (client.player != null) {
                    client.player.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.NIGHT_VISION, -1, 0, false, false, false
                    ));
                    client.player.sendMessage(
                        Text.literal("§6[SpawnerFinder] §aNight Vision: BẬT"),
                        true
                    );
                }
            } else {
                client.options.getGamma().setValue(previousGamma);
                if (client.player != null) {
                    client.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
                    client.player.sendMessage(
                        Text.literal("§6[SpawnerFinder] §cNight Vision: TẮT"),
                        true
                    );
                }
            }
        }

        if (nightVision) {
            // Không cưỡng bức gamma liên tục — chỉ đảm bảo hiệu ứng Night Vision còn tồn tại
            if (client.player != null && !client.player.hasStatusEffect(StatusEffects.NIGHT_VISION)) {
                client.player.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.NIGHT_VISION, -1, 0, false, false, false
                ));
            }
        }

        if (autoSellKey.wasPressed()) {
            AutoSellManager.toggle();
        }

        AutoSellManager.tick(client);
        InventoryHelper.tick(client);

        if (!enabled) return;
        if (client.world == null || client.player == null) return;

        SpawnerTracker.scan(client);
        ChestTracker.scan(client);
        PillagerTracker.scan(client);
        StructureTracker.scan(client);
    }

    public static SpawnerInfo findNearest() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return null;
        SpawnerInfo nearest = null;
        double nearestDist = Double.MAX_VALUE;
        for (SpawnerInfo info : foundSpawners) {
            double dist = client.player.squaredDistanceTo(info.centerPos());
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = info;
            }
        }
        return nearest;
    }
}
