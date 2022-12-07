package ac.grim.grimac.checks.impl.prediction;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.LogUtil;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import ac.grim.grimac.utils.lists.EvictingQueue;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@CheckData(name = "Prediction (Debug)")
public class DebugHandler extends Check implements PostPredictionCheck {

    Set<Player> listeners = new CopyOnWriteArraySet<>(new HashSet<>());
    boolean outputToConsole = false;

    boolean enabledFlags = false;
    boolean lastMovementIsFlag = false;

    EvictingQueue<String> predicted = new EvictingQueue<>(5);
    EvictingQueue<String> actually = new EvictingQueue<>(5);
    EvictingQueue<String> offset = new EvictingQueue<>(5);

    public DebugHandler(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPredictionComplete(final PredictionComplete predictionComplete) {
        if (!predictionComplete.isChecked()) return;

        double offset = predictionComplete.getOffset();

        // No one is listening to this debug
        if (listeners.isEmpty() && !outputToConsole) return;
        // This is pointless debug!
        if (player.predictedVelocity.vector.lengthSquared() == 0 && offset == 0) return;

        ChatColor color = pickColor(offset, offset);

        Vector predicted = player.predictedVelocity.vector;
        Vector actually = player.actualMovement;

        ChatColor xColor = pickColor(Math.abs(predicted.getX() - actually.getX()), offset);
        ChatColor yColor = pickColor(Math.abs(predicted.getY() - actually.getY()), offset);
        ChatColor zColor = pickColor(Math.abs(predicted.getZ() - actually.getZ()), offset);

        String p = color + "P: " + xColor + predicted.getX() + " " + yColor + predicted.getY() + " " + zColor + predicted.getZ();
        String a = color + "A: " + xColor + actually.getX() + " " + yColor + actually.getY() + " " + zColor + actually.getZ();
        String canSkipTick = (player.couldSkipTick + " ").substring(0, 1);
        String actualMovementSkip = (player.skippedTickInActualMovement + " ").substring(0, 1);
        String o = ChatColor.GRAY + "" + canSkipTick + "→0.03→" + actualMovementSkip + color + " O: " + offset;

        String prefix = player.bukkitPlayer == null ? "null" : player.bukkitPlayer.getName() + " ";

        boolean thisFlag = color != ChatColor.GRAY && color != ChatColor.GREEN;
        if (enabledFlags) {
            // If the last movement was a flag, don't duplicate messages to the player
            if (lastMovementIsFlag) {
                this.predicted.clear();
                this.actually.clear();
                this.offset.clear();
            }
            // Even if last was a flag, we must send the new message if the player flagged
            this.predicted.add(p);
            this.actually.add(a);
            this.offset.add(o);

            lastMovementIsFlag = thisFlag;
        }

        if (thisFlag) {
            for (int i = 0; i < this.predicted.size(); i++) {
                player.user.sendMessage(this.predicted.get(i));
                player.user.sendMessage(this.actually.get(i));
                player.user.sendMessage(this.offset.get(i));
            }
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
