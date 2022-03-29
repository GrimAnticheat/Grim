package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import org.bukkit.GameMode;

@CheckData(name = "DisablerC")
public class BadPacketsK extends PacketCheck {
    public BadPacketsK(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.SPECTATE) {
            if (player.gamemode == GameMode.SPECTATOR) {
                reward();
            } else {
                flagAndAlert();
            }
        }
    }
}
