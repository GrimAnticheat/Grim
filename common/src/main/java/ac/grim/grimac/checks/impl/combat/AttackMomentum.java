package ac.grim.grimac.checks.impl.combat;

import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import ac.grim.grimac.utils.math.GrimMath;

@CheckData(name = "AttackMomentum", description = "Checks for unnatural momentum during combat", setback = 5)
public class AttackMomentum extends Check implements PostPredictionCheck {

    private double threshold;
    private double lastHVel = -1.0;
    private int buffer = 0;

    public AttackMomentum(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPredictionComplete(PredictionComplete predictionComplete) {
        if (!predictionComplete.isChecked()) return;

        double hVel = Math.sqrt(GrimMath.square(player.actualMovement.getX()) + GrimMath.square(player.actualMovement.getZ()));

        if (player.minAttackSlow > 0) {
            if (lastHVel != -1.0) {
                double diff = Math.abs(hVel - lastHVel);

                if (diff < threshold && hVel > 0.1) {
                    if (++buffer > 4) {
                        flagAndAlertWithSetback();
                    }
                } else {
                    buffer = Math.max(0, buffer - 1);
                    reward();
                }
            }
        } else {
            buffer = 0;
        }

        lastHVel = hVel;
    }

    @Override
    public void onReload(ConfigManager config) {
        threshold = config.getDoubleElse(getConfigName() + ".threshold", 1E-9);
    }
}
