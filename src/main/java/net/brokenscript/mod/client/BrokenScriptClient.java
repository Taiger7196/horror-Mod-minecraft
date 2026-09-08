package net.brokenscript.mod.client;

import net.brokenscript.mod.BrokenScriptMod;
import net.brokenscript.mod.client.hud.FakeCrashOverlay;
import net.brokenscript.mod.client.hud.GlitchHudLayer;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * Client-only mod entry ({@code dist = Dist.CLIENT}); never loaded on a
 * dedicated server. The static handlers below listen on the MOD event bus
 * (registrations); gameplay-time client events live in {@link ClientEvents}.
 */
@Mod(value = BrokenScriptMod.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = BrokenScriptMod.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class BrokenScriptClient {

    public BrokenScriptClient(ModContainer container) {
    }

    /**
     * Entity renderers. Humanoid ghosts reuse invisible renderers here as a
     * placeholder wired for later art: swap {@code NoopRenderer} for
     * {@code HumanoidMobRenderer} subclasses once the textures/models land.
     * (Invisible stalkers that dowse your torches are plenty scary meanwhile.)
     */
    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(net.brokenscript.mod.registry.ModEntities.HEROBRINE.get(), NoopRenderer::new);
        event.registerEntityRenderer(net.brokenscript.mod.registry.ModEntities.NULL.get(), NoopRenderer::new);
        event.registerEntityRenderer(net.brokenscript.mod.registry.ModEntities.ENTITY_303.get(), NoopRenderer::new);
        event.registerEntityRenderer(net.brokenscript.mod.registry.ModEntities.ENTITY_505.get(), NoopRenderer::new);
        event.registerEntityRenderer(net.brokenscript.mod.registry.ModEntities.LICK.get(), NoopRenderer::new);
        event.registerEntityRenderer(net.brokenscript.mod.registry.ModEntities.THE_REAPER.get(), NoopRenderer::new);
    }

    /** HUD layers: glitch jitter under the crosshair layer, crash overlay above everything. */
    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR,
                BrokenScriptMod.id("glitch_hud"), new GlitchHudLayer());
        event.registerAboveAll(BrokenScriptMod.id("fake_crash"), new FakeCrashOverlay());
    }
}
