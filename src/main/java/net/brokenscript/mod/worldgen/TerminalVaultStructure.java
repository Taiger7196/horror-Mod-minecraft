package net.brokenscript.mod.worldgen;

import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.brokenscript.mod.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

/**
 * The rare underground "Terminal Vault" - a small obsidian server room housing
 * the Terminal Block for the end-game quest.
 *
 * <p>Registered as a structure type in {@link ModStructures}; placement rules
 * (biomes, salt, spacing 48/24 chunks) live in datapack JSON under
 * {@code data/brokenscript/worldgen/structure/} and
 * {@code .../worldgen/structure_set/}.</p>
 */
public class TerminalVaultStructure extends Structure {
    public static final MapCodec<TerminalVaultStructure> CODEC = simpleCodec(TerminalVaultStructure::new);

    public TerminalVaultStructure(Structure.StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunkPos = context.chunkPos();
        // Bury the vault deep: y in [-40, 0), varied per-position.
        int y = -40 + context.random().nextInt(40);
        BlockPos origin = new BlockPos(chunkPos.getMinBlockX() + 4, y, chunkPos.getMinBlockZ() + 4);
        return Optional.of(new GenerationStub(origin, builder ->
                builder.addPiece(new TerminalVaultPiece(origin))));
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.TERMINAL_VAULT.get();
    }
}
