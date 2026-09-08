package net.brokenscript.mod.util;

import java.util.List;

/**
 * The repair command sequence for the "Fixing the Script" end-game quest.
 * Shared between the client CLI screen (for local echo / hints) and the
 * server (authoritative validation in {@code ModNetworking}).
 */
public final class TerminalProtocol {
    public static final int REQUIRED_FRAGMENTS = 8;

    /** Commands must be entered in order. The server validates each one. */
    public static final List<String> COMMANDS = List.of(
            "scan --deep",
            "isolate --entity=all",
            "purge --corrupted --force",
            "rebuild boot.sector",
            "commit fixed_the_script");

    public static final List<String> RESPONSES = List.of(
            "SCAN COMPLETE. 6 FOREIGN PROCESSES FOUND. THEY KNOW YOU ARE HERE.",
            "ISOLATION FIELD RAISED. CONTAINMENT AT 34%. HOLD THE ROOM.",
            "PURGING... SEGMENTATION FAULT IN CHUNK [?,?]. RETRYING. SURVIVE.",
            "BOOT SECTOR REBUILT FROM 8 FRAGMENTS. ONE COMMAND REMAINS.",
            "COMMIT ACCEPTED. SCRIPT INTEGRITY RESTORED. THANK YOU, PLAYER.");

    public static boolean matches(int step, String input) {
        return step >= 0 && step < COMMANDS.size()
                && COMMANDS.get(step).equalsIgnoreCase(input.trim());
    }

    private TerminalProtocol() {}
}
