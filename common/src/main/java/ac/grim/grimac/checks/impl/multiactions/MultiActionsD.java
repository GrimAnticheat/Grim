package ac.grim.grimac.checks.impl.multiactions;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;

import java.util.StringJoiner;

@CheckData(name = "MultiActionsD", description = "Closed inventory while moving", experimental = true)
public class MultiActionsD extends Check implements PacketCheck {
    public MultiActionsD(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.CLOSE_WINDOW) {
            StringJoiner verbose = new StringJoiner(", ");
            if (player.isSprinting && (!player.isSwimming || !player.clientClaimsLastOnGround)) {
                verbose.add("sprinting");
            }

            if (player.isSneaking && player.getClientVersion().isOlderThan(ClientVersion.V_1_15)) {
                verbose.add("sneaking");
            }

            if (player.supportsEndTick() && (player.packetStateData.knownInput.forward()
                    || player.packetStateData.knownInput.backward() || player.packetStateData.knownInput.left()
                    || player.packetStateData.knownInput.right() || player.packetStateData.knownInput.jump())) {
                verbose.add("input");
            }

            String joinedVerbose = verbose.toString();

            if (!joinedVerbose.isEmpty() && flagAndAlert(joinedVerbose) && shouldModifyPackets()) {
                event.setCancelled(true);
                player.onPacketCancel();
            }
        }
    }
}
