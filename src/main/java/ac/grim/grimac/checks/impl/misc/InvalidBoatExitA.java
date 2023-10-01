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

@CheckData(name = "InvalidBoatExitA", configName = "InvalidBoatExitA", experimental = false)
public class InvalidBoatExitA extends Check implements PacketCheck {
    int boatLeaveCounter = 0;
    public InvalidBoatExitA(GrimPlayer player) {
        super(player);
    }
    double[] deltaYValues = new double[] {
        0,
        0,
        0,
        -0.0784,
        -0.15,
        -0.11
    };

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9)) //Boats arent exempt on 1.9+
            return;

        if (WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) { //Check if it is a flying.
            if (player.bukkitPlayer.isInsideVehicle() && player.bukkitPlayer.getVehicle() instanceof Boat) { //Check if we are in a boat.
                boatLeaveCounter = 6;
            } else { //note: i give up on comments after this point sorry >.>
                double deltaY = player.y-player.lastY;
                switch (boatLeaveCounter) {
                    //hard coded? yes. do i care? no. does it work: yes
                    case 6:
                    case 5:
                    case 4:
                    case 3:
                    case 2:
                    case 1:
                        //check if deltaY is possible

                        if (Math.abs(deltaYValues[boatLeaveCounter-1]-deltaY) > 0.7) {
                            //small boat longjumps r fine, dont care really, this shouldnt increase vl just alert.
                            flagAndAlert("Invalid boat exit, offset: " + Math.abs(deltaYValues[boatLeaveCounter-1]-deltaY));
                            //flagAndAlert("Invalid boat exit, offset: " + Math.abs(deltaYValues[boatLeaveCounter-1]-deltaY));
                        }

                        break;
                    default:
                        break;
                }
                boatLeaveCounter -= 1;
            }
        }

    }
}
