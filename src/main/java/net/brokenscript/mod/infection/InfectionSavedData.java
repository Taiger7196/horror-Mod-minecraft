package net.brokenscript.mod.infection;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * The world-saved "InfectionLevel" capability (0.0 - 100.0), persisted through
 * 1.21.5's {@link SavedDataType} codec system. Stored on the overworld's
 * {@code DimensionDataStorage}, resolving to
 * {@code saves/<world>/data/brokenscript_infection.dat}.
 *
 * <p>Also tracks the story progress ({@code storyChapter} = index of the next
 * undelivered {@code StoryChapter}) so USER_0's pages arrive exactly once and
 * in order for the lifetime of a world.</p>
 *
 * <p>Server-side only. The client receives a read-only mirror through
 * {@code InfectionSyncPayload}.</p>
 */
public class InfectionSavedData extends SavedData {
    public static final Codec<InfectionSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.fieldOf("infection").forGetter(data -> data.infection),
            Codec.BOOL.fieldOf("scriptRepaired").forGetter(data -> data.scriptRepaired),
            Codec.INT.optionalFieldOf("storyChapter", 0).forGetter(data -> data.storyChapter)
    ).apply(instance, InfectionSavedData::new));

    /** NeoForge convenience constructor: (id, initial constructor, codec). */
    public static final SavedDataType<InfectionSavedData> TYPE =
            new SavedDataType<>("brokenscript_infection", InfectionSavedData::new, CODEC);

    private double infection;
    private boolean scriptRepaired;
    private int storyChapter;

    public InfectionSavedData() {
        this(0.0D, false, 0);
    }

    private InfectionSavedData(double infection, boolean scriptRepaired, int storyChapter) {
        this.infection = infection;
        this.scriptRepaired = scriptRepaired;
        this.storyChapter = storyChapter;
    }

    public static InfectionSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public double level() {
        return infection;
    }

    public InfectionStage stage() {
        return InfectionStage.byLevel(infection);
    }

    public boolean isScriptRepaired() {
        return scriptRepaired;
    }

    /** Index of the next undelivered story chapter (see {@code story.StoryChapter}). */
    public int storyChapter() {
        return storyChapter;
    }

    public void setStoryChapter(int chapter) {
        if (chapter != this.storyChapter) {
            this.storyChapter = chapter;
            this.setDirty();
        }
    }

    public void set(double value) {
        double clamped = Math.clamp(value, 0.0D, 100.0D);
        if (clamped != this.infection) {
            this.infection = clamped;
            this.setDirty();
        }
    }

    public void add(double delta) {
        // Once the script has been repaired the world stays quiet - forever.
        if (delta > 0 && scriptRepaired) {
            return;
        }
        set(infection + delta);
    }

    public void markRepaired() {
        this.scriptRepaired = true;
        set(0.0D);
        this.setDirty();
    }

    /** Playtest helper: undo the repair so the haunting can be re-tested. */
    public void unrepair() {
        this.scriptRepaired = false;
        this.setDirty();
    }
}
