package net.brokenscript.mod.client.screen;

import java.util.ArrayDeque;
import java.util.Deque;
import net.brokenscript.mod.network.serverbound.TerminalCommandPayload;
import net.brokenscript.mod.util.TerminalProtocol;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The simulated Command Line Interface for the Terminal Block.
 *
 * <p>Pure view layer: every submitted command is sent to the server via
 * {@link TerminalCommandPayload}; the server validates it against
 * {@link TerminalProtocol} and replies with a {@code TerminalResponsePayload}
 * (see {@link #acceptResponse}). The screen deliberately does NOT pause the
 * game - the wave assault outside keeps running while you type.</p>
 */
public class TerminalScreen extends Screen {
    private static final int MAX_LINES = 14;
    private static final int GREEN = ARGB.color(255, 80, 240, 100);
    private static final int DIM_GREEN = ARGB.color(255, 40, 140, 60);
    private static final int RED = ARGB.color(255, 220, 60, 60);

    private final BlockPos terminalPos;
    private int repairStep;
    private final Deque<Line> history = new ArrayDeque<>();

    private EditBox input;

    private record Line(String text, boolean error) {}

    public TerminalScreen(BlockPos terminalPos, int repairStep) {
        super(Component.literal("SCRIPT REPAIR TERMINAL v3.0"));
        this.terminalPos = terminalPos;
        this.repairStep = repairStep;
        pushLine("BROKEN SCRIPT (tm) RECOVERY CONSOLE", false);
        pushLine("type 'help' for the repair sequence. survive while you work.", false);
        pushLine(progressLine(), false);
    }

    @Override
    protected void init() {
        input = new EditBox(this.font, this.width / 2 - 150, this.height - 40, 300, 18,
                Component.literal("command"));
        input.setMaxLength(256);
        input.setBordered(true);
        addRenderableWidget(input);
        setInitialFocus(input);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257 || keyCode == 335) { // ENTER / KP_ENTER
            submit();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void submit() {
        String command = input.getValue().trim();
        input.setValue("");
        if (command.isEmpty()) {
            return;
        }
        pushLine("> " + command, false);

        // Local-only helpers; real commands go to the server.
        if (command.equalsIgnoreCase("help")) {
            pushLine("repair sequence (in order):", false);
            for (int i = 0; i < TerminalProtocol.COMMANDS.size(); i++) {
                String marker = i < repairStep ? "[done] " : "       ";
                pushLine("  " + marker + TerminalProtocol.COMMANDS.get(i), i < repairStep);
            }
            return;
        }
        if (command.equalsIgnoreCase("clear")) {
            history.clear();
            return;
        }
        PacketDistributor.sendToServer(new TerminalCommandPayload(terminalPos, command));
    }

    /** Called from {@code ClientPayloadHandlers} when the server replies. */
    public void acceptResponse(String line, int authoritativeStep, boolean accepted) {
        this.repairStep = authoritativeStep;
        pushLine(line, !accepted);
        pushLine(progressLine(), false);
        if (authoritativeStep >= TerminalProtocol.COMMANDS.size()) {
            pushLine("connection closed by remote host.", false);
        }
    }

    private String progressLine() {
        return "[integrity " + (repairStep * 100 / TerminalProtocol.COMMANDS.size())
                + "%] step " + Math.min(repairStep + 1, TerminalProtocol.COMMANDS.size())
                + "/" + TerminalProtocol.COMMANDS.size();
    }

    private void pushLine(String text, boolean error) {
        history.addLast(new Line(text, error));
        while (history.size() > MAX_LINES) {
            history.removeFirst();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        int left = this.width / 2 - 160;
        int top = 30;
        graphics.fill(left - 8, top - 10, this.width / 2 + 168, this.height - 50,
                ARGB.color(230, 5, 12, 5));

        graphics.drawString(this.font, this.title.getString(), left, top - 2, DIM_GREEN);
        int y = top + 14;
        for (Line line : history) {
            graphics.drawString(this.font, line.text(), left, y, line.error() ? RED : GREEN);
            y += 11;
        }
        // blinking block cursor next to the edit box
        if ((System.currentTimeMillis() / 500L) % 2 == 0) {
            graphics.drawString(this.font, ">", this.width / 2 - 162, this.height - 36, GREEN);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false; // the waves outside do not pause. neither do you.
    }
}
