package ac.grim.grimac.checks.impl.aim.processor;

import ac.grim.grimac.checks.type.RotationCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.RotationUpdate;
import ac.grim.grimac.utils.math.GraphUtil;

import java.util.ArrayList;
import java.util.List;

// Frequency
public class Cinematic extends RotationCheck {
    private final List<Double> yawSamples = new ArrayList<>(20);
    private final List<Double> pitchSamples = new ArrayList<>(20);
    private long lastSmooth = 0L, lastHighRate = 0L;
    private double lastDeltaYaw = 0.0d, lastDeltaPitch = 0.0d;

    public Cinematic(final GrimPlayer playerData) {
        super(playerData);
    }

    @Override
    public void process(final RotationUpdate rotationUpdate) {
        final long now = System.currentTimeMillis();

        final double deltaYaw = rotationUpdate.getDeltaYaw();
        final double deltaPitch = rotationUpdate.getDeltaPitch();

        final double differenceYaw = Math.abs(deltaYaw - lastDeltaYaw);
        final double differencePitch = Math.abs(deltaPitch - lastDeltaPitch);

        final double joltYaw = Math.abs(differenceYaw - deltaYaw);
        final double joltPitch = Math.abs(differencePitch - deltaPitch);

        final boolean cinematic = (now - lastHighRate > 250L) || now - lastSmooth < 9000L;

        if (joltYaw > 1.0 && joltPitch > 1.0) {
            this.lastHighRate = now;
        }

        if (deltaYaw > 0.0 && deltaPitch > 0.0) {
            yawSamples.add(deltaYaw);
            pitchSamples.add(deltaPitch);
        }

        if (yawSamples.size() == 20 && pitchSamples.size() == 20) {
            // Get the cerberus/positive graph of the sample-lists
            final GraphUtil.GraphResult resultsYaw = GraphUtil.getGraphNoString(yawSamples);
            final GraphUtil.GraphResult resultsPitch = GraphUtil.getGraphNoString(pitchSamples);

            // Negative values
            final int negativesYaw = resultsYaw.getNegatives();
            final int negativesPitch = resultsPitch.getNegatives();

            // Positive values
            final int positivesYaw = resultsYaw.getPositives();
            final int positivesPitch = resultsPitch.getPositives();

            // Cinematic camera usually does this on *most* speeds and is accurate for the most part.
            if (positivesYaw > negativesYaw || positivesPitch > negativesPitch) {
                this.lastSmooth = now;
            }

            yawSamples.clear();
            pitchSamples.clear();
        }

        rotationUpdate.setCinematic(cinematic);

        this.lastDeltaYaw = deltaYaw;
        this.lastDeltaPitch = deltaPitch;
    }
}
