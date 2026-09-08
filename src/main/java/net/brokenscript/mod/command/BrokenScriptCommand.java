package net.brokenscript.mod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.brokenscript.mod.horror.HorrorDirector;
import net.brokenscript.mod.infection.InfectionSavedData;
import net.brokenscript.mod.story.StoryChapter;
import net.brokenscript.mod.story.StoryManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/**
 * {@code /brokenscript} - playtest & ops toolkit (permission level 2).
 *
 * <pre>
 *   /brokenscript status                  - infection, stage, story progress
 *   /brokenscript infection set &lt;0..100&gt;  - jump to a level
 *   /brokenscript infection add &lt;delta&gt;   - nudge the level
 *   /brokenscript story next              - force-deliver the next chapter
 *   /brokenscript story give              - re-give all unlocked pages to sender
 *   /brokenscript event                   - roll one scripted event now (sender)
 *   /brokenscript reset                   - wipe infection + repair flag (story kept)
 * </pre>
 */
public final class BrokenScriptCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("brokenscript")
                .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(literal("status").executes(ctx -> status(ctx.getSource())))
                .then(literal("infection")
                        .then(literal("set").then(argument("level", DoubleArgumentType.doubleArg(0.0D, 100.0D))
                                .executes(ctx -> setInfection(ctx.getSource(),
                                        DoubleArgumentType.getDouble(ctx, "level")))))
                        .then(literal("add").then(argument("delta", DoubleArgumentType.doubleArg(-100.0D, 100.0D))
                                .executes(ctx -> addInfection(ctx.getSource(),
                                        DoubleArgumentType.getDouble(ctx, "delta"))))))
                .then(literal("story")
                        .then(literal("next").executes(ctx -> storyNext(ctx.getSource())))
                        .then(literal("give").executes(ctx -> storyGive(ctx.getSource()))))
                .then(literal("event").executes(ctx -> rollEvent(ctx.getSource())))
                .then(literal("reset").executes(ctx -> reset(ctx.getSource()))));
    }

    private static int status(CommandSourceStack source) {
        InfectionSavedData data = InfectionSavedData.get(source.getServer());
        int chapter = data.storyChapter();
        int total = StoryChapter.values().length;
        source.sendSuccess(() -> Component.literal(String.format(
                        "[brokenscript] infection %.2f%% | stage %d | story %d/%d | repaired: %s",
                        data.level(), data.stage().stageNumber(), chapter, total, data.isScriptRepaired()))
                .withStyle(ChatFormatting.GRAY), false);
        return 1;
    }

    private static int setInfection(CommandSourceStack source, double level) {
        InfectionSavedData data = InfectionSavedData.get(source.getServer());
        data.set(level);
        source.sendSuccess(() -> Component.literal(
                String.format("[brokenscript] infection -> %.2f%% (stage %d)",
                        data.level(), data.stage().stageNumber())), true);
        return 1;
    }

    private static int addInfection(CommandSourceStack source, double delta) {
        InfectionSavedData data = InfectionSavedData.get(source.getServer());
        data.set(data.level() + delta);
        source.sendSuccess(() -> Component.literal(
                String.format("[brokenscript] infection -> %.2f%% (stage %d)",
                        data.level(), data.stage().stageNumber())), true);
        return 1;
    }

    private static int storyNext(CommandSourceStack source) {
        boolean delivered = StoryManager.forceNext(source.getServer());
        source.sendSuccess(() -> Component.literal(delivered
                ? "[brokenscript] next chapter delivered."
                : "[brokenscript] no chapters left. the story is over."), true);
        return delivered ? 1 : 0;
    }

    private static int storyGive(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("[brokenscript] players only."));
            return 0;
        }
        int unlocked = InfectionSavedData.get(source.getServer()).storyChapter();
        for (int i = 0; i < unlocked; i++) {
            var book = StoryManager.createPageBook(StoryChapter.values()[i]);
            if (!player.getInventory().add(book)) {
                player.drop(book, false);
            }
        }
        int count = unlocked;
        source.sendSuccess(() -> Component.literal(
                "[brokenscript] re-gave " + count + " recovered page(s)."), false);
        return count;
    }

    private static int rollEvent(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("[brokenscript] players only."));
            return 0;
        }
        HorrorDirector.forceEventFor(player);
        source.sendSuccess(() -> Component.literal("[brokenscript] event rolled. look behind you."), false);
        return 1;
    }

    private static int reset(CommandSourceStack source) {
        InfectionSavedData data = InfectionSavedData.get(source.getServer());
        data.unrepair();
        data.set(0.0D);
        source.sendSuccess(() -> Component.literal(
                "[brokenscript] infection reset to 0, repair flag cleared. story progress kept."), true);
        return 1;
    }

    private BrokenScriptCommand() {}
}
