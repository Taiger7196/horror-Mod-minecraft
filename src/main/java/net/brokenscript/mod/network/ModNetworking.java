package net.brokenscript.mod.network;

import net.brokenscript.mod.block.TerminalBlockEntity;
import net.brokenscript.mod.network.clientbound.FakeSystemMessagePayload;
import net.brokenscript.mod.network.clientbound.GlitchEffectPayload;
import net.brokenscript.mod.network.clientbound.InfectionSyncPayload;
import net.brokenscript.mod.network.clientbound.OpenTerminalPayload;
import net.brokenscript.mod.network.clientbound.TerminalResponsePayload;
import net.brokenscript.mod.network.serverbound.TerminalCommandPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Payload registration using the 1.21.x {@code PayloadRegistrar} /
 * {@code CustomPacketPayload} system. Handlers run on the main thread of the
 * receiving side by default ({@code HandlerThread.MAIN}), so all world access
 * below is thread-safe without extra {@code enqueueWork} ceremony.
 *
 * <p>Clientbound handlers delegate into {@code client.ClientPayloadHandlers}
 * from inside the lambda body, so the client-only class is never loaded on a
 * dedicated server.</p>
 *
 * <p>Wired to the mod event bus in {@code BrokenScriptMod}'s constructor.</p>
 */
public final class ModNetworking {

    public static void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        // --- Clientbound (handled in client/ClientPayloadHandlers) ---
        registrar.playToClient(InfectionSyncPayload.TYPE, InfectionSyncPayload.STREAM_CODEC,
                (payload, context) -> net.brokenscript.mod.client.ClientPayloadHandlers.handleInfectionSync(payload));
        registrar.playToClient(GlitchEffectPayload.TYPE, GlitchEffectPayload.STREAM_CODEC,
                (payload, context) -> net.brokenscript.mod.client.ClientPayloadHandlers.handleGlitchEffect(payload));
        registrar.playToClient(OpenTerminalPayload.TYPE, OpenTerminalPayload.STREAM_CODEC,
                (payload, context) -> net.brokenscript.mod.client.ClientPayloadHandlers.handleOpenTerminal(payload));
        registrar.playToClient(TerminalResponsePayload.TYPE, TerminalResponsePayload.STREAM_CODEC,
                (payload, context) -> net.brokenscript.mod.client.ClientPayloadHandlers.handleTerminalResponse(payload));
        registrar.playToClient(FakeSystemMessagePayload.TYPE, FakeSystemMessagePayload.STREAM_CODEC,
                (payload, context) -> net.brokenscript.mod.client.ClientPayloadHandlers.handleFakeSystemMessage(payload));

        // --- Serverbound ---
        registrar.playToServer(TerminalCommandPayload.TYPE, TerminalCommandPayload.STREAM_CODEC,
                ModNetworking::handleTerminalCommand);
    }

    /**
     * Validates and executes a CLI command against the terminal block entity.
     * Runs on the server main thread; every claim from the client is
     * re-checked (distance, block presence) before the world is touched.
     */
    private static void handleTerminalCommand(TerminalCommandPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        var level = player.level();  // any Level is fine here; block entity lookup below
        var pos = payload.terminalPos();
        if (!level.isLoaded(pos)
                || player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) > 64.0D
                || !(level.getBlockEntity(pos) instanceof TerminalBlockEntity terminal)) {
            return; // stale or spoofed packet - silently drop
        }
        int stepBefore = terminal.getRepairStep();
        Component response = terminal.handleCommand(player, payload.command());
        boolean accepted = terminal.getRepairStep() > stepBefore;
        PacketDistributor.sendToPlayer(player,
                new TerminalResponsePayload(response.getString(), terminal.getRepairStep(), accepted));
    }

    private ModNetworking() {}
}
