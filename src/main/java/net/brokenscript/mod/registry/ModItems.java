package net.brokenscript.mod.registry;

import net.brokenscript.mod.BrokenScriptMod;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BrokenScriptMod.MODID);

    /**
     * Quest item: 8 of these are needed to boot the Terminal. Dropped by
     * stage 3/4 entities (see the entity loot tables under
     * {@code data/brokenscript/loot_table/entities/}) and found in the
     * Terminal Vault chest loot.
     */
    public static final DeferredItem<Item> SOURCE_FRAGMENT = ITEMS.registerSimpleItem(
            "source_fragment",
            new Item.Properties().rarity(Rarity.EPIC).stacksTo(16));

    public static final DeferredItem<BlockItem> TERMINAL_BLOCK_ITEM =
            ITEMS.registerSimpleBlockItem("terminal_block", ModBlocks.TERMINAL_BLOCK);

    public static final DeferredItem<BlockItem> PURIFICATION_ALTAR_ITEM =
            ITEMS.registerSimpleBlockItem("purification_altar", ModBlocks.PURIFICATION_ALTAR);

    private ModItems() {}
}
