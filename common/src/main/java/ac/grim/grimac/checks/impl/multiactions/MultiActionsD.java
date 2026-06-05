package ac.grim.grimac.checks.impl.multiactions;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;

@CheckData(name = "MultiActionsD", stableKey = "grim.multiactions.inventory_close_while_moving", description = "Closed inventory while moving")
public class MultiActionsD extends Check implements PacketCheck {
    public MultiActionsD(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.CLOSE_WINDOW) return;
        if (player.serverOpenedInventoryThisTick) return;

        String verbose = MultiActionsC.getVerbose(player);
        if (verbose.isEmpty()) return;

        // The client force-closes the inventory while inside a nether portal, sending this close
        // window packet even while moving. This only happens on 1.12.2 and newer clients.
        if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_12_2) && player.isInNetherPortal) return;

        // Don't cancel this packet, because it won't do anything except for making chests
        // look like they are still open (desynced),
        // and it can cause incompatibility issues with plugins
        flagAndAlert(verbose);
    }
}
