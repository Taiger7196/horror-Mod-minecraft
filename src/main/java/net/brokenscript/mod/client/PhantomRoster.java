package net.brokenscript.mod.client;

import com.mojang.authlib.GameProfile;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.world.level.GameType;

/**
 * Tab-list spoofing: fabricates {@link ClientboundPlayerInfoUpdatePacket}s on
 * the client and feeds them through the vanilla handler, so phantom players
 * ("Null", "Entity_303", ...) appear in the TAB overlay with a 999ms ping.
 *
 * <p>The packet's private {@code entries} list is opened up by the access
 * transformer ({@code META-INF/accesstransformer.cfg}); everything else goes
 * through vanilla code paths, so the tab overlay, skin lookup, and list order
 * behave exactly like a real (if unsettling) player entry.</p>
 */
public final class PhantomRoster {
    private static final Set<String> ACTIVE_PHANTOMS = new HashSet<>();
    private static final int PHANTOM_PING_MS = 999;

    public static void addPhantom(String name) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null || !ACTIVE_PHANTOMS.add(name)) {
            return;
        }
        var entry = new ClientboundPlayerInfoUpdatePacket.Entry(
                phantomId(name),
                new GameProfile(phantomId(name), name),
                true,                       // listed in the tab overlay
                PHANTOM_PING_MS,            // the famous 999ms ping
                GameType.SURVIVAL,
                Component.literal(name).withStyle(ChatFormatting.DARK_GRAY),
                true,                       // showHat
                Integer.MIN_VALUE,          // listOrder: sink to the bottom of the list
                null);                      // no chat session - they never speak. yet.

        var packet = new ClientboundPlayerInfoUpdatePacket(
                EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER,
                        ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED,
                        ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LATENCY,
                        ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME),
                List.of());
        packet.entries = List.of(entry); // AT-opened field
        connection.handlePlayerInfoUpdate(packet);
    }

    public static void removePhantom(String name) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null || !ACTIVE_PHANTOMS.remove(name)) {
            return;
        }
        connection.handlePlayerInfoRemove(new ClientboundPlayerInfoRemovePacket(List.of(phantomId(name))));
    }

    public static void clear() {
        ACTIVE_PHANTOMS.clear();
    }

    private static UUID phantomId(String name) {
        return UUID.nameUUIDFromBytes(("brokenscript:phantom:" + name).getBytes(StandardCharsets.UTF_8));
    }

    private PhantomRoster() {}
}
