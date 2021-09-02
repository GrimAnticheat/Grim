package ac.grim.grimac.checks.impl.prediction;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.LogUtil;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import org.bukkit.ChatColor;
import org.bukkit.util.Vector;

@CheckData(name = "Prediction (Debug)", buffer = 0)
public class DebugHandler extends PostPredictionCheck {
    public DebugHandler(GrimPlayer player) {
        super(player);
    }

    public void onPredictionComplete(final PredictionComplete predictionComplete) {
        double offset = predictionComplete.getOffset();

        ChatColor color;
        if (offset <= 0) {
            color = ChatColor.GRAY;
        } else if (offset < 0.0001) {
            color = ChatColor.GREEN;
        } else if (offset < 0.01) {
            color = ChatColor.YELLOW;
        } else {
            color = ChatColor.RED;
        }

        Vector predicted = player.predictedVelocity.vector;
        Vector actually = player.actualMovement;

        String p = color + "P: " + predicted.getX() + " " + predicted.getY() + " " + predicted.getZ();
        String a = color + "A: " + actually.getX() + " " + actually.getY() + " " + actually.getZ();
        String o = color + "O: " + offset + " " + player.isSwimming;

        LogUtil.info(p);
        LogUtil.info(a);
        LogUtil.info(o);
    }
}
