package ac.grim.grimac.checks.impl.vehicle;

import ac.grim.grimac.api.storage.verbose.VerboseSchema;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.KnownInput;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientSteerBoat;

@CheckData(name = "VehicleF", stableKey = "grim.vehicle.boat_input_mismatch", experimental = true, description = "Sent incorrect boat paddle states", verboseVersion = 1)
public class VehicleF extends Check implements PacketCheck {
    public static final VerboseSchema V = VerboseSchema.of(
            "sentLeft:bool",
            "sentRight:bool",
            "expectedLeft:bool",
            "expectedRight:bool");

    public VehicleF(GrimPlayer player) {
        super(player);
    }

    private PacketEntity lastTickVehicle;

    @Override
    public void onPacketReceive(final PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.STEER_BOAT) {
            // lastVehicleSwitch isn't updated by this time.
            if (lastTickVehicle != player.getVehicle()) return;

            WrapperPlayClientSteerBoat packet = new WrapperPlayClientSteerBoat(event);

            boolean expectedLeft;
            boolean expectedRight;

            if (player.supportsEndTick()) {
                KnownInput input = player.packetStateData.knownInput;
                expectedLeft = input.forward() || !input.left() && input.right();
                expectedRight = input.forward() || input.left() && !input.right();
            } else {
                expectedLeft = player.vehicleData.nextVehicleForward > 0 || player.vehicleData.nextVehicleHorizontal < 0;
                expectedRight = player.vehicleData.nextVehicleForward > 0 || player.vehicleData.nextVehicleHorizontal > 0;

                if (player.vehicleData.nextVehicleForward == 0 && packet.isLeftPaddleTurning() && packet.isRightPaddleTurning()) {
                    return; // the player is pressing forward and backward
                }
            }

            if (packet.isLeftPaddleTurning() != expectedLeft || packet.isRightPaddleTurning() != expectedRight) {
                boolean sentLeft = packet.isLeftPaddleTurning();
                boolean sentRight = packet.isRightPaddleTurning();
                if (flagAndAlert(V.write(verbose()).bool(sentLeft).bool(sentRight).bool(expectedLeft).bool(expectedRight))
                    && shouldModifyPackets()) {
                    packet.setLeftPaddleTurning(expectedLeft);
                    packet.setRightPaddleTurning(expectedRight);
                    event.markForReEncode(true);
                }
            }
        }

        if (isTickPacket(event.getPacketType())) {
            lastTickVehicle = player.getVehicle();
        }
    }
}
