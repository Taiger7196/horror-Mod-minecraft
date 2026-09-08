package net.brokenscript.mod.registry;

import net.brokenscript.mod.BrokenScriptMod;
import net.brokenscript.mod.entity.GlitchEntity;
import net.brokenscript.mod.entity.HerobrineEntity;
import net.brokenscript.mod.entity.LickEntity;
import net.brokenscript.mod.entity.NullEntity;
import net.brokenscript.mod.entity.TheReaperEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, BrokenScriptMod.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<HerobrineEntity>> HEROBRINE =
            register("herobrine", EntityType.Builder.of(HerobrineEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F).clientTrackingRange(48).fireImmune());

    public static final DeferredHolder<EntityType<?>, EntityType<NullEntity>> NULL =
            register("null", EntityType.Builder.of(NullEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F).clientTrackingRange(64).fireImmune());

    /** Entity 303 - close-range glitcher. */
    public static final DeferredHolder<EntityType<?>, EntityType<GlitchEntity>> ENTITY_303 =
            register("entity_303", EntityType.Builder.of(GlitchEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F).clientTrackingRange(64).fireImmune());

    /** Entity 505 - long-range spectator variant of 303. */
    public static final DeferredHolder<EntityType<?>, EntityType<GlitchEntity>> ENTITY_505 =
            register("entity_505", EntityType.Builder.of(GlitchEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F).clientTrackingRange(96).fireImmune());

    /** Green Steve's crooked friend; digs the cursed crosses. */
    public static final DeferredHolder<EntityType<?>, EntityType<LickEntity>> LICK =
            register("lick", EntityType.Builder.of(LickEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F).clientTrackingRange(48));

    public static final DeferredHolder<EntityType<?>, EntityType<TheReaperEntity>> THE_REAPER =
            register("the_reaper", EntityType.Builder.of(TheReaperEntity::new, MobCategory.MONSTER)
                    .sized(0.8F, 2.4F).clientTrackingRange(64).fireImmune());

    private static <T extends Entity> DeferredHolder<EntityType<?>, EntityType<T>> register(
            String name, EntityType.Builder<T> builder) {
        return ENTITY_TYPES.register(name,
                () -> builder.build(ResourceKey.create(Registries.ENTITY_TYPE, BrokenScriptMod.id(name))));
    }

    /** Mod-bus listener wired up in the main mod constructor. */
    public static void onRegisterAttributes(EntityAttributeCreationEvent event) {
        event.put(HEROBRINE.get(), HerobrineEntity.createAttributes().build());
        event.put(NULL.get(), NullEntity.createAttributes().build());
        event.put(ENTITY_303.get(), GlitchEntity.createAttributes().build());
        event.put(ENTITY_505.get(), GlitchEntity.createAttributes().build());
        event.put(LICK.get(), LickEntity.createAttributes().build());
        event.put(THE_REAPER.get(), TheReaperEntity.createAttributes().build());
    }

    private ModEntities() {}
}
