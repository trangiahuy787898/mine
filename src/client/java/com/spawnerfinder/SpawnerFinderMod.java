package com.spawnerfinder;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
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
    public static boolean showESP = true;
    public static boolean showBeam = true;
    public static boolean showHUD = true;

    public static boolean scanExtra = false;
    public static boolean showChest = true;
    public static boolean showEnderChest = true;
    public static boolean showPillager = true;
    public static boolean showStructure = true;

    public static boolean nightVision = false;
    private static double previousGamma = 0.5;
    private static int nightVisionCooldown = 0;

    private static KeyBinding toggleKey;
    private static KeyBinding cycleViewKey;
    private static KeyBinding nightVisionKey;
    private static KeyBinding autoTradeKey;
    private static KeyBinding scanExtraKey;

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

        autoTradeKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.spawnerfinder.autotrade",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_F10,
            KeyBinding.Category.MISC
        ));

        scanExtraKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.spawnerfinder.scanextra",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_F9,
            KeyBinding.Category.MISC
        ));

        ClientChunkEvents.CHUNK_LOAD.register((world, chunk) -> {
            SpawnerTracker.onChunkLoad(chunk, world);
        });

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
        HudRenderCallback.EVENT.register(new SpawnerHUD());
        WorldRenderEvents.BEFORE_ENTITIES.register(new SpawnerRenderer());
    }

    private void onTick(MinecraftClient client) {
        if (toggleKey.wasPressed()) {
            enabled = !enabled;
            if (!enabled) {
                SpawnerTracker.stopProbing();
                SpawnerTracker.resetCaches();
            } else {
                if (!scanExtra) {
                    foundChests.clear();
                    foundEnderChests.clear();
                    foundPillagers.clear();
                    foundStructures.clear();
                }
                SpawnerTracker.startProbing();
            }
            if (client.player != null) {
                client.player.sendMessage(
                    Text.literal("§6[SpawnerFinder] §" + (enabled ? "aBẬT §7(Đang quét " + SpawnerTracker.CHUNK_RADIUS + "x" + SpawnerTracker.CHUNK_RADIUS + " chunks...)" : "cTẮT")),
                    true
                );
            }
        }

        if (cycleViewKey.wasPressed()) {
            if (showESP && showBeam && showHUD && showSpawner && showShulker) {
                showESP = false;
            } else if (!showESP && showBeam && showHUD && showSpawner && showShulker) {
                showBeam = false;
            } else if (!showESP && !showBeam && showHUD && showSpawner && showShulker) {
                showHUD = false;
            } else if (!showESP && !showBeam && !showHUD && showSpawner && showShulker) {
                showSpawner = false;
            } else if (!showESP && !showBeam && !showHUD && !showSpawner && showShulker) {
                showShulker = false;
            } else {
                showESP = true;
                showBeam = true;
                showHUD = true;
                showSpawner = true;
                showShulker = true;
            }
            if (client.player != null) {
                client.player.sendMessage(
                    Text.literal("§6[SpawnerFinder] §fESP: " + (showESP ? "§a✔" : "§c✘") +
                        " §fBeam: " + (showBeam ? "§a✔" : "§c✘") +
                        " §fHUD: " + (showHUD ? "§a✔" : "§c✘") +
                        " §fSpawner: " + (showSpawner ? "§a✔" : "§c✘") +
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
            if (client.player != null && !client.player.hasStatusEffect(StatusEffects.NIGHT_VISION)) {
                client.player.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.NIGHT_VISION, -1, 0, false, false, false
                ));
            }
        }

        if (autoTradeKey.wasPressed()) {
            AutoTradeManager.toggle();
        }
        AutoTradeManager.tick(client);
        InventoryHelper.tick(client);

        if (scanExtraKey.wasPressed()) {
            scanExtra = !scanExtra;
            if (client.player != null) {
                client.player.sendMessage(
                    Text.literal("§6[SpawnerFinder] §fQuét mở rộng: " + (scanExtra ? "§aBẬT §7(rương, pillager, cấu trúc)" : "§cTẮT")),
                    true
                );
            }
            if (!scanExtra) {
                foundChests.clear();
                foundEnderChests.clear();
                foundPillagers.clear();
                foundStructures.clear();
                SpawnerTracker.clearExtraCaches();
            }
        }

        if (!enabled && !scanExtra) return;
        if (client.world == null || client.player == null) return;

        SpawnerTracker.tick(client);
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
