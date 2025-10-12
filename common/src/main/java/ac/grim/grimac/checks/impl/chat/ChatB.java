package ac.grim.grimac.checks.impl.chat;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.LogUtil;
import ac.grim.grimac.utils.reflection.ViaVersionUtil;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientChatCommand;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientChatCommandUnsigned;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientChatMessage;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.protocols.v1_10to1_11.Protocol1_10To1_11;
import com.viaversion.viaversion.protocols.v1_9_1to1_9_3.packet.ServerboundPackets1_9_3;

import java.util.UUID;

// this can false from click events, but I doubt this would actually
// happen unless they're trying to flag, or if the server is set up badly
@CheckData(name = "ChatB", description = "Invalid chat message")
public class ChatB extends Check implements PacketCheck {
    public ChatB(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.CHAT_MESSAGE) {
            String message = new WrapperPlayClientChatMessage(event).getMessage();
            if (checkChatMessage(message)) {
                event.setCancelled(true);
            }
        }

        if (event.getPacketType() == PacketType.Play.Client.CHAT_COMMAND_UNSIGNED) {
            String command = "/" + new WrapperPlayClientChatCommandUnsigned(event).getCommand();
            if (!command.stripTrailing().equals(command)) {
                if (flagAndAlert("command=" + command)) {
                    event.setCancelled(true);
                    player.onPacketCancel();
                }
            }
        }

        if (event.getPacketType() == PacketType.Play.Client.CHAT_COMMAND) {
            // TODO make previa after making wrapper parse by client version instead of server version
            String command = "/" + new WrapperPlayClientChatCommand(event).getCommand();
            if (!command.trim().equals(command)) {
                if (flagAndAlert("command=" + command)) {
                    event.setCancelled(true);
                    player.onPacketCancel();
                }
            }
        }
    }

    // returns whether the packet should be cancelled
    private boolean checkChatMessage(String message) {
        if (message.isEmpty() || !message.trim().equals(message) || message.startsWith("/") && PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_19)) {
            if (flagAndAlert("message=" + message) && shouldModifyPackets()) {
                player.onPacketCancel();
                return true;
            }
        }
        return false;
    }

    static {
        injectVia();
    }

    private static void injectVia() {
        if (!ViaVersionUtil.isAvailable || PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_11)) {
            return;
        }

        final var protocol = Via.getManager().getProtocolManager().getProtocol(Protocol1_10To1_11.class);
        if (protocol == null) {
            LogUtil.warn("Failed to inject ViaVersion message hook for 1.11+ clients: Protocol1_10To1_11 isn't registered!");
            return;
        }

        protocol.registerServerbound(ServerboundPackets1_9_3.CHAT, ServerboundPackets1_9_3.CHAT, wrapper -> {
            String msg = wrapper.read(Types.STRING);

            if (msg.length() > 100) {
                UUID uuid = wrapper.user().getProtocolInfo().getUuid();
                if (uuid != null) {
                    GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(uuid);
                    if (player != null && player.checkManager.getPacketCheck(ChatB.class).checkChatMessage(msg)) {
                        wrapper.cancel();
                        return;
                    }
                }

                msg = msg.substring(0, 100).trim();
            }

            wrapper.write(Types.STRING, msg);
        }, true);
    }
}
