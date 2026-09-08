package net.brokenscript.mod.story;

/**
 * The narrative spine of the mod: USER_0's recovered session pages.
 *
 * <p>Chapters unlock strictly in order and only when their trigger condition
 * is met, so the story can never "leak" content the player has not earned:
 * each page only ever references phenomena the player has already seen at
 * that infection level.</p>
 *
 * <p>All text lives in the lang files (en_us + it_it) as translatable
 * components, keyed {@code brokenscript.story.<id>.p1..pN} and
 * {@code brokenscript.story.<id>.whisper}.</p>
 */
public enum StoryChapter {
    /** First join. A stray note: someone was here before you. */
    BOOT("boot", 0.0D, 2),
    /** ~10%: USER_0 noticed the first anomalies. */
    ANOMALIES("anomalies", 10.0D, 2),
    /** ~25% (stage 2): being watched; names in the player list. */
    WATCHED("watched", 25.0D, 2),
    /** ~40%: the processes have names. */
    PROCESSES("processes", 40.0D, 2),
    /** ~55% (stage 3): USER_0 found something buried. Gameplay hook: the vault. */
    THE_VAULT("the_vault", 55.0D, 2),
    /** ~70%: eight fragments. Gameplay hook: the quest recipe. */
    FRAGMENTS("fragments", 70.0D, 2),
    /** ~85% (stage 4): USER_0's last log, cut off mid-sentence. */
    LAST_LOG("last_log", 85.0D, 2),
    /** Triggered by booting the terminal, not by infection. */
    SESSION_RESTORED("session_restored", Double.MAX_VALUE, 2),
    /** Triggered by completing the repair. The reveal. */
    EPILOGUE("epilogue", Double.MAX_VALUE, 3);

    private final String id;
    private final double infectionThreshold;
    private final int pageCount;

    StoryChapter(String id, double infectionThreshold, int pageCount) {
        this.id = id;
        this.infectionThreshold = infectionThreshold;
        this.pageCount = pageCount;
    }

    public String id() {
        return id;
    }

    /** Infection level at which this chapter unlocks ({@code MAX_VALUE} = event-triggered only). */
    public double infectionThreshold() {
        return infectionThreshold;
    }

    public int pageCount() {
        return pageCount;
    }

    public String pageKey(int page) {
        return "brokenscript.story." + id + ".p" + (page + 1);
    }

    public String whisperKey() {
        return "brokenscript.story." + id + ".whisper";
    }

    /** The file-style title stamped on the recovered page. */
    public String bookTitle() {
        return "PAGE_0" + (ordinal() + 1) + ".log";
    }
}
