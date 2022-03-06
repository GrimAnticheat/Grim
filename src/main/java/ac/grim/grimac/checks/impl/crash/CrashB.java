package ac.grim.grimac.checks.impl.crash;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClientStatus;
import org.bukkit.GameMode;

@CheckData(name = "CrashB")
public class CrashB extends PacketCheck {
    public CrashB(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.SET_CREATIVE_SLOT) {
            if (player.gamemode == GameMode.CREATIVE) {
                reward();
            } else {
                flagAndAlert(); // Ban
            }
        }
    }
}
