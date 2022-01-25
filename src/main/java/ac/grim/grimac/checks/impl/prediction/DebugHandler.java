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

    @Override
    public void onPredictionComplete(final PredictionComplete predictionComplete) {
        double offset = predictionComplete.getOffset();

        // No one is listening to this debug
        if (listeners.isEmpty() && !outputToConsole) return;

        ChatColor color = pickColor(offset);

        Vector predicted = player.predictedVelocity.vector;
        Vector actually = player.actualMovement;

        ChatColor xColor = pickColor(Math.abs(predicted.getX() - actually.getX()));
        ChatColor yColor = pickColor(Math.abs(predicted.getY() - actually.getY()));
        ChatColor zColor = pickColor(Math.abs(predicted.getZ() - actually.getZ()));

        String p = color + "P: " + xColor + predicted.getX() + " " + yColor + predicted.getY() + " " + zColor + predicted.getZ();
        String a = color + "A: " + xColor + actually.getX() + " " + yColor + actually.getY() + " " + zColor + actually.getZ();
        String canSkipTick = (player.couldSkipTick + " ").substring(0, 1);
        String actualMovementSkip = (player.skippedTickInActualMovement + " ").substring(0, 1);
        String o = ChatColor.GRAY + "" + canSkipTick + "→0.03→" + actualMovementSkip + color + " O: " + offset;

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
            LogUtil.info(prefix + player.vehicleData.lastVehicleSwitch);
        }
    }

    private ChatColor pickColor(double offset) {
        if (offset <= 0) {
            return ChatColor.GRAY;
        } else if (offset < 0.0001) {
            return ChatColor.GREEN;
        } else if (offset < 0.01) {
            return ChatColor.YELLOW;
        } else {
            return ChatColor.RED;
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
