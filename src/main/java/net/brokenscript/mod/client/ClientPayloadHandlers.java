package net.brokenscript.mod.client;

import net.brokenscript.mod.client.screen.TerminalScreen;
import net.brokenscript.mod.network.clientbound.FakeSystemMessagePayload;
import net.brokenscript.mod.network.clientbound.GlitchEffectPayload;
import net.brokenscript.mod.network.clientbound.InfectionSyncPayload;
import net.brokenscript.mod.network.clientbound.OpenTerminalPayload;
import net.brokenscript.mod.network.clientbound.TerminalResponsePayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Client-side payload dispatch. Only ever class-loaded on the physical client
 * (the lambdas in {@code ModNetworking} defer loading until a payload actually
 * arrives, which never happens on a dedicated server). Handlers already run on
 * the client main thread ({@code HandlerThread.MAIN}).
 */
public final class ClientPayloadHandlers {

    public static void handleInfectionSync(InfectionSyncPayload payload) {
        ClientHorrorState.setInfectionLevel(payload.infectionLevel());
    }

    public static void handleGlitchEffect(GlitchEffectPayload payload) {
        switch (payload.kind()) {
            case UI_SCRAMBLE -> ClientHorrorState.startUiScramble(payload.durationTicks());
            case MOUSE_SCRAMBLE -> ClientHorrorState.startMouseScramble(payload.durationTicks());
            case FAKE_CRASH -> ClientHorrorState.startFakeCrash(payload.durationTicks());
            case INVERT_CONTROLS -> ClientHorrorState.startInvertControls(payload.durationTicks());
        }
    }

    public static void handleOpenTerminal(OpenTerminalPayload payload) {
        Minecraft.getInstance().setScreen(new TerminalScreen(payload.terminalPos(), payload.repairStep()));
    }

    public static void handleTerminalResponse(TerminalResponsePayload payload) {
        if (Minecraft.getInstance().screen instanceof TerminalScreen terminal) {
            terminal.acceptResponse(payload.line(), payload.repairStep(), payload.accepted());
        }
    }

    /**
     * Fake "Null joined the game" chat line + phantom tab-list entry, both
     * fabricated purely on this client. Nothing real joined anything.
     */
    public static void handleFakeSystemMessage(FakeSystemMessagePayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        String key = payload.joined() ? "multiplayer.player.joined" : "multiplayer.player.left";
        minecraft.gui.getChat().addMessage(
                Component.translatable(key, Component.literal(payload.phantomName()))
                        .withStyle(ChatFormatting.YELLOW));
        if (payload.joined()) {
            PhantomRoster.addPhantom(payload.phantomName());
        } else {
            PhantomRoster.removePhantom(payload.phantomName());
        }
    }

    private ClientPayloadHandlers() {}
}
