package com.example.autofish;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AutoFishClient implements ClientModInitializer {

    private static final Random RNG = new Random();
    private static final Pattern MATH =
            Pattern.compile("(-?\\d+)\\s*([+\\-*xX\u00d7/\u00f7])\\s*(-?\\d+)");

    // Ayarlar (settings ekrani bunlari degistirir)
    public static boolean fishEnabled = false;
    public static boolean captchaEnabled = true;

    private static int cooldown = 0;
    private static String pendingAnswer = null;
    private static int answerDelay = 0;

    private static KeyBinding toggleKey;
    private static KeyBinding settingsKey;

    public static void setFishEnabled(boolean value) {
        fishEnabled = value;
        cooldown = 20;
    }

    @Override
    public void onInitializeClient() {
        // Tus ayarlari: Ayarlar > Kontroller > "AutoFish" kategorisinden degistirilebilir
        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of("autofish", "main"));

        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.autofish.toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, category));
        settingsKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.autofish.settings", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_H, category));

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("autofish").executes(ctx -> {
                setFishEnabled(!fishEnabled);
                ctx.getSource().sendFeedback(Text.literal("AutoFish: " + (fishEnabled ? "ACIK" : "KAPALI")));
                return 1;
            }));
            dispatcher.register(ClientCommandManager.literal("autocaptcha").executes(ctx -> {
                captchaEnabled = !captchaEnabled;
                ctx.getSource().sendFeedback(Text.literal("AutoCaptcha: " + (captchaEnabled ? "ACIK" : "KAPALI")));
                return 1;
            }));
        });

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> handleChat(message.getString()));
        ClientReceiveMessageEvents.CHAT.register((message, signed, sender, params, ts) -> handleChat(message.getString()));

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    private void handleChat(String text) {
        if (!captchaEnabled || pendingAnswer != null) return;

        Matcher m = MATH.matcher(text);
        if (!m.find()) return;

        long a = Long.parseLong(m.group(1));
        long b = Long.parseLong(m.group(3));
        Long result = switch (m.group(2)) {
            case "+" -> a + b;
            case "-" -> a - b;
            case "*", "x", "X", "\u00d7" -> a * b;
            case "/", "\u00f7" -> (b != 0 && a % b == 0) ? a / b : null;
            default -> null;
        };
        if (result == null) return;

        pendingAnswer = String.valueOf(result);
        answerDelay = 30 + RNG.nextInt(50);
    }

    private void onTick(MinecraftClient client) {
        if (client.player == null || client.interactionManager == null) return;

        // Tus kontrolleri
        while (toggleKey.wasPressed()) {
            setFishEnabled(!fishEnabled);
            client.player.sendMessage(
                    Text.literal("AutoFish: " + (fishEnabled ? "ACIK" : "KAPALI")), true);
        }
        while (settingsKey.wasPressed()) {
            client.setScreen(new SettingsScreen());
        }

        if (pendingAnswer != null && --answerDelay <= 0) {
            client.player.networkHandler.sendChatMessage(pendingAnswer);
            // Sunucu komutla cevap istiyorsa:
            // client.player.networkHandler.sendChatCommand("captcha " + pendingAnswer);
            pendingAnswer = null;
        }

        if (!fishEnabled) return;
        if (cooldown > 0) { cooldown--; return; }
        if (!client.player.getMainHandStack().isOf(Items.FISHING_ROD)) return;

        FishingBobberEntity hook = client.player.fishHook;

        if (hook == null) {
            client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
            client.player.swingHand(Hand.MAIN_HAND);
            cooldown = 20 + RNG.nextInt(20);
            return;
        }

        if (hook.age > 40 && hook.isTouchingWater() && hook.getVelocity().y < -0.04) {
            client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
            client.player.swingHand(Hand.MAIN_HAND);
            cooldown = 15 + RNG.nextInt(20);
        }
    }
}
