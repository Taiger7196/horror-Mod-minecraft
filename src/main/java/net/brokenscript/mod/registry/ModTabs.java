package net.brokenscript.mod.registry;

import net.brokenscript.mod.BrokenScriptMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, BrokenScriptMod.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB =
            CREATIVE_MODE_TABS.register("main", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.brokenscript"))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> ModItems.SOURCE_FRAGMENT.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.SOURCE_FRAGMENT.get());
                        output.accept(ModItems.TERMINAL_BLOCK_ITEM.get());
                        output.accept(ModItems.PURIFICATION_ALTAR_ITEM.get());
                    })
                    .build());

    private ModTabs() {}
}
