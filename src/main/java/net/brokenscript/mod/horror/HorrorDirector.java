package net.brokenscript.mod.horror;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.brokenscript.mod.Config;
import net.brokenscript.mod.block.PurificationAltarBlock;
import net.brokenscript.mod.infection.InfectionSavedData;
import net.brokenscript.mod.infection.InfectionStage;
import net.brokenscript.mod.network.clientbound.FakeSystemMessagePayload;
import net.brokenscript.mod.network.clientbound.GlitchEffectPayload;
import net.brokenscript.mod.network.clientbound.InfectionSyncPayload;
import net.brokenscript.mod.registry.ModEntities;
import net.brokenscript.mod.registry.ModSounds;
import net.brokenscript.mod.util.CorruptedNames;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The server-side "game master". Ticked once per second from
 * {@code CommonEvents#onServerTick} (main server thread - all world mutation
 * here is therefore thread-safe by construction).
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>Advance the world {@code InfectionLevel} (faster deep underground,
 *       faster near cursed netherrack/obsidian scars, slower near altars).</li>
 *   <li>Sync the level to clients and announce stage escalations.</li>
 *   <li>Roll per-player scripted events appropriate to the current stage.</li>
 * </ul>
 */
public final class HorrorDirector {
    private static final int TICKS_PER_STEP = 20; // run once a second

    private static int stepCounter = 0;
    private static InfectionStage lastBroadcastStage = null;
    private static final Map<UUID, Long> nextEventAt = new HashMap<>();

