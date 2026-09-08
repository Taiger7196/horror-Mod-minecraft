package net.brokenscript.mod.registry;

import com.mojang.serialization.MapCodec;
import net.brokenscript.mod.BrokenScriptMod;
import net.brokenscript.mod.worldgen.TerminalVaultPiece;
import net.brokenscript.mod.worldgen.TerminalVaultStructure;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Structure type + piece registrations. The actual placement (biomes, spacing,
 * step) lives in datapack JSON:
 * {@code data/brokenscript/worldgen/structure/terminal_vault.json} and
 * {@code data/brokenscript/worldgen/structure_set/terminal_vault.json}.
 */
public final class ModStructures {
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, BrokenScriptMod.MODID);

    public static final DeferredRegister<StructurePieceType> STRUCTURE_PIECES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, BrokenScriptMod.MODID);

    public static final DeferredHolder<StructureType<?>, StructureType<TerminalVaultStructure>> TERMINAL_VAULT =
            STRUCTURE_TYPES.register("terminal_vault", () -> explicitStructureTypeTyping(TerminalVaultStructure.CODEC));

    public static final DeferredHolder<StructurePieceType, StructurePieceType> TERMINAL_VAULT_PIECE =
            STRUCTURE_PIECES.register("terminal_vault_piece",
                    () -> (StructurePieceType.ContextlessType) TerminalVaultPiece::new);

    private static <S extends Structure> StructureType<S> explicitStructureTypeTyping(MapCodec<S> codec) {
        return () -> codec;
    }

    private ModStructures() {}
}
