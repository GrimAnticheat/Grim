package ac.grim.grimac.checks.impl.packetorder;

import ac.grim.grimac.api.storage.verbose.VerboseSchema;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.impl.verbose.VerboseCodecs;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;

import static com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying.isFlying;

@CheckData(name = "PacketOrderO", stableKey = "grim.packetorder.tick_end_order", verboseVersion = 2, description = "Sent packets after movement before the expected client tick end", experimental = true)
public class PacketOrderO extends Check implements PacketCheck {
    public static final VerboseSchema V = VerboseSchema.of(2, "packetId:zz");

    public PacketOrderO(final GrimPlayer player) {
        super(player);
    }

    private boolean flying;

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.CLIENT_TICK_END) {
            flying = false;
        }

        if (isFlying(event.getPacketType()) && player.supportsEndTick() && !player.packetStateData.lastPacketWasTeleport) {
            flying = true;
            return;
        }

        if (flying && !isAsync(event.getPacketType()) && event.getPacketType() != PacketType.Play.Client.VEHICLE_MOVE) {
            if (player.inVehicle() && event.getPacketType() == PacketType.Play.Client.ENTITY_ACTION) {
                WrapperPlayClientEntityAction.Action action = new WrapperPlayClientEntityAction(event).getAction();
                if (action == WrapperPlayClientEntityAction.Action.START_SPRINTING || action == WrapperPlayClientEntityAction.Action.STOP_SPRINTING) {
                    return;
                }
            }

            int packetId = VerboseCodecs.packetId(event.getPacketType(), player.getClientVersion());
            flagAndAlert(V.write(verbose()).zz(packetId));
        }
    }
}
