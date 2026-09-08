package net.brokenscript.mod.network.clientbound;

import io.netty.buffer.ByteBuf;
import net.brokenscript.mod.BrokenScriptMod;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server -> Client: orders a client-side sensory glitch. Sent by
 * {@code GlitchEntity} (Entity 303) and the stage 4 director.
 */
public record GlitchEffectPayload(Kind kind, int durationTicks) implements CustomPacketPayload {
    public static final Type<GlitchEffectPayload> TYPE = new Type<>(BrokenScriptMod.id("glitch_effect"));

    public enum Kind {
        /** Scrambled UI: shifted hotbar, corrupted item names, jittering HUD. */
        UI_SCRAMBLE,
        /** Temporary randomized mouse sensitivity. */
        MOUSE_SCRAMBLE,
        /** Full-screen fake crash overlay ("system failure"). */
        FAKE_CRASH,
        /** Inverted W/S movement - used by the stage 4 director. */
        INVERT_CONTROLS;

        public static final StreamCodec<ByteBuf, Kind> STREAM_CODEC =
                ByteBufCodecs.idMapper(i -> Kind.values()[i], Kind::ordinal);
    }

    public static final StreamCodec<ByteBuf, GlitchEffectPayload> STREAM_CODEC = StreamCodec.composite(
            Kind.STREAM_CODEC, GlitchEffectPayload::kind,
            ByteBufCodecs.VAR_INT, GlitchEffectPayload::durationTicks,
            GlitchEffectPayload::new);

    @Override
    public Type<GlitchEffectPayload> type() {
        return TYPE;
    }
}
