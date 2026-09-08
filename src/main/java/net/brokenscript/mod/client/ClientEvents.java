package net.brokenscript.mod.client;

import net.brokenscript.mod.BrokenScriptMod;
import net.brokenscript.mod.Config;
import net.brokenscript.mod.infection.InfectionStage;
import net.brokenscript.mod.util.CorruptedNames;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * Client game-bus listeners: the input glitcher, tooltip corruption, and the
 * per-tick countdowns for active glitch effects.
 */
@EventBusSubscriber(modid = BrokenScriptMod.MODID, value = Dist.CLIENT)
public final class ClientEvents {

    // --- effect timers + mouse sensitivity scramble ---

    @SubscribeEvent
    public static void onClientTickPost(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean wasScrambling = ClientHorrorState.mouseScrambleActive();
        ClientHorrorState.tickDown();

        if (minecraft.player == null) {
            return;
        }

        if (ClientHorrorState.mouseScrambleActive() && Config.ALLOW_INPUT_GLITCHES.getAsBoolean()) {
            // Save the player's real sensitivity once, then jitter around it.
            if (ClientHorrorState.savedSensitivity() == null) {
                ClientHorrorState.swapSavedSensitivity(minecraft.options.sensitivity().get());
            }
            if (ClientHorrorState.RANDOM.nextInt(10) == 0) {
                double base = ClientHorrorState.savedSensitivity();
                double jitter = Math.clamp(
                        base * (0.3D + ClientHorrorState.RANDOM.nextDouble() * 1.6D), 0.05D, 1.0D);
                minecraft.options.sensitivity().set(jitter);
            }
        } else if (wasScrambling && !ClientHorrorState.mouseScrambleActive()) {
            // Effect just ended: restore the player's actual setting. Always.
            Double saved = ClientHorrorState.swapSavedSensitivity(null);
            if (saved != null) {
                minecraft.options.sensitivity().set(saved);
            }
        }
    }

    // --- keybinding swap: W/S (and A/D) inversion during peak horror ---

    @SubscribeEvent
    public static void onMovementInputUpdate(MovementInputUpdateEvent event) {
        if (!ClientHorrorState.invertControlsActive() || !Config.ALLOW_INPUT_GLITCHES.getAsBoolean()) {
            return;
        }
        ClientInput input = event.getInput();
        // moveVector is AT-opened; negating it inverts W/S (and A/D) while
        // keeping vanilla's slowdown/sneak math intact (it ran before this event).
        input.moveVector = input.moveVector.scale(-1.0F);
    }

    // --- item rename interceptor: hovered items become corrupted file names ---

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        if (!ClientHorrorState.uiScrambleActive()
                && !ClientHorrorState.stage().atLeast(InfectionStage.COLLAPSE)) {
            return;
        }
        // Roughly 1 in 6 hovers shows a corrupted name; unstable on purpose.
        if (ClientHorrorState.RANDOM.nextInt(6) == 0 && !event.getToolTip().isEmpty()) {
            event.getToolTip().set(0, net.minecraft.network.chat.Component.literal(
                            CorruptedNames.randomOf(CorruptedNames.CORRUPTED_FILE_NAMES, ClientHorrorState.RANDOM))
                    .withStyle(net.minecraft.ChatFormatting.DARK_RED));
        }
    }

    // --- chat spoofing: incoming messages get visibly corrupted at stage 3+ ---

    @SubscribeEvent
    public static void onClientChatReceived(net.neoforged.neoforge.client.event.ClientChatReceivedEvent event) {
        if (!ClientHorrorState.stage().atLeast(InfectionStage.HUNTING)) {
            return;
        }
        // 1 in 5 messages arrives partially eaten by the script.
        if (ClientHorrorState.RANDOM.nextInt(5) == 0) {
            event.setMessage(CorruptedNames.corrupt(event.getMessage(), ClientHorrorState.RANDOM));
        }
    }

    // --- cleanup on disconnect ---

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        PhantomRoster.clear();
        Double saved = ClientHorrorState.swapSavedSensitivity(null);
        if (saved != null) {
            Minecraft.getInstance().options.sensitivity().set(saved);
        }
        ClientHorrorState.setInfectionLevel(0.0D);
    }
}
