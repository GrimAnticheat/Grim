package ac.grim.grimac.utils.viaversion;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.checks.impl.chat.ChatB;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.LogUtil;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.protocols.v1_10to1_11.Protocol1_10To1_11;
import com.viaversion.viaversion.protocols.v1_9_1to1_9_3.packet.ServerboundPackets1_9_3;
import lombok.experimental.UtilityClass;

import java.util.UUID;

@UtilityClass
class ViaVersionHooks {
    // this method's only purpose is getting <clinit> called
    static void load() {}

    static {
        inject1_11ChatHook();
    }

    private static void inject1_11ChatHook() {
        if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_11)) {
            return; // not needed
        }

        final Protocol1_10To1_11 protocol = Via.getManager().getProtocolManager().getProtocol(Protocol1_10To1_11.class);
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
