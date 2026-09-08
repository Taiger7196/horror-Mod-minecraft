package net.brokenscript.mod.worldgen;

import net.brokenscript.mod.BrokenScriptMod;
import net.brokenscript.mod.registry.ModBlocks;
import net.brokenscript.mod.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Hand-built 11x6x11 "server room": obsidian shell, crying-obsidian pillars,
 * the Terminal Block on a dais, and two chests wired to the
 * {@code brokenscript:chests/terminal_vault} loot table (Source Fragments).
 */
public class TerminalVaultPiece extends StructurePiece {
    private static final int SIZE_XZ = 11;
    private static final int SIZE_Y = 6;

    public static final ResourceKey<LootTable> VAULT_LOOT = ResourceKey.create(
            net.minecraft.core.registries.Registries.LOOT_TABLE,
            BrokenScriptMod.id("chests/terminal_vault"));

    public TerminalVaultPiece(BlockPos origin) {
        super(ModStructures.TERMINAL_VAULT_PIECE.get(), 0, BoundingBox.fromCorners(
                origin, origin.offset(SIZE_XZ - 1, SIZE_Y - 1, SIZE_XZ - 1)));
        setOrientation(Direction.NORTH);
    }

    /** Deserialization constructor (ContextlessType). */
    public TerminalVaultPiece(CompoundTag tag) {
        super(ModStructures.TERMINAL_VAULT_PIECE.get(), tag);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        // Bounding box + orientation are saved by the base class; no extra state.
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager,
                            ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunkPos, BlockPos center) {
        // Shell: obsidian walls/floor/ceiling with occasional crying obsidian.
        for (int x = 0; x < SIZE_XZ; x++) {
            for (int y = 0; y < SIZE_Y; y++) {
                for (int z = 0; z < SIZE_XZ; z++) {
                    boolean isShell = x == 0 || z == 0 || y == 0
                            || x == SIZE_XZ - 1 || z == SIZE_XZ - 1 || y == SIZE_Y - 1;
                    if (isShell) {
                        placeBlock(level, random.nextFloat() < 0.12F
                                        ? Blocks.CRYING_OBSIDIAN.defaultBlockState()
                                        : Blocks.OBSIDIAN.defaultBlockState(),
                                x, y, z, box);
                    } else {
                        placeBlock(level, Blocks.CAVE_AIR.defaultBlockState(), x, y, z, box);
                    }
                }
            }
        }

        // Corner pillars + sea lanterns: dead server-rack lighting.
        for (int[] corner : new int[][]{{2, 2}, {2, 8}, {8, 2}, {8, 8}}) {
            for (int y = 1; y < SIZE_Y - 1; y++) {
                placeBlock(level, Blocks.CRYING_OBSIDIAN.defaultBlockState(), corner[0], y, corner[1], box);
            }
            placeBlock(level, Blocks.SEA_LANTERN.defaultBlockState(), corner[0], SIZE_Y - 2, corner[1], box);
        }

        // Dais + the Terminal Block at the heart of the room.
        placeBlock(level, Blocks.POLISHED_BLACKSTONE.defaultBlockState(), 5, 1, 5, box);
        placeBlock(level, ModBlocks.TERMINAL_BLOCK.get().defaultBlockState(), 5, 2, 5, box);

        // Two loot chests against opposite walls.
        placeLootChest(level, box, 1, 1, 5);
        placeLootChest(level, box, 9, 1, 5);
    }

    private void placeLootChest(WorldGenLevel level, BoundingBox box, int x, int y, int z) {
        placeBlock(level, Blocks.CHEST.defaultBlockState(), x, y, z, box);
        BlockPos worldPos = new BlockPos(getWorldX(x, z), getWorldY(y), getWorldZ(x, z));
        if (box.isInside(worldPos)) {
            RandomizableContainer.setBlockEntityLootTable(level, level.getRandom(), worldPos, VAULT_LOOT);
        }
    }
}
