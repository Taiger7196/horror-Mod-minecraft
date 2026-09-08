package net.brokenscript.mod.registry;

import java.util.Set;
import net.brokenscript.mod.BrokenScriptMod;
import net.brokenscript.mod.block.TerminalBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, BrokenScriptMod.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TerminalBlockEntity>> TERMINAL =
            BLOCK_ENTITY_TYPES.register("terminal_block",
                    () -> new BlockEntityType<>(TerminalBlockEntity::new, Set.of(ModBlocks.TERMINAL_BLOCK.get())));

    private ModBlockEntities() {}
}
