package net.brokenscript.mod.client.hud;

import net.brokenscript.mod.client.ClientHorrorState;
import net.brokenscript.mod.infection.InfectionStage;
import net.brokenscript.mod.util.CorruptedNames;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.util.ARGB;

/**
 * "UI distortion" layer, registered above the hotbar in
 * {@code BrokenScriptClient#onRegisterGuiLayers}.
 *
 * <p>While a UI scramble is active (or permanently at stage 4) it draws
 * horizontal tear-lines, random obfuscated glyph runs, and misplaced
 * corrupted file names across the HUD.</p>
 */
public class GlitchHudLayer implements LayeredDraw.Layer {

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        boolean stage4 = ClientHorrorState.stage().atLeast(InfectionStage.COLLAPSE);
        if (!ClientHorrorState.uiScrambleActive() && !stage4) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui) {
            return;
        }
        var random = ClientHorrorState.RANDOM;
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();

        // Horizontal tear lines - thin translucent strips at random heights.
        int tears = stage4 ? 4 : 2;
        for (int i = 0; i < tears; i++) {
            if (random.nextInt(3) != 0) {
                continue; // flicker: not every frame
            }
            int y = random.nextInt(height);
            int h = 1 + random.nextInt(3);
            int color = ARGB.color(70 + random.nextInt(60), 255, 255, 255);
            graphics.fill(0, y, width, y + h, color);
        }

        // Obfuscated glyph runs in random corners.
        if (random.nextInt(4) == 0) {
            String glyphs = "\u00a7k" + "x".repeat(4 + random.nextInt(10));
            graphics.drawString(minecraft.font, glyphs,
                    random.nextInt(Math.max(1, width - 60)),
                    random.nextInt(Math.max(1, height - 20)),
                    ARGB.color(200, 170, 0, 0));
        }

        // A corrupted "file name" pinned near the crosshair, occasionally.
        if (random.nextInt(8) == 0) {
            String name = CorruptedNames.randomOf(CorruptedNames.CORRUPTED_FILE_NAMES, random);
            graphics.drawCenteredString(minecraft.font, name,
                    width / 2 + random.nextInt(41) - 20,
                    height / 2 + 12 + random.nextInt(9) - 4,
                    ARGB.color(230, 200, 30, 30));
        }
    }
}
