package ac.grim.grimac.checks.impl.multiactions;

import ac.grim.grimac.api.storage.verbose.Verbose;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketReceiveListener;
import ac.grim.grimac.checks.type.PreViaPacketReceiveListener;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerInput;
import org.jetbrains.annotations.NotNull;

@CheckData(name = "MultiActionsM", stableKey = "grim.multiactions.inventory_sneak", description = "Sneaking while in an inventory", experimental = true)
public class MultiActionsM extends Check implements PreViaPacketReceiveListener, PacketReceiveListener {
    private static final int TICK = 0;
    private static final int START = 1;
    private static final int STOP = 2;
    private static final Verbose V = Verbose
            .of("tick")
            .or("start")
            .or("stop");

    public MultiActionsM(GrimPlayer player) {
        super(player);
    }

    private boolean wasInputSneaking;

    @Override
    public void onPreViaPacketReceive(@NotNull PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.ENTITY_ACTION) {
            WrapperPlayClientEntityAction packet = new WrapperPlayClientEntityAction(event);
            switch (packet.getAction()) {
                case START_SNEAKING -> {
                    if (player.openWindow.mustBeOpen() && flag(V.write(verbose(), START)) && shouldModifyPackets()) {
                        player.closeInventory();
                    }
                }
                case STOP_SNEAKING -> {
                    if (player.openWindow.mustBeOpen() && player.openWindow.getTicksOpen() != 0
                            && flag(V.write(verbose(), STOP)) && shouldModifyPackets()) {
                        player.closeInventory();
                    }
                }
            }
        } else if (event.getPacketType() == PacketType.Play.Client.PLAYER_INPUT) {
            WrapperPlayClientPlayerInput packet = new WrapperPlayClientPlayerInput(event);
            boolean sneaking = packet.isShift();

            if (sneaking != this.wasInputSneaking) {
                boolean enoughTicks = player.openWindow.getTicksOpen() != 0 || sneaking;
                if (player.openWindow.mustBeOpen() && enoughTicks
                        && flag(V.write(verbose(), sneaking ? START : STOP)) && shouldModifyPackets()) {
                    player.closeInventory();
                }
            }

            this.wasInputSneaking = packet.isShift();
        } else if (event.getPacketType() == PacketType.Play.Client.CLIENT_TICK_END) {
            onTick();
        }
    }

    @Override
    public void onPacketReceive(@NotNull PacketReceiveEvent event) {
        if (!player.supportsEndTickPreVia()
                && WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())
                && !player.packetStateData.lastPacketWasTeleport
                && !player.packetStateData.lastPacketWasOnePointSeventeenDuplicate) {
            onTick();
        }
    }

    private void onTick() {
        if (MultiActionsC.isVerboseSneaking(player)
                && player.openWindow.mustBeOpen()
                && flag(V.write(verbose(), TICK))
                && shouldModifyPackets()) {
            player.closeInventory();
        }
    }
}
