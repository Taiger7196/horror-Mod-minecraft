package net.brokenscript.mod.event;

import net.brokenscript.mod.BrokenScriptMod;
import net.brokenscript.mod.entity.GlitchEntity;
import net.brokenscript.mod.entity.HerobrineEntity;
import net.brokenscript.mod.entity.LickEntity;
import net.brokenscript.mod.entity.TheReaperEntity;
import net.brokenscript.mod.horror.HorrorDirector;
import net.brokenscript.mod.infection.InfectionSavedData;
import net.brokenscript.mod.infection.InfectionStage;
import net.brokenscript.mod.network.clientbound.InfectionSyncPayload;
import net.brokenscript.mod.registry.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Common/server game-bus listeners. Everything here executes on the logical
 * server; client-only handlers live under {@code net.brokenscript.mod.client}.
 */
@EventBusSubscriber(modid = BrokenScriptMod.MODID)
public final class CommonEvents {

    /** Drives the whole haunting - one director step per server tick batch. */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        HorrorDirector.tick(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        HorrorDirector.reset();
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        HorrorDirector.reset();
    }

    /** Late-joining players immediately learn the world's infection level. */
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            double level = InfectionSavedData.get(player.level().getServer()).level();
            PacketDistributor.sendToPlayer(player, new InfectionSyncPayload(level));
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        HorrorDirector.onPlayerLeave(event.getEntity().getUUID());
    }

    /**
     * "Sealing" a stage 3/4 entity drops Source Code Fragments - but only at
     * stage 3+, so farming them early is impossible. Complements the JSON loot
     * tables (which handle the guaranteed structure loot).
     */
    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        var entity = event.getEntity();
        if (!(entity.level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return;
        }
        boolean isSealable = entity instanceof HerobrineEntity
                || entity instanceof TheReaperEntity
                || entity instanceof LickEntity
                || (entity instanceof GlitchEntity glitch && !glitch.isSpectatorVariant());
        if (!isSealable) {
            return;
        }
        InfectionStage stage = InfectionSavedData.get(serverLevel.getServer()).stage();
        if (!stage.atLeast(InfectionStage.HUNTING)) {
            return;
        }
        int count = entity instanceof TheReaperEntity ? 2 : 1;
        event.getDrops().add(new net.minecraft.world.entity.item.ItemEntity(
                serverLevel, entity.getX(), entity.getY() + 0.5D, entity.getZ(),
                new ItemStack(ModItems.SOURCE_FRAGMENT.get(), count)));
    }
}
