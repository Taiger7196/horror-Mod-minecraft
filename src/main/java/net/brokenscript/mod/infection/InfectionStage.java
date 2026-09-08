package net.brokenscript.mod.infection;

/**
 * The four escalation stages of the haunting.
 *
 * <ul>
 *   <li>Stage 1 (0-25): subtle audio/visual anomalies, fake ambient sounds.</li>
 *   <li>Stage 2 (26-50): spectator entities at long range, tab/chat spoofing begins.</li>
 *   <li>Stage 3 (51-75): active aggression, UI distortion, chat corruption.</li>
 *   <li>Stage 4 (76-100): world degradation, The Reaper, simulated system failures.</li>
 * </ul>
 */
public enum InfectionStage {
    DORMANT(1, 0.0D),
    WATCHING(2, 25.0D),
    HUNTING(3, 50.0D),
    COLLAPSE(4, 75.0D);

    private final int stageNumber;
    private final double minLevel;

    InfectionStage(int stageNumber, double minLevel) {
        this.stageNumber = stageNumber;
        this.minLevel = minLevel;
    }

    public int stageNumber() {
        return stageNumber;
    }

    public double minLevel() {
        return minLevel;
    }

    public static InfectionStage byLevel(double level) {
        return switch ((int) Math.clamp(level, 0.0D, 100.0D) / 25) {
            case 0 -> DORMANT;
            case 1 -> WATCHING;
            case 2 -> HUNTING;
            default -> COLLAPSE;
        };
    }

    public boolean atLeast(InfectionStage other) {
        return this.stageNumber >= other.stageNumber;
    }
}
