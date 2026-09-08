package net.brokenscript.mod.util;

import net.brokenscript.mod.BrokenScriptMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

/**
 * Custom damage types. The JSON definition lives at
 * {@code data/brokenscript/damage_type/reaper.json} and is added to
 * {@code minecraft:bypasses_armor} + {@code minecraft:bypasses_shield} via
 * data tags, so The Reaper ignores standard armor calculations entirely.
 */
public final class ModDamageTypes {
    public static final ResourceKey<DamageType> REAPER =
            ResourceKey.create(Registries.DAMAGE_TYPE, BrokenScriptMod.id("reaper"));

    public static DamageSource reaper(ServerLevel level, @Nullable Entity attacker) {
        return new DamageSource(
                level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(REAPER),
                attacker);
    }

    private ModDamageTypes() {}
}
