package ac.grim.grimac.checks.impl.elytra;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import ac.grim.grimac.utils.nmsutil.Collisions;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.bukkit.util.Vector;

@CheckData(name = "ElytraJ")
public class ElytraJ extends Check implements PostPredictionCheck {

    public ElytraJ(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPredictionComplete(final PredictionComplete predictionComplete) {
        if (player.yRot > -30) {
            ObjectArrayList<SimpleCollisionBox> boxes = new ObjectArrayList<>(9);
            Collisions.getCollisionBoxes(player, player.boundingBox, boxes, false);

            if (isRising(player.predictedVelocity.vector, boxes)) {
                flagAndAlert();
                player.getSetbackTeleportUtil().executeNonSimulatingSetback();
            }
        }
    }

    private boolean isRising(Vector vector, ObjectArrayList<SimpleCollisionBox> boxes) {
        return boxes.stream().noneMatch(box -> box.isIntersected(player.boundingBox))
                && player.fireworks.getMaxFireworksAppliedPossible() > 0
                && vector.getY() > 1.6 && vector.getY() > vector.clone().setY(0).length();
    }
}
