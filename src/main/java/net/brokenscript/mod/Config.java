package net.brokenscript.mod;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Common config. Horror should always be consensual at the meta level:
 * everything invasive (real file writes, fake crashes, control inversion)
 * can be switched off here.
 */
public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ENABLED = BUILDER
            .comment("Master switch for the entire horror director. Set false to pause the haunting.")
            .define("enabled", true);

    public static final ModConfigSpec.DoubleValue INFECTION_RATE = BUILDER
            .comment("Multiplier for how fast the world infection level rises. 1.0 targets a 20+ hour arc.")
            .defineInRange("infectionRateMultiplier", 1.0D, 0.0D, 100.0D);

    public static final ModConfigSpec.IntValue MIN_EVENT_INTERVAL = BUILDER
            .comment("Minimum seconds between scripted horror events per player.")
            .defineInRange("minEventIntervalSeconds", 45, 5, 3600);

    public static final ModConfigSpec.BooleanValue ALLOW_UNCANNY_LOGS = BUILDER
            .comment("Allow the mod to write creepy .txt files into <gamedir>/LOGS_UNCANNY/.")
            .define("allowUncannyLogs", true);

    public static final ModConfigSpec.BooleanValue ALLOW_FAKE_CRASH = BUILDER
            .comment("Allow fake crash overlays / simulated system failures on the client.")
            .define("allowFakeCrashOverlays", true);

    public static final ModConfigSpec.BooleanValue ALLOW_INPUT_GLITCHES = BUILDER
            .comment("Allow temporary control inversion and mouse sensitivity scrambling.")
            .define("allowInputGlitches", true);

    public static final ModConfigSpec.BooleanValue ALLOW_WORLD_DEGRADATION = BUILDER
            .comment("Allow stage 4 events to physically corrupt blocks near players.")
            .define("allowWorldDegradation", true);

    static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {}
}
