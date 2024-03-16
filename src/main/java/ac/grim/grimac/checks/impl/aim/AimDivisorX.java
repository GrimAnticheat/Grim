package ac.grim.grimac.checks.impl.aim;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.RotationCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.BetterStream;
import ac.grim.grimac.utils.anticheat.update.RotationUpdate;
import ac.grim.grimac.utils.lists.EvictingQueue;
import ac.grim.grimac.utils.math.GrimMath;

@CheckData(name = "AimDivisorX")
public class AimDivisorX extends Check implements RotationCheck {
    public AimDivisorX(GrimPlayer playerData) {
        super(playerData);
    }

    private EvictingQueue<Boolean> invalidDivisorList;
    private EvictingQueue<Double> rotationList;
    private double minAverageRot;
    private double maxInvalidRows;
    private double minRowLength;
    private int sampleSize;

    @Override
    public void process(final RotationUpdate rotationUpdate) {
        double divisorX = rotationUpdate.getProcessor().divisorX;
        double deltaX = rotationUpdate.getDeltaXRotABS();
        if(player.compensatedEntities.getSelf().getRiding() != null) {
            return; //Fix false positives in boats and other entities
        }
        if(!(Math.abs(rotationUpdate.getTo().getPitch()) < 90)) {
            return; //Ignore 90 and -90 pitch rotations
        }
        if(player.packetStateData.lastPacketWasTeleport) {
            return;
        }

        invalidDivisorList.add(divisorX < GrimMath.MINIMUM_DIVISOR);
        rotationList.add(deltaX);
        if(invalidDivisorList.size() >= sampleSize) {
            double averageRot = BetterStream.getAverageDouble(rotationList);
            if(getRowCount() > maxInvalidRows && averageRot > minAverageRot) {
                flagAndAlert("rows=" +getRowCount() + " avg=" + averageRot);
            }
        }


    }

    private int getRowCount() {

        int rowCount = 0;
        int currentTrueCount = 0;

        for (Boolean b : invalidDivisorList) {
            if (b) {
                currentTrueCount++;
            } else {
                if (currentTrueCount >= minRowLength) {
                    rowCount++;
                }
                currentTrueCount = 0; // Reset current count for next group
            }
        }


        if (currentTrueCount >= minRowLength) {
            rowCount++; //If the list ends with true check if the row is long enough
        }

        return rowCount;
    }



    @Override
    public void reload() {
        super.reload();
        sampleSize = getConfig().getIntElse(getConfigName() + ".sampleSize", 25);
        maxInvalidRows = getConfig().getDoubleElse(getConfigName() + ".maxInvalidRows", 2);  //How many independent rows are allowed in the sample size
        minRowLength = getConfig().getDoubleElse(getConfigName() + ".minRowLength", 3); //The minimum length
        minAverageRot = getConfig().getDoubleElse(getConfigName() + ".minAverageRot", 0.4D);
        invalidDivisorList = new EvictingQueue<>(sampleSize);
        rotationList = new EvictingQueue<>(sampleSize);
    }
}
