package ac.grim.grimac.checks.impl.misc;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import org.bukkit.Bukkit;
import org.bukkit.entity.Boat;

@CheckData(name = "Invalid Boat Exit A")
public class InvalidBoatExitA extends Check implements PacketCheck {
    int boatLeaveCounter = 0;
    public InvalidBoatExitA(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9)) //Boats arent exempt on 1.9+
            return;

        if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) { //Check if it is a flying.
            if (player.bukkitPlayer.isInsideVehicle() && player.bukkitPlayer.getVehicle() instanceof Boat) { //Check if we are in a boat.
                boatLeaveCounter = 2;
            } else { //note: i give up on comments after this point sorry >.>
                double deltaY = player.y-player.lastY;
                switch (--boatLeaveCounter) {
                    //hard coded? yes. do i care? no. does it work: yes
                    case 2:
                    case 1:
                        Bukkit.broadcastMessage(String.valueOf(deltaY));
                        break;
                }
            }
        }

    }
}
