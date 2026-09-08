package net.brokenscript.mod.block;

import java.util.UUID;
import net.brokenscript.mod.entity.GlitchEntity;
import net.brokenscript.mod.entity.TheReaperEntity;
import net.brokenscript.mod.infection.InfectionSavedData;
import net.brokenscript.mod.registry.ModBlockEntities;
import net.brokenscript.mod.registry.ModEntities;
import net.brokenscript.mod.registry.ModItems;
import net.brokenscript.mod.util.TerminalProtocol;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * State holder for the Terminal: stored fragments, boot status, quest
 * progress, and the wave-assault referee for the CLI minigame.
 *
 * <p>While a repair session is active, waves of glitch entities spawn around
 * the terminal. If the session player dies or leaves, progress on the current
 * command is kept but the assault stops.</p>
 */
public class TerminalBlockEntity extends BlockEntity {
    private int storedFragments = 0;
    private boolean booted = false;
    private int repairStep = 0;

    /** Transient session state - deliberately not saved. */
    @Nullable
    private UUID sessionPlayer = null;
    private int waveCooldown = 0;

    public TerminalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TERMINAL.get(), pos, state);
    }

    // --- persistence (1.21.5 CompoundTag with Optional-style getters) ---

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("StoredFragments", storedFragments);
        tag.putBoolean("Booted", booted);
        tag.putInt("RepairStep", repairStep);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.storedFragments = tag.getIntOr("StoredFragments", 0);
        this.booted = tag.getBooleanOr("Booted", false);
        this.repairStep = tag.getIntOr("RepairStep", 0);
    }

    // --- fragment intake ---

    /** Pulls Source Code Fragments out of the player's inventory. Returns how many were taken. */
    public int tryInsertFragments(ServerPlayer player) {
        int needed = TerminalProtocol.REQUIRED_FRAGMENTS - storedFragments;
        if (needed <= 0) {
            return 0;
        }
        int taken = 0;
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize() && taken < needed; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(ModItems.SOURCE_FRAGMENT.get())) {
                int take = Math.min(stack.getCount(), needed - taken);
                stack.shrink(take);
                taken += take;
            }
        }
        if (taken > 0) {
            storedFragments += taken;
            setChanged();
        }
        return taken;
    }

    public int getStoredFragments() {
        return storedFragments;
    }

    public boolean isBooted() {
        return booted;
    }

    public int getRepairStep() {
        return repairStep;
    }

    public void boot() {
        this.booted = true;
        setChanged();
    }

    public void beginSession(ServerPlayer player) {
        this.sessionPlayer = player.getUUID();
        this.waveCooldown = 100; // five seconds of grace before the first wave
    }

    /**
     * Called from {@code ModNetworking} when the session player submits a CLI
     * command. Runs on the main server thread (payload handlers default to
     * {@code HandlerThread.MAIN}). Returns the response line to echo back.
     */
    public Component handleCommand(ServerPlayer player, String input) {
        if (!booted) {
            return Component.literal("> NO POWER.").withStyle(ChatFormatting.DARK_GRAY);
        }
        if (repairStep >= TerminalProtocol.COMMANDS.size()) {
            return Component.literal("> SCRIPT ALREADY REPAIRED. GO OUTSIDE.").withStyle(ChatFormatting.GREEN);
        }
        if (!TerminalProtocol.matches(repairStep, input)) {
            punishWrongCommand(player);
            return Component.literal("> SYNTAX REJECTED. THE SCRIPT LAUGHS AT YOU.")
                    .withStyle(ChatFormatting.DARK_RED);
        }

        Component response = Component.literal("> " + TerminalProtocol.RESPONSES.get(repairStep))
                .withStyle(ChatFormatting.GREEN);
        repairStep++;
        setChanged();

        if (repairStep >= TerminalProtocol.COMMANDS.size() && level instanceof ServerLevel serverLevel) {
            // Quest complete: the haunting ends.
            InfectionSavedData.get(serverLevel.getServer()).markRepaired();
            this.sessionPlayer = null;
            serverLevel.getServer().getPlayerList().broadcastSystemMessage(
                    Component.literal("[SYSTEM] script integrity restored. goodbye.")
                            .withStyle(ChatFormatting.GREEN), false);
        }
        return response;
    }

    private void punishWrongCommand(ServerPlayer player) {
        if (level instanceof ServerLevel serverLevel) {
            spawnWave(serverLevel, 2 + serverLevel.random.nextInt(2));
            player.hurtServer(serverLevel, serverLevel.damageSources().magic(), 2.0F);
        }
    }

    // --- wave assault ticker (server thread, registered via TerminalBlock#getTicker) ---

    public static void serverTick(Level level, BlockPos pos, BlockState state, TerminalBlockEntity terminal) {
        if (!(level instanceof ServerLevel serverLevel) || terminal.sessionPlayer == null) {
            return;
        }
        if (terminal.repairStep >= TerminalProtocol.COMMANDS.size()) {
            terminal.sessionPlayer = null;
            return;
        }
        ServerPlayer player = serverLevel.getServer().getPlayerList().getPlayer(terminal.sessionPlayer);
        if (player == null || player.isRemoved()
                || player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) > 32 * 32) {
            terminal.sessionPlayer = null; // player fled or died - the assault relents
            return;
        }
        if (--terminal.waveCooldown <= 0) {
            // Waves scale with quest progress: the closer the fix, the angrier the script.
            terminal.spawnWave(serverLevel, 1 + terminal.repairStep);
            terminal.waveCooldown = Math.max(140, 400 - terminal.repairStep * 60);
        }
    }

    private void spawnWave(ServerLevel level, int count) {
        for (int i = 0; i < count; i++) {
            BlockPos spawn = worldPosition.offset(
                    level.random.nextInt(17) - 8,
                    level.random.nextInt(3),
                    level.random.nextInt(17) - 8);
            // Mostly 303 attackers; a Reaper joins late waves as the closer.
            Mob mob = level.random.nextInt(4) == 0 && repairStep >= 3
                    ? new TheReaperEntity(ModEntities.THE_REAPER.get(), level)
                    : new GlitchEntity(ModEntities.ENTITY_303.get(), level);
            mob.snapTo(spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D,
                    level.random.nextFloat() * 360.0F, 0.0F);
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(spawn), EntitySpawnReason.EVENT, null);
            level.addFreshEntity(mob);
        }
    }
}
