package ac.grim.grimac.checks.impl.aim;

import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.RotationListener;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.RotationUpdate;

@CheckData(name = "AimStaticX", stableKey = "grim.aim.static_x", description = "Scanned X rotation while looking vertically.")
public class AimStaticX extends Check implements RotationListener {

    private double buffer = 0;
    private double decay;
    private int maxBuffer;
    private double minDeltaY, maxDeltaX;

    public AimStaticX(GrimPlayer playerData) {
        super(playerData);
    }

    @Override
    public void process(final RotationUpdate rotationUpdate) {
        double deltaX = rotationUpdate.getDeltaXRotABS();
        double deltaY = rotationUpdate.getDeltaYRotABS();
        if (player.compensatedEntities.self.getRiding() != null) {
            return; //Fix false positives in boats and other entities
        }
        if (Math.abs(rotationUpdate.getTo().pitch()) == 90) {
            return; //Ignore 90 and -90 pitch rotations
        }

        if (player.packetStateData.lastPacketWasTeleport) {
            return;
        }
        if (deltaX <= maxDeltaX && deltaY >= minDeltaY) {
            if (buffer++ > maxBuffer) {
                flag("deltaX=" + deltaX + " deltaY=" + deltaY);
            }
        } else {
            buffer = Math.max(0, buffer - decay);
            if (buffer == 0) {
                reward();
            }
        }
    }

    @Override
    public void onReload(ConfigManager config) {
        maxBuffer = config.getIntElse(getConfigName() + ".buffer", 7);
        decay = config.getDoubleElse(getConfigName() + ".decay", 1);
        minDeltaY = config.getDoubleElse(getConfigName() + ".minDeltaY", 1D);
        maxDeltaX = config.getDoubleElse(getConfigName() + ".maxDeltaX", 0.0001D);
    }
}
