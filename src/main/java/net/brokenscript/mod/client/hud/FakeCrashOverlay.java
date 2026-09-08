package net.brokenscript.mod.client.hud;

import net.brokenscript.mod.client.ClientHorrorState;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.util.ARGB;

/**
 * The "simulated system failure": a full-screen fake crash report drawn above
 * every other GUI layer. Purely cosmetic - input keeps working, nothing is
 * actually wrong, and it dissolves on its own after a few seconds.
 */
public class FakeCrashOverlay implements LayeredDraw.Layer {

    private static final String[] REPORT_LINES = {
            "---- Minecraft Crash Report ----",
            "// I blame the player.",
            "",
            "Time: <REDACTED>",
            "Description: Unexpected observer in ticking world",
            "",
            "java.lang.IllegalStateException: entity 'you' does not belong here",
            "        at net.minecraft.world.level.Level.tick(Level.java:303)",
            "        at bs.script.Reality.assertIntact(Reality.java:505)",
            "        at bs.script.Reality.notice(you)",
            "",
            "-- Affected level --",
            "  All of it.",
            "",
            "This crash report is not saved to disk. Nothing is saved anymore.",
            "",
            "DO NOT REPORT THIS CRASH. IT ALREADY KNOWS."
    };

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        if (!ClientHorrorState.fakeCrashActive()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();

        graphics.fill(0, 0, width, height, ARGB.color(235, 12, 12, 12));

        int y = 24;
        for (String line : REPORT_LINES) {
            int color = line.startsWith("----") ? ARGB.color(255, 255, 85, 85)
                    : line.startsWith("DO NOT") ? ARGB.color(255, 200, 20, 20)
                    : ARGB.color(255, 200, 200, 200);
            graphics.drawString(minecraft.font, line, 24, y, color);
            y += 11;
        }

        // A flickering cursor, because the "crash" is still typing.
        if ((System.currentTimeMillis() / 400L) % 2 == 0) {
            graphics.drawString(minecraft.font, "_", 24, y + 6, ARGB.color(255, 220, 220, 220));
        }
    }
}
