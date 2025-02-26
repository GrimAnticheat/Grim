package ac.grim.grimac.checks.impl.vehicle;

import ac.grim.grimac.checks.AbstractPacketCheck;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.player.GrimPlayer;

@CheckData(name = "VehicleC")
public class VehicleC extends AbstractPacketCheck {
    public VehicleC(GrimPlayer player) {
        super(player);
    }
}
