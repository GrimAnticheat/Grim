package ac.grim.grimac.checks.impl.multiactions;

import ac.grim.grimac.api.storage.verbose.VerboseSchema;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;

@CheckData(name = "MultiActionsD", stableKey = "grim.multiactions.inventory_close_while_moving", verboseVersion = 1, description = "Closed inventory while moving")
public class MultiActionsD extends Check implements PacketCheck {
    public static final VerboseSchema V = MultiActionsC.verboseSchema();

    public MultiActionsD(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.CLOSE_WINDOW) return;
        if (player.serverOpenedInventoryThisTick) return;

        boolean sprinting = MultiActionsC.isVerboseSprinting(player);
        boolean sneaking = MultiActionsC.isVerboseSneaking(player);
        boolean input = MultiActionsC.isVerboseInput(player);
        if (!sprinting && !sneaking && !input) return;

        // The client force-closes the inventory while inside a nether portal, sending this close
        // window packet even while moving. This only happens on 1.12.2 and newer clients.
        if (player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_12_2) && player.isInNetherPortal) return;

        // Don't cancel this packet, because it won't do anything except for making chests
        // look like they are still open (desynced),
        // and it can cause incompatibility issues with plugins
        flagAndAlert(V.write(verbose()).bool(sprinting).bool(sneaking).bool(input));
    }
}
