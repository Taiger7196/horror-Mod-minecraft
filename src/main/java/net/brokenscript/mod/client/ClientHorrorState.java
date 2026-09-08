package net.brokenscript.mod.client;

import net.brokenscript.mod.infection.InfectionStage;
import net.minecraft.util.RandomSource;

/**
 * Client-side mirror of the horror state. Written only from the render/client
 * thread (payload handlers run on the main client thread), read by HUD layers
 * and event handlers.
 */
public final class ClientHorrorState {
    public static final RandomSource RANDOM = RandomSource.create();

    private static double infectionLevel = 0.0D;

    // Remaining durations, in client ticks.
    private static int uiScrambleTicks = 0;
    private static int mouseScrambleTicks = 0;
    private static int fakeCrashTicks = 0;
    private static int invertControlsTicks = 0;

    /** The player's real sensitivity, saved before the scramble started. */
    private static Double savedSensitivity = null;

    private ClientHorrorState() {}

    public static void setInfectionLevel(double level) {
        infectionLevel = level;
    }

    public static double infectionLevel() {
        return infectionLevel;
    }

    public static InfectionStage stage() {
        return InfectionStage.byLevel(infectionLevel);
    }

    public static void startUiScramble(int ticks) {
        uiScrambleTicks = Math.max(uiScrambleTicks, ticks);
    }

    public static void startMouseScramble(int ticks) {
        mouseScrambleTicks = Math.max(mouseScrambleTicks, ticks);
    }

    public static void startFakeCrash(int ticks) {
        fakeCrashTicks = Math.max(fakeCrashTicks, ticks);
    }

    public static void startInvertControls(int ticks) {
        invertControlsTicks = Math.max(invertControlsTicks, ticks);
    }

    public static boolean uiScrambleActive() {
        return uiScrambleTicks > 0;
    }

    public static boolean mouseScrambleActive() {
        return mouseScrambleTicks > 0;
    }

    public static boolean fakeCrashActive() {
        return fakeCrashTicks > 0;
    }

    public static boolean invertControlsActive() {
        return invertControlsTicks > 0;
    }

    public static Double swapSavedSensitivity(Double value) {
        Double previous = savedSensitivity;
        savedSensitivity = value;
        return previous;
    }

    public static Double savedSensitivity() {
        return savedSensitivity;
    }

    /** Called once per client tick from {@code ClientEvents}. */
    public static void tickDown() {
        if (uiScrambleTicks > 0) uiScrambleTicks--;
        if (mouseScrambleTicks > 0) mouseScrambleTicks--;
        if (fakeCrashTicks > 0) fakeCrashTicks--;
        if (invertControlsTicks > 0) invertControlsTicks--;
    }
}
