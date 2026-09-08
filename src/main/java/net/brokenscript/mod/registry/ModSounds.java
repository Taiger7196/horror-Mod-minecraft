package net.brokenscript.mod.registry;

import net.brokenscript.mod.BrokenScriptMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Sound events. The actual audio is remapped onto vanilla ogg files through
 * {@code assets/brokenscript/sounds.json}, so no audio assets need shipping -
 * the horror comes from hearing familiar sounds where they should not be.
 */
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, BrokenScriptMod.MODID);

    /** Fake cave ambience played behind the player. */
    public static final DeferredHolder<SoundEvent, SoundEvent> WHISPER = register("whisper");
    /** Warden-heart thump used for stage escalation stings. */
    public static final DeferredHolder<SoundEvent, SoundEvent> HEARTBEAT = register("heartbeat");
    /** Portal-noise static burst used by the glitch entities. */
    public static final DeferredHolder<SoundEvent, SoundEvent> STATIC = register("static");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(BrokenScriptMod.id(name)));
    }

    private ModSounds() {}
}
