package net.brokenscript.mod.network.clientbound;

import io.netty.buffer.ByteBuf;
import net.brokenscript.mod.BrokenScriptMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server -> Client: opens the Terminal CLI screen for the given terminal. */
public record OpenTerminalPayload(BlockPos terminalPos, int repairStep) implements CustomPacketPayload {
    public static final Type<OpenTerminalPayload> TYPE = new Type<>(BrokenScriptMod.id("open_terminal"));

    public static final StreamCodec<ByteBuf, OpenTerminalPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, OpenTerminalPayload::terminalPos,
            ByteBufCodecs.VAR_INT, OpenTerminalPayload::repairStep,
            OpenTerminalPayload::new);

    @Override
    public Type<OpenTerminalPayload> type() {
        return TYPE;
    }
}
