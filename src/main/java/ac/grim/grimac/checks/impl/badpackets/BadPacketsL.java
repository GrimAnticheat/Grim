package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;

//checks for impossible dig packets
@CheckData(name = "BadPacketsL")
public class BadPacketsL extends Check implements PacketCheck {

    public BadPacketsL(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
            WrapperPlayClientPlayerDigging packet = new WrapperPlayClientPlayerDigging(event);
            if (packet.getAction() == DiggingAction.RELEASE_USE_ITEM) {
                // 1.8 and above clients only send this packet in one place, with BlockPos.ZERO and Direction.DOWN
                // 1.7 and below clients send this packet in the same place, except use Direction.SOUTH
                if ((packet.getBlockFace() != BlockFace.DOWN && player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_8))
                        || (packet.getBlockFace() != BlockFace.SOUTH && player.getClientVersion().isOlderThan(ClientVersion.V_1_8))
                        || packet.getBlockPosition().getX() != 0
                        || packet.getBlockPosition().getY() != 0
                        || packet.getBlockPosition().getZ() != 0) {
                    flagAndAlert();
                }
            }
        }
    }
}
