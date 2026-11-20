package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.packethandler.PacketHandlerRegistryBuilder;
import ac.grim.grimac.checks.type.PacketListener;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import org.jetbrains.annotations.NotNull;

@CheckData(name = "BadPacketsZ", stableKey = "grim.badpackets.duplicate_player_input", description = "Sent duplicate player input packets in the same client tick", experimental = true)
public class BadPacketsZ extends Check implements PacketListener {
    private boolean sent;

    public BadPacketsZ(GrimPlayer player) {
        super(player);
    }

    @Override
    public boolean isApplicable() {
        return player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_21_2);
    }

    @Override
    public void registerPacketHandlers(@NotNull PacketHandlerRegistryBuilder registry) {
        registry.registerPreViaReceiveHandler(event -> sent = false, PacketType.Play.Client.CLIENT_TICK_END);
        registry.registerPreViaReceiveHandler(event -> {
            if (sent) {
                flag();
            }

            sent = true;
        }, PacketType.Play.Client.PLAYER_INPUT);
    }
}
