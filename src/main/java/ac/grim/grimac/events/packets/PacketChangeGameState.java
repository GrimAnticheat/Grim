package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChangeGameState;

public class PacketChangeGameState extends Check implements PacketCheck {
    public PacketChangeGameState(GrimPlayer playerData) {
        super(playerData);
    }

    @Override
    public void onPacketSend(final PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.CHANGE_GAME_STATE) {
            WrapperPlayServerChangeGameState packet = new WrapperPlayServerChangeGameState(event);

            if (packet.getReason() == WrapperPlayServerChangeGameState.Reason.CHANGE_GAME_MODE) {
                player.sendTransaction();

                player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> {
                    // Bukkit's gamemode order is unreliable, so go from int -> packetevents -> bukkit
                    GameMode previous = player.gamemode;
                    int gamemode = (int) packet.getValue();

                    // Some plugins send invalid values such as -1, this is what the client does
                    if (gamemode < 0 || gamemode >= GameMode.values().length) {
                        player.gamemode = GameMode.SURVIVAL;
                    } else {
                        player.gamemode = GameMode.values()[gamemode];
                    }

                    if (previous == GameMode.SPECTATOR && player.gamemode != GameMode.SPECTATOR) {
                        GrimAPI.INSTANCE.getSpectateManager().handlePlayerStopSpectating(player.playerUUID);
                    }
                });
            }
        }
    }
}
