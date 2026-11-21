package ac.grim.grimac.checks.impl.chat;

import ac.grim.grimac.api.storage.verbose.Verbose;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.packethandler.PacketHandlerRegistryBuilder;
import ac.grim.grimac.checks.type.PacketListener;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientChatCommand;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientChatCommandUnsigned;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientChatMessage;
import org.jetbrains.annotations.NotNull;

// this can false from click events, but I doubt this would actually
// happen unless they're trying to flag, or if the server is set up badly
@CheckData(name = "ChatB", stableKey = "grim.exploit.spigot_antispam_bypass", description = "Invalid chat message")
public class ChatB extends Check implements PacketListener {
    private static final Verbose V = Verbose.of("[message|command]={str}");

    public ChatB(GrimPlayer player) {
        super(player);
    }

    @Override
    public void registerPacketHandlers(@NotNull PacketHandlerRegistryBuilder registry) {
        registry.registerPreViaReceiveHandler(event -> {
            String message = new WrapperPlayClientChatMessage(event).getMessage();
            if (message.isEmpty() || !message.trim().equals(message)
                    || message.startsWith("/") && player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_19)) {
                if (flag(V.write(verbose()).bool(true).str(message)) && shouldModifyPackets()) {
                    player.onPacketCancel();
                    event.setCancelled(true);
                }
            }
        }, PacketType.Play.Client.CHAT_MESSAGE);

        registry.registerPreViaReceiveHandler(event -> {
            String command = "/" + new WrapperPlayClientChatCommandUnsigned(event).getCommand();
            if (!command.stripTrailing().equals(command) && flag(V.write(verbose()).bool(false).str(command))) {
                event.setCancelled(true);
                player.onPacketCancel();
            }
        }, PacketType.Play.Client.CHAT_COMMAND_UNSIGNED);

        registry.registerPreViaReceiveHandler(event -> {
            String command = "/" + new WrapperPlayClientChatCommand(event).getCommand();
            if (!command.trim().equals(command) && flag(V.write(verbose()).bool(false).str(command))) {
                event.setCancelled(true);
                player.onPacketCancel();
            }
        }, PacketType.Play.Client.CHAT_COMMAND);
    }
}
