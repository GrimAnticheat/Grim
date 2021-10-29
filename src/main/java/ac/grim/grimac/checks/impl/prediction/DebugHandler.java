package ac.grim.grimac.checks.impl.prediction;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.LogUtil;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@CheckData(name = "Prediction (Debug)", buffer = 0)
public class DebugHandler extends PostPredictionCheck {

    List<Player> listeners = Collections.synchronizedList(new ArrayList<>());
    boolean outputToConsole = false;

    public DebugHandler(GrimPlayer player) {
        super(player);
    }

    public void onPredictionComplete(final PredictionComplete predictionComplete) {
        double offset = predictionComplete.getOffset();

        // No one is listening to this debug
        if (listeners.isEmpty() && !outputToConsole) return;

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
        String o = color + "O: " + offset + " " + player.couldSkipTick + " " + player.onGround + " " + player.speed;

        String prefix = getPlayer().bukkitPlayer.getName() + " ";

        for (Player player : listeners) {
            // Don't add prefix if the player is listening to oneself
            player.sendMessage((player == getPlayer().bukkitPlayer ? "" : prefix) + p);
            player.sendMessage((player == getPlayer().bukkitPlayer ? "" : prefix) + a);
            player.sendMessage((player == getPlayer().bukkitPlayer ? "" : prefix) + o);
        }

        // Don't memory leak player references
        listeners.removeIf(player -> !player.isOnline());

        if (outputToConsole) {
            LogUtil.info(prefix + p);
            LogUtil.info(prefix + a);
            LogUtil.info(prefix + o);
        }
    }

    public void toggleListener(Player player) {
        // Toggle, if already added, remove.  If not added, then add
        if (!listeners.remove(player)) listeners.add(player);
    }

    public boolean toggleConsoleOutput() {
        this.outputToConsole = !outputToConsole;
        return this.outputToConsole;
    }
}
