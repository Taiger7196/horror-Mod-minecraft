package net.brokenscript.mod.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.RandomSource;

/** Shared pools of unsettling strings + text corruption helpers. */
public final class CorruptedNames {

    public static final String[] PHANTOM_PLAYERS = {
            "Null", "Entity_303", "Entity_505", "herobrine", "green_steve", "you"
    };

    public static final String[] CORRUPTED_FILE_NAMES = {
            "ERR_404.json", "NULL.exe", "boot.sector.bak", "w_h_y.dll",
            "player.dat_old", "chunk_[-13,7].mca", "DO_NOT_OPEN.txt",
            "level.dat.corrupted", "herobrine.class", "0x00000000"
    };

    public static final String[] LOG_LINES = {
            "i can see your cursor",
            "why did you dig there",
            "the script is broken. you broke it.",
            "your render distance will not save you",
            "he stands where you are not looking",
            "8 fragments. then it stops. maybe.",
            "stop reading these files",
            "the chunk you were born in remembers you",
            "your bed is not a respawn point. it is an anchor.",
            "we removed the exit in patch 3.0"
    };

    public static String randomOf(String[] pool, RandomSource random) {
        return pool[random.nextInt(pool.length)];
    }

    /**
     * Corrupts a chat component: keeps the head of the text readable and
     * wraps a random tail segment in obfuscated red glyphs.
     */
    public static Component corrupt(Component original, RandomSource random) {
        String text = original.getString();
        if (text.length() < 6) {
            return Component.literal(text).withStyle(ChatFormatting.OBFUSCATED, ChatFormatting.DARK_RED);
        }
        int cut = 2 + random.nextInt(text.length() - 4);
        int end = Math.min(text.length(), cut + 3 + random.nextInt(8));
        MutableComponent result = Component.literal(text.substring(0, cut));
        result.append(Component.literal(text.substring(cut, end))
                .withStyle(ChatFormatting.OBFUSCATED, ChatFormatting.DARK_RED));
        if (end < text.length()) {
            result.append(Component.literal(text.substring(end)));
        }
        return result;
    }

    private CorruptedNames() {}
}
