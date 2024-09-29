package ac.grim.grimac.checks.impl.multiactions;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;

@CheckData(name = "MultiActionsD", experimental = true)
public class MultiActionsD extends Check implements PacketCheck {
    public MultiActionsD(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.CLOSE_WINDOW) {
            String verbose = player.isSprinting ? "sprinting" : "";

            if (player.packetStateData.isSlowedByUsingItem()) {
                verbose += (verbose.isEmpty() ? "" : ", ") + "using";
            }

            if (!verbose.isEmpty() && flagAndAlert(verbose) && shouldModifyPackets()) {
                event.setCancelled(true);
                player.onPacketCancel();
            }
        }
    }
}
