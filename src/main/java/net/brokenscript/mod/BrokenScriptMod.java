package net.brokenscript.mod;

import com.mojang.logging.LogUtils;
import net.brokenscript.mod.network.ModNetworking;
import net.brokenscript.mod.registry.ModBlockEntities;
import net.brokenscript.mod.registry.ModBlocks;
import net.brokenscript.mod.registry.ModEntities;
import net.brokenscript.mod.registry.ModItems;
import net.brokenscript.mod.registry.ModSounds;
import net.brokenscript.mod.registry.ModStructures;
import net.brokenscript.mod.registry.ModTabs;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

/**
 * Broken Script 3.0 (Fan-Made) - a long-form psychological & meta-horror mod.
 *
 * <p>Everything hangs off the mod event bus here: deferred registers, entity
 * attributes and payload (networking) registration. Game-bus listeners live in
 * {@code event/CommonEvents} and the client-only classes under {@code client/}.</p>
 */
@Mod(BrokenScriptMod.MODID)
public final class BrokenScriptMod {
    public static final String MODID = "brokenscript";
    public static final Logger LOGGER = LogUtils.getLogger();

    public BrokenScriptMod(IEventBus modEventBus, ModContainer modContainer) {
        // Deferred registers - all game objects are registered through these.
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITY_TYPES.register(modEventBus);
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModSounds.SOUND_EVENTS.register(modEventBus);
        ModStructures.STRUCTURE_TYPES.register(modEventBus);
        ModStructures.STRUCTURE_PIECES.register(modEventBus);
        ModTabs.CREATIVE_MODE_TABS.register(modEventBus);

        // Mod-bus listeners (registered imperatively so no @EventBusSubscriber bus
        // parameter is needed - RegisterPayloadHandlersEvent and
        // EntityAttributeCreationEvent are both mod-bus events).
        modEventBus.addListener(ModEntities::onRegisterAttributes);
        modEventBus.addListener(ModNetworking::onRegisterPayloadHandlers);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        LOGGER.info("[brokenscript] loading... do not read the logs.");
    }

    /** Shorthand for {@code brokenscript:<path>} resource locations. */
    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
