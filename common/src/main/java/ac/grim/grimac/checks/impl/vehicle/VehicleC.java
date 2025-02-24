package ac.grim.grimac.checks.impl.vehicle;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;

@CheckData(name = "VehicleC")
public class VehicleC extends Check implements PacketCheck {
    public VehicleC(GrimPlayer player) {
        super(player);
    }
}
