package ac.grim.grimac.checks.impl.sprint;

import ac.grim.grimac.api.storage.verbose.Verbose;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.impl.multiactions.MultiActionsC;
import ac.grim.grimac.checks.type.PacketReceiveListener;
import ac.grim.grimac.checks.type.PreViaPacketReceiveListener;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import org.jetbrains.annotations.NotNull;

@CheckData(name = "SprintH", stableKey = "grim.sprint.inventory", description = "Sprinting while in an inventory", experimental = true)
public class SprintH extends Check implements PreViaPacketReceiveListener, PacketReceiveListener {
    private static final int TICK = 0;
    private static final int START = 1;
    private static final Verbose V = Verbose.of("tick").or("start");

    public SprintH(@NotNull GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPreViaPacketReceive(@NotNull PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.ENTITY_ACTION) {
            WrapperPlayClientEntityAction packet = new WrapperPlayClientEntityAction(event);
            if (packet.getAction() != WrapperPlayClientEntityAction.Action.START_SPRINTING) return;
            if (player.openWindow.mustBeOpen() && flag(V.write(verbose(), START)) && shouldModifyPackets()) {
                player.closeInventory();
            }
            return;
        }

        if (!player.supportsEndTickPreVia() || event.getPacketType() != PacketType.Play.Client.CLIENT_TICK_END
                || !player.openWindow.mustBeOpen()) return;

        boolean sprinting = player.packetStateData.knownInput.sprint()
                && player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_21_9)
                || MultiActionsC.isVerboseSprinting(player);
        if (sprinting && flag(V.write(verbose(), TICK)) && shouldModifyPackets()) {
            player.closeInventory();
        }
    }

    @Override
    public void onPacketReceive(@NotNull PacketReceiveEvent event) {
        if (player.supportsEndTickPreVia()
                || !WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())
                || player.packetStateData.lastPacketWasTeleport
                || !player.openWindow.mustBeOpen()) return;

        if (MultiActionsC.isVerboseSprinting(player) && flag(V.write(verbose(), TICK)) && shouldModifyPackets()) {
            player.closeInventory();
        }
    }
}
