package ac.grim.grimac.utils.viaversion;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.checks.impl.badpackets.BadPacketsM;
import ac.grim.grimac.checks.impl.chat.ChatB;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.LogUtil;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.viaversion.viabackwards.protocol.v1_15to1_14_4.Protocol1_15To1_14_4;
import com.viaversion.viabackwards.protocol.v1_15to1_14_4.storage.ImmediateRespawnStorage;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.protocols.v1_10to1_11.Protocol1_10To1_11;
import com.viaversion.viaversion.protocols.v1_13_2to1_14.packet.ServerboundPackets1_14;
import com.viaversion.viaversion.protocols.v1_14_4to1_15.packet.ClientboundPackets1_15;
import com.viaversion.viaversion.protocols.v1_9_1to1_9_3.packet.ServerboundPackets1_9_3;
import lombok.experimental.UtilityClass;

import java.util.Objects;
import java.util.UUID;

@UtilityClass
class ViaVersionHooks {
    // this method's only purpose is getting <clinit> called
    static void load() {}

    static {
        inject1_11ChatHook();
        if (ViaVersionUtil.hasViaBackwards) {
            inject1_14_4ImmediateRespawnHook();
        }
    }

    private static void inject1_11ChatHook() {
        if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_11)) {
            return; // not needed
        }

        final Protocol1_10To1_11 protocol = Via.getManager().getProtocolManager().getProtocol(Protocol1_10To1_11.class);
        if (protocol == null) {
            LogUtil.warn("Failed to inject ViaVersion chat message hook for 1.11+ clients: Protocol1_10To1_11 isn't registered!");
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

    private static void inject1_14_4ImmediateRespawnHook() {
        if (PacketEvents.getAPI().getServerManager().getVersion().isOlderThan(ServerVersion.V_1_15)) {
            return; // not needed
        }

        final Protocol1_15To1_14_4 protocol = Via.getManager().getProtocolManager().getProtocol(Protocol1_15To1_14_4.class);
        if (protocol == null) {
            LogUtil.warn("Failed to inject ViaVersion immediate respawn hook for pre-1.15 clients: Protocol1_15To1_14_4 isn't registered!");
            return;
        }

        protocol.replaceClientbound(ClientboundPackets1_15.SET_HEALTH, wrapper -> {
            float health = wrapper.passthrough(Types.FLOAT);
            if (health > 0) return;
            ImmediateRespawnStorage storage = wrapper.user().get(ImmediateRespawnStorage.class);
            if (!Objects.requireNonNull(storage).isImmediateRespawn()) return;

            UUID uuid = wrapper.user().getProtocolInfo().getUuid();
            if (uuid != null) {
                GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(uuid);
                if (player != null) {
                    player.checkManager.getPacketCheck(BadPacketsM.class).exemptVia();
                }
            }

            PacketWrapper statusPacket = wrapper.create(ServerboundPackets1_14.CLIENT_COMMAND);
            statusPacket.write(Types.VAR_INT, 0);
            statusPacket.sendToServer(Protocol1_15To1_14_4.class);
        });
    }
}
