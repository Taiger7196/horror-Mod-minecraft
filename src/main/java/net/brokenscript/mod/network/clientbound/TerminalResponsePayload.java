package net.brokenscript.mod.network.clientbound;

import io.netty.buffer.ByteBuf;
import net.brokenscript.mod.BrokenScriptMod;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server -> Client: a CLI response line for the terminal screen, plus the
 * authoritative repair step so the client HUD can't drift out of sync.
 */
public record TerminalResponsePayload(String line, int repairStep, boolean accepted)
        implements CustomPacketPayload {
    public static final Type<TerminalResponsePayload> TYPE = new Type<>(BrokenScriptMod.id("terminal_response"));

    public static final StreamCodec<ByteBuf, TerminalResponsePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, TerminalResponsePayload::line,
            ByteBufCodecs.VAR_INT, TerminalResponsePayload::repairStep,
            ByteBufCodecs.BOOL, TerminalResponsePayload::accepted,
            TerminalResponsePayload::new);

    @Override
    public Type<TerminalResponsePayload> type() {
        return TYPE;
    }
}
