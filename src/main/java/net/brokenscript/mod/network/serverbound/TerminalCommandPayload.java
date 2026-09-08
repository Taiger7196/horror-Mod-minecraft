package net.brokenscript.mod.network.serverbound;

import io.netty.buffer.ByteBuf;
import net.brokenscript.mod.BrokenScriptMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client -> Server: the player typed a command into the Terminal CLI. */
public record TerminalCommandPayload(BlockPos terminalPos, String command) implements CustomPacketPayload {
    public static final Type<TerminalCommandPayload> TYPE = new Type<>(BrokenScriptMod.id("terminal_command"));

    public static final StreamCodec<ByteBuf, TerminalCommandPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, TerminalCommandPayload::terminalPos,
            ByteBufCodecs.stringUtf8(256), TerminalCommandPayload::command,
            TerminalCommandPayload::new);

    @Override
    public Type<TerminalCommandPayload> type() {
        return TYPE;
    }
}
