package ac.grim.grimac.checks.impl.prediction;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.LogUtil;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import ac.grim.grimac.utils.lists.EvictingList;
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

    List<String> predicted = new EvictingList<>(5);
    List<String> actually = new EvictingList<>(5);
    List<String> offset = new EvictingList<>(5);

    public DebugHandler(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPredictionComplete(final PredictionComplete predictionComplete) {
        double offset = predictionComplete.getOffset();

        ChatColor color = pickColor(offset, offset);

        Vector predicted = player.predictedVelocity.vector;
        Vector actually = player.actualMovement;

        String p = "P: " + String.format("%.5f", predicted.getX()) + " " + String.format("%.5f", predicted.getY()) + " " + String.format("%.5f", predicted.getZ());
        String a = "A: " + String.format("%.5f", actually.getX()) + " " + String.format("%.5f", actually.getY()) + " " + String.format("%.5f", actually.getZ());
        String canSkipTick = (player.couldSkipTick + " ").substring(0, 1);
        String actualMovementSkip = (player.skippedTickInActualMovement + " ").substring(0, 1);
        String o = ChatColor.GRAY + "" + canSkipTick + "→0.03→" + actualMovementSkip + color + " O: " + offset;

        String prefix = player.bukkitPlayer == null ? "null" : player.bukkitPlayer.getName() + " ";

        boolean thisFlag = offset > 0.0001;

        // Even if last was a flag, we must send the new message if the player flagged
        this.predicted.add(p);
        this.actually.add(a);
        this.offset.add(o);

        if (thisFlag) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < this.predicted.size(); i++) {
                sb.append(this.predicted.get(i)).append("\n");
                sb.append(this.actually.get(i)).append("\n");
                sb.append(this.offset.get(i)).append("\n");
            }

            sb.append("\n\nGliding ");
            sb.append(player.isGliding);
            sb.append(" Swimming ");
            sb.append(player.isSwimming);
            sb.append(" Pose ");
            sb.append(player.pose);
            sb.append(" In vehicle ");
            sb.append(player.inVehicle);
            sb.append(" Fireworks ");
            sb.append(player.compensatedFireworks.getMaxFireworksAppliedPossible());
            sb.append(" Movement num ");
            sb.append(player.movementPackets);
            sb.append(" Player position ");
            sb.append(" X ");
            sb.append(player.x);
            sb.append(" Y ");
            sb.append(player.y);
            sb.append(" Z ");
            sb.append(player.z);

            GrimAPI.INSTANCE.getDiscordManager().sendAlert(player, "Alerts", "debug", sb.toString());
        }

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

    private ChatColor pickColor(double offset, double totalOffset) {
        if (player.getSetbackTeleportUtil().blockOffsets) return ChatColor.GRAY;
        if (offset <= 0 || totalOffset <= 0) { // If exempt don't bother coloring, so I stop getting false false reports
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
