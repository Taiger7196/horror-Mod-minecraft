package net.brokenscript.mod.network.clientbound;

import io.netty.buffer.ByteBuf;
import net.brokenscript.mod.BrokenScriptMod;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server -> Client: injects a fabricated "system" line into the chat GUI
 * ("Null joined the game", "Entity_303 left the game", ...). Sent as a custom
 * payload rather than a real system chat packet so the client handler can also
 * kick off tab-list spoofing for the same phantom name.
 */
public record FakeSystemMessagePayload(String phantomName, boolean joined) implements CustomPacketPayload {
    public static final Type<FakeSystemMessagePayload> TYPE = new Type<>(BrokenScriptMod.id("fake_system_message"));

    public static final StreamCodec<ByteBuf, FakeSystemMessagePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, FakeSystemMessagePayload::phantomName,
            ByteBufCodecs.BOOL, FakeSystemMessagePayload::joined,
            FakeSystemMessagePayload::new);

    @Override
    public Type<FakeSystemMessagePayload> type() {
        return TYPE;
    }
}
