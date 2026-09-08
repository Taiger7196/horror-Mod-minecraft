package net.brokenscript.mod.horror;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import net.brokenscript.mod.BrokenScriptMod;
import net.brokenscript.mod.Config;
import net.brokenscript.mod.infection.InfectionStage;
import net.brokenscript.mod.util.CorruptedNames;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.neoforged.fml.loading.FMLPaths;

/**
 * Real-world file horror: writes actual {@code .txt} files into
 * {@code <gamedir>/LOGS_UNCANNY/}. Content escalates with the infection stage,
 * mixing cryptic one-liners, the player's real coordinates, and ASCII art.
 *
 * <p>All disk I/O runs on {@link Util#ioPool()} - never on the server thread.
 * Files are only ever created inside the game directory, and the whole
 * feature honors {@link Config#ALLOW_UNCANNY_LOGS}.</p>
 */
public final class LogsUncannyManager {
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss");
    private static final String[] FILE_PREFIXES = {
            "session", "crash_report", "heartbeat", "watching", "backup_of_you"
    };
    private static final int MAX_FILES = 64; // don't actually flood anyone's disk

    private static final String ASCII_EYES =
            "  _______________________\n"
            + " |                       |\n"
            + " |   .--.         .--.  |\n"
            + " |  | () |       | () | |\n"
            + " |   '--'         '--'  |\n"
            + " |                       |\n"
            + " |_______________________|\n";

    /** Drops one new cryptic file. Called by the director on stage 2+. */
    public static void writeCrypticFile(String playerName, BlockPos playerPos,
                                        InfectionStage stage, RandomSource random) {
        if (!Config.ALLOW_UNCANNY_LOGS.getAsBoolean()) {
            return;
        }
        // Snapshot everything we need on the calling (server) thread...
        String prefix = FILE_PREFIXES[random.nextInt(FILE_PREFIXES.length)];
        String fileName = prefix + "_" + LocalDateTime.now().format(STAMP) + ".txt";
        String body = composeBody(playerName, playerPos, stage, random);

        // ...then push the blocking I/O onto the IO pool.
        Util.ioPool().execute(() -> {
            try {
                Path dir = FMLPaths.GAMEDIR.get().resolve("LOGS_UNCANNY");
                Files.createDirectories(dir);
                try (var files = Files.list(dir)) {
                    if (files.count() >= MAX_FILES) {
                        return; // enough haunting for one hard drive
                    }
                }
                Files.writeString(dir.resolve(fileName), body, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            } catch (IOException e) {
                BrokenScriptMod.LOGGER.debug("LOGS_UNCANNY write failed (ignored): {}", e.toString());
            }
        });
    }

    /**
     * Story echo: whenever a chapter of USER_0's log is delivered in-game, a
     * matching artifact appears on the real disk - the same page, "recovered".
     * Only ever contains already-unlocked chapters, so nothing leaks early.
     */
    public static void writeStoryEcho(net.brokenscript.mod.story.StoryChapter chapter) {
        if (!Config.ALLOW_UNCANNY_LOGS.getAsBoolean()) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("=== recovered session page: ").append(chapter.bookTitle()).append(" ===\n");
        sb.append("=== author: USER_0 | do not redistribute ===\n\n");
        for (int i = 0; i < chapter.pageCount(); i++) {
            sb.append("[").append(chapter.pageKey(i)).append("]\n");
        }
        sb.append("\n(text withheld from plain files. read it where you found it.)\n");
        String body = sb.toString();
        Util.ioPool().execute(() -> {
            try {
                Path dir = FMLPaths.GAMEDIR.get().resolve("LOGS_UNCANNY");
                Files.createDirectories(dir);
                Files.writeString(dir.resolve(chapter.bookTitle() + ".txt"), body, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            } catch (IOException e) {
                BrokenScriptMod.LOGGER.debug("LOGS_UNCANNY story echo failed (ignored): {}", e.toString());
            }
        });
    }

    /** Appends a line to the rolling THEY_ARE_HERE.txt journal on stage escalation. */
    public static void appendEscalation(InfectionStage newStage, double level) {
        if (!Config.ALLOW_UNCANNY_LOGS.getAsBoolean()) {
            return;
        }
        String line = String.format(Locale.ROOT, "[%s] stage %d reached. integrity %.1f%%. %s%n",
                LocalDateTime.now().format(STAMP), newStage.stageNumber(), 100.0D - level,
                switch (newStage) {
                    case DORMANT -> "it sleeps.";
                    case WATCHING -> "it noticed.";
                    case HUNTING -> "it is coming.";
                    case COLLAPSE -> "RUN.";
                });
        Util.ioPool().execute(() -> {
            try {
                Path dir = FMLPaths.GAMEDIR.get().resolve("LOGS_UNCANNY");
                Files.createDirectories(dir);
                Files.writeString(dir.resolve("THEY_ARE_HERE.txt"), line, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
                BrokenScriptMod.LOGGER.debug("LOGS_UNCANNY append failed (ignored): {}", e.toString());
            }
        });
    }

    private static String composeBody(String playerName, BlockPos pos,
                                      InfectionStage stage, RandomSource random) {
        StringBuilder sb = new StringBuilder();
        sb.append("// recovered fragment - do not distribute\n\n");

        int lines = 1 + stage.stageNumber();
        for (int i = 0; i < lines; i++) {
            sb.append(CorruptedNames.randomOf(CorruptedNames.LOG_LINES, random)).append('\n');
        }

        if (stage.atLeast(InfectionStage.WATCHING)) {
            // Real coordinates, slightly wrong - close enough to be checked.
            sb.append(String.format(Locale.ROOT, "%nlast known position of \"%s\": %d, %d, %d%n",
                    playerName,
                    pos.getX() + random.nextInt(5) - 2,
                    pos.getY(),
                    pos.getZ() + random.nextInt(5) - 2));
        }
        if (stage.atLeast(InfectionStage.HUNTING)) {
            sb.append('\n').append(ASCII_EYES);
        }
        if (stage == InfectionStage.COLLAPSE) {
            sb.append("\nfile integrity 0x")
              .append(Integer.toHexString(random.nextInt()).toUpperCase(Locale.ROOT))
              .append(" -- CANNOT REPAIR\nfind the terminal. eight fragments. hurry.\n");
        }
        return sb.toString();
    }

    private LogsUncannyManager() {}
}
