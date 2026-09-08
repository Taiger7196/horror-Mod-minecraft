package net.brokenscript.mod.network.clientbound;

import net.brokenscript.mod.BrokenScriptMod;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server -> Client: mirrors the world infection level so client-side systems
 * (ambience, overlays, input glitcher intensity) can scale with the stage
 * without ever being authoritative.
 */
public record InfectionSyncPayload(double infectionLevel) implements CustomPacketPayload {
    public static final Type<InfectionSyncPayload> TYPE = new Type<>(BrokenScriptMod.id("infection_sync"));

    public static final StreamCodec<io.netty.buffer.ByteBuf, InfectionSyncPayload> STREAM_CODEC =
            ByteBufCodecs.DOUBLE.map(InfectionSyncPayload::new, InfectionSyncPayload::infectionLevel);

    @Override
    public Type<InfectionSyncPayload> type() {
        return TYPE;
    }
}
