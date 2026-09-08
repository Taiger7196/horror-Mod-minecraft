package net.brokenscript.mod.registry;

import net.brokenscript.mod.BrokenScriptMod;
import net.brokenscript.mod.block.PurificationAltarBlock;
import net.brokenscript.mod.block.TerminalBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Block registry. {@code DeferredRegister.Blocks#registerBlock} automatically
 * assigns the registry id to the {@link BlockBehaviour.Properties} (mandatory
 * since 1.21.2's {@code setId} requirement).
 */
public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(BrokenScriptMod.MODID);

    /** End-game CLI block. Houses the "Fixing the Script" quest. */
    public static final DeferredBlock<TerminalBlock> TERMINAL_BLOCK = BLOCKS.registerBlock(
            "terminal_block",
            TerminalBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(50.0F, 1200.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.NETHERITE_BLOCK)
                    .lightLevel(state -> 7));

    /** Holy light source - the only thing The Reaper fears. */
    public static final DeferredBlock<PurificationAltarBlock> PURIFICATION_ALTAR = BLOCKS.registerBlock(
            "purification_altar",
            PurificationAltarBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.GOLD)
                    .strength(3.5F)
                    .sound(SoundType.AMETHYST)
                    .lightLevel(state -> 15));

    private ModBlocks() {}
}
