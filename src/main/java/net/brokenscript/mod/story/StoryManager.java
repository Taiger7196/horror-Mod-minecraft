package net.brokenscript.mod.story;

import java.util.ArrayList;
import java.util.List;
import net.brokenscript.mod.horror.LogsUncannyManager;
import net.brokenscript.mod.infection.InfectionSavedData;
import net.brokenscript.mod.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;

/**
 * Delivers USER_0's story, one chapter at a time.
 *
 * <p>Ticked from {@code HorrorDirector} on the server thread. Progress is
 * persisted in {@link InfectionSavedData#storyChapter()} so a world never
 * repeats or skips chapters, and chapters only unlock in order - even if the
 * infection is set higher by command, pages arrive one per delivery window,
 * preserving the reading order.</p>
 *
 * <p>Delivery ritual: the whisper sound plays, a dark line appears in chat,
 * and a written book ("PAGE_XX.log", author USER_0) slips into the player's
 * inventory. A copy of the moment is also echoed to LOGS_UNCANNY.</p>
 */
public final class StoryManager {
    /** Minimum seconds between two chapter deliveries (no page-dump on high infection). */
    private static final long DELIVERY_COOLDOWN_TICKS = 20L * 90;

    private static long lastDeliveryGameTime = Long.MIN_VALUE;

    /** Called once per director step (1s). Unlocks the next threshold chapter if due. */
    public static void tick(MinecraftServer server, InfectionSavedData data) {
        if (server.getPlayerList().getPlayers().isEmpty()) {
            return; // never deliver a page into an empty world
        }
        int next = data.storyChapter();
        StoryChapter[] chapters = StoryChapter.values();
        if (next >= chapters.length) {
            return;
        }
        StoryChapter chapter = chapters[next];
        if (chapter.infectionThreshold() == Double.MAX_VALUE) {
            return; // event-triggered chapters are delivered explicitly
        }
        if (data.level() < chapter.infectionThreshold()) {
            return;
        }
        long now = server.overworld().getGameTime();
        if (lastDeliveryGameTime != Long.MIN_VALUE && now - lastDeliveryGameTime < DELIVERY_COOLDOWN_TICKS) {
            return; // let the previous page breathe
        }
        lastDeliveryGameTime = now;
        deliver(server, data, chapter);
    }

    /** Terminal booted with 8 fragments: USER_0's session is restored. */
    public static void onTerminalBooted(MinecraftServer server) {
        deliverEventChapter(server, StoryChapter.SESSION_RESTORED);
    }

    /** Script repaired: the epilogue - and the reveal. */
    public static void onScriptRepaired(MinecraftServer server) {
        deliverEventChapter(server, StoryChapter.EPILOGUE);
    }

    /** Playtest helper: force-deliver the next pending chapter, ignoring thresholds. */
    public static boolean forceNext(MinecraftServer server) {
        InfectionSavedData data = InfectionSavedData.get(server);
        int next = data.storyChapter();
        StoryChapter[] chapters = StoryChapter.values();
        if (next >= chapters.length) {
            return false;
        }
        deliver(server, data, chapters[next]);
        return true;
    }

    private static void deliverEventChapter(MinecraftServer server, StoryChapter chapter) {
        InfectionSavedData data = InfectionSavedData.get(server);
        // Deliver any skipped threshold chapters silently as logs first, so the
        // numbered pages always stay in order, then the event chapter as a book.
        while (data.storyChapter() < chapter.ordinal()) {
            StoryChapter skipped = StoryChapter.values()[data.storyChapter()];
            LogsUncannyManager.writeStoryEcho(skipped);
            data.setStoryChapter(data.storyChapter() + 1);
        }
        if (data.storyChapter() == chapter.ordinal()) {
            deliver(server, data, chapter);
        }
    }

    private static void deliver(MinecraftServer server, InfectionSavedData data, StoryChapter chapter) {
        data.setStoryChapter(chapter.ordinal() + 1);
        LogsUncannyManager.writeStoryEcho(chapter);

        Component whisper = Component.translatable(chapter.whisperKey())
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC);

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(whisper);
            player.level().playSound(null, player.blockPosition(), ModSounds.WHISPER.get(),
                    SoundSource.AMBIENT, 0.5F, 0.65F);
            ItemStack book = createPageBook(chapter);
            if (!player.getInventory().add(book)) {
                player.drop(book, false); // inventory full: the page falls at your feet
            }
        }
    }

    /** Builds the "recovered page" written book for a chapter. */
    public static ItemStack createPageBook(StoryChapter chapter) {
        List<Filterable<Component>> pages = new ArrayList<>(chapter.pageCount());
        for (int i = 0; i < chapter.pageCount(); i++) {
            pages.add(Filterable.passThrough(Component.translatable(chapter.pageKey(i))));
        }
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        book.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(
                Filterable.passThrough(chapter.bookTitle()),
                "USER_0",
                0,               // generation: original
                pages,
                true));          // resolved
        return book;
    }

    /** Reset between server runs. */
    public static void reset() {
        lastDeliveryGameTime = Long.MIN_VALUE;
    }

    private StoryManager() {}
}