    public static void tick(MinecraftServer server) {
        if (!Config.ENABLED.getAsBoolean() || ++stepCounter < TICKS_PER_STEP) {
            return;
        }
        stepCounter = 0;

        InfectionSavedData data = InfectionSavedData.get(server);
        if (data.isScriptRepaired()) {
            return; // the world has been fixed; silence, forever
        }

        advanceInfection(server, data);
        broadcastStage(server, data);

        InfectionStage stage = data.stage();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!player.isSpectator() && !player.isCreative()) {
                maybeRunEventFor(player, stage, data);
            }
        }
    }

    // --- infection growth ---

    private static void advanceInfection(MinecraftServer server, InfectionSavedData data) {
        var players = server.getPlayerList().getPlayers();
        if (players.isEmpty()) {
            return;
        }
        // Baseline tuned for a 20+ hour arc: ~0.0014%/s -> full at ~20h of play.
        double delta = 0.0014D;
        for (ServerPlayer player : players) {
            ServerLevel level = (ServerLevel) player.level();
            BlockPos pos = player.blockPosition();
            if (pos.getY() < 0) {
                delta += 0.0028D; // the deep dark remembers
            }
            if (isNearCursedBlocks(level, pos)) {
                delta += 0.005D; // standing in a scar left by Lick
            }
            if (PurificationAltarBlock.isNearAltar(level, pos, PurificationAltarBlock.HOLY_RADIUS)) {
                delta -= 0.02D; // holy light actively cleanses
            }
        }
        data.add(delta * Config.INFECTION_RATE.getAsDouble());

        PacketDistributor.sendToAllPlayers(new InfectionSyncPayload(data.level()));
    }

    private static boolean isNearCursedBlocks(ServerLevel level, BlockPos center) {
        int hits = 0;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-4, -2, -4), center.offset(4, 2, 4))) {
            var state = level.getBlockState(pos);
            if (state.is(Blocks.NETHERRACK) || state.is(Blocks.OBSIDIAN)) {
                if (++hits >= 6 && level.dimension() == Level.OVERWORLD) {
                    return true; // netherrack has no business being here
                }
            }
        }
        return false;
    }

    private static void broadcastStage(MinecraftServer server, InfectionSavedData data) {
        InfectionStage stage = data.stage();
        if (stage == lastBroadcastStage) {
            return;
        }
        boolean escalated = lastBroadcastStage == null || stage.atLeast(lastBroadcastStage);
        lastBroadcastStage = stage;
        if (!escalated || stage == InfectionStage.DORMANT) {
            return;
        }
        LogsUncannyManager.appendEscalation(stage, data.level());
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.level().playSound(null, player.blockPosition(), ModSounds.HEARTBEAT.get(),
                    SoundSource.AMBIENT, 1.0F, 0.6F);
        }
    }

    // --- per-player scripted events ---

    private static void maybeRunEventFor(ServerPlayer player, InfectionStage stage, InfectionSavedData data) {
        long gameTime = player.level().getGameTime();
        long readyAt = nextEventAt.getOrDefault(player.getUUID(), 0L);
        if (gameTime < readyAt) {
            return;
        }
        RandomSource random = player.level().random;
        // Roll roughly once per 30s of eligibility; stage raises the odds.
        if (random.nextInt(Math.max(2, 12 - stage.stageNumber() * 2)) != 0) {
            return;
        }
        nextEventAt.put(player.getUUID(),
                gameTime + Config.MIN_EVENT_INTERVAL.getAsInt() * 20L);

        switch (stage) {
            case DORMANT -> runStage1(player, random);
            case WATCHING -> runStage2(player, random, data);
            case HUNTING -> runStage3(player, random, data);
            case COLLAPSE -> runStage4(player, random, data);
        }
    }

    /** Stage 1: nothing you could screenshot. Sounds behind you, at most. */
    private static void runStage1(ServerPlayer player, RandomSource random) {
        ServerLevel level = (ServerLevel) player.level();
        switch (random.nextInt(3)) {
            case 0 -> {
                // A footstep-ish stone sound just behind the player.
                BlockPos behind = BlockPos.containing(
                        player.position().subtract(player.getViewVector(1.0F).scale(3.0D)));
                level.playSound(null, behind, net.minecraft.sounds.SoundEvents.STONE_STEP,
                        SoundSource.AMBIENT, 0.8F, 0.9F);
            }
            case 1 -> level.playSound(null, player.blockPosition(), ModSounds.WHISPER.get(),
                    SoundSource.AMBIENT, 0.3F, 0.8F + random.nextFloat() * 0.4F);
            default -> {
                // Fake cave ambience on the surface, in daylight. Wrong on purpose.
                level.playSound(null, player.blockPosition().above(8),
                        net.minecraft.sounds.SoundEvents.AMBIENT_CAVE.value(),
                        SoundSource.AMBIENT, 0.7F, 1.0F);
            }
        }
    }

    /** Stage 2: spectators at the edge of vision, phantom players, first files. */
    private static void runStage2(ServerPlayer player, RandomSource random, InfectionSavedData data) {
        ServerLevel level = (ServerLevel) player.level();
        switch (random.nextInt(4)) {
            case 0 -> spawnDistantSpectator(level, player, random,
                    random.nextBoolean() ? ModEntities.NULL.get() : ModEntities.ENTITY_505.get());
            case 1 -> PacketDistributor.sendToPlayer(player, new FakeSystemMessagePayload(
                    CorruptedNames.randomOf(CorruptedNames.PHANTOM_PLAYERS, random), random.nextBoolean()));
            case 2 -> LogsUncannyManager.writeCrypticFile(
                    player.getGameProfile().getName(), player.blockPosition(), data.stage(), random);
            default -> stripLeavesNear(level, player.blockPosition(), random);
        }
    }

    /** Stage 3: they stop hiding. */
    private static void runStage3(ServerPlayer player, RandomSource random, InfectionSavedData data) {
        ServerLevel level = (ServerLevel) player.level();
        switch (random.nextInt(5)) {
            case 0 -> spawnNear(level, player, random, ModEntities.HEROBRINE.get(), 20, 40);
            case 1 -> spawnNear(level, player, random, ModEntities.ENTITY_303.get(), 16, 30);
            case 2 -> spawnNear(level, player, random, ModEntities.LICK.get(), 16, 30);
            case 3 -> PacketDistributor.sendToPlayer(player,
                    new GlitchEffectPayload(GlitchEffectPayload.Kind.UI_SCRAMBLE, 100 + random.nextInt(100)));
            default -> LogsUncannyManager.writeCrypticFile(
                    player.getGameProfile().getName(), player.blockPosition(), data.stage(), random);
        }
    }

    /** Stage 4: the world itself gives up. */
    private static void runStage4(ServerPlayer player, RandomSource random, InfectionSavedData data) {
        ServerLevel level = (ServerLevel) player.level();
        switch (random.nextInt(5)) {
            case 0 -> spawnNear(level, player, random, ModEntities.THE_REAPER.get(), 24, 48);
            case 1 -> {
                if (Config.ALLOW_INPUT_GLITCHES.getAsBoolean()) {
                    PacketDistributor.sendToPlayer(player, new GlitchEffectPayload(
                            GlitchEffectPayload.Kind.INVERT_CONTROLS, 100 + random.nextInt(100)));
                }
            }
            case 2 -> {
                if (Config.ALLOW_FAKE_CRASH.getAsBoolean()) {
                    PacketDistributor.sendToPlayer(player, new GlitchEffectPayload(
                            GlitchEffectPayload.Kind.FAKE_CRASH, 120));
                }
            }
            case 3 -> degradeWorldAround(level, player.blockPosition(), random);
            default -> LogsUncannyManager.writeCrypticFile(
                    player.getGameProfile().getName(), player.blockPosition(), data.stage(), random);
        }
    }

    // --- event helpers (all main-thread) ---

    /** Spawns a spectator entity far away, at high render distance, facing the player. */
    private static void spawnDistantSpectator(ServerLevel level, ServerPlayer player,
                                              RandomSource random, EntityType<? extends PathfinderMob> type) {
        spawnNear(level, player, random, type, 48, 80);
    }

    private static void spawnNear(ServerLevel level, ServerPlayer player, RandomSource random,
                                  EntityType<? extends PathfinderMob> type, int minDist, int maxDist) {
        for (int attempts = 0; attempts < 24; attempts++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            int dist = minDist + random.nextInt(Math.max(1, maxDist - minDist));
            BlockPos base = player.blockPosition().offset(
                    (int) (Math.cos(angle) * dist), 0, (int) (Math.sin(angle) * dist));
            BlockPos surface = level.getHeightmapPos(
                    net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, base);
            if (!level.isLoaded(surface)) {
                continue;
            }
            Mob mob = (Mob) type.create(level, EntitySpawnReason.EVENT);
            if (mob == null) {
                return;
            }
            mob.snapTo(surface.getX() + 0.5D, surface.getY(), surface.getZ() + 0.5D, 0.0F, 0.0F);
            mob.getLookControl().setLookAt(player);
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(surface), EntitySpawnReason.EVENT, null);
            level.addFreshEntity(mob);
            return;
        }
    }

    /** "Leaves Stripped Trees": silently deletes leaf clusters near the player. */
    private static void stripLeavesNear(ServerLevel level, BlockPos center, RandomSource random) {
        BlockPos target = center.offset(random.nextInt(33) - 16, random.nextInt(9), random.nextInt(33) - 16);
        int removed = 0;
        for (BlockPos pos : BlockPos.betweenClosed(target.offset(-3, -3, -3), target.offset(3, 3, 3))) {
            if (level.getBlockState(pos).is(net.minecraft.tags.BlockTags.LEAVES)) {
                level.removeBlock(pos, false);
                if (++removed >= 40) {
                    return;
                }
            }
        }
    }

    /** Stage 4 world degradation: scattered decay in a small radius. */
    private static void degradeWorldAround(ServerLevel level, BlockPos center, RandomSource random) {
        if (!Config.ALLOW_WORLD_DEGRADATION.getAsBoolean()) {
            return;
        }
        for (int i = 0; i < 24; i++) {
            BlockPos pos = center.offset(random.nextInt(17) - 8, random.nextInt(9) - 4, random.nextInt(17) - 8);
            var state = level.getBlockState(pos);
            if (state.isAir() || state.getDestroySpeed(level, pos) < 0.0F) {
                continue; // never touch bedrock/unbreakables
            }
            if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT)) {
                level.setBlockAndUpdate(pos, Blocks.COARSE_DIRT.defaultBlockState());
            } else if (state.is(Blocks.STONE)) {
                level.setBlockAndUpdate(pos, Blocks.COBBLESTONE.defaultBlockState());
            } else if (state.is(net.minecraft.tags.BlockTags.LEAVES) && random.nextBoolean()) {
                level.removeBlock(pos, false);
            } else if (random.nextFloat() < 0.05F && state.is(net.minecraft.tags.BlockTags.LOGS)) {
                level.setBlockAndUpdate(pos, Blocks.NETHERRACK.defaultBlockState());
            }
        }
    }

    public static void onPlayerLeave(UUID id) {
        nextEventAt.remove(id);
    }

    /** Reset static state between server runs (singleplayer relaunch safety). */
    public static void reset() {
        stepCounter = 0;
        lastBroadcastStage = null;
        nextEventAt.clear();
    }

    private HorrorDirector() {}
}
