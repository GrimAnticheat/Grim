package ac.grim.grimac.manager.init.start;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class ConsoleOutputAppender extends AbstractAppender {
    protected ConsoleOutputAppender() {
        // 1.8 support - Let it create its own layout
        // 1.8 support - Don't specify properties and use deprecated method
        super("GrimAppender", null, null, false);
    }

    @Override
    public void append(LogEvent event) {
        // Vanilla anticheat logs on warn level
        if (event.getLevel() == Level.WARN) {
            String message = event.getMessage().getFormattedMessage();

            int movedTooQuickly = message.indexOf("moved too quickly!");
            if (movedTooQuickly != -1) {
                // We don't care about vehicles, we don't use those teleport packets.
                if (message.substring(0, movedTooQuickly).contains("vehicle of")) return;

                Player player = Bukkit.getPlayer(message.substring(0, movedTooQuickly - 1));
                handleVanillaAC(player);
            }

            int movedWrongly = message.indexOf("moved wrongly!");
            if (movedWrongly != -1) {
                // We don't care about vehicles, we don't use those teleport packets.
                if (message.substring(0, movedWrongly).contains("vehicle of")) return;

                Player player = Bukkit.getPlayer(message.substring(0, movedWrongly - 1));
                handleVanillaAC(player);
            }
        }
    }

    // This should be sync to the BUKKIT thread
    // as long as no stupid jar uses an async appender, which paper at one point did, but
    // it was reverted because it broke hacks like this.
    //
    // Hopefully no stupid MCM jar is dumb enough to re-enable the async appender because async = better
    private void handleVanillaAC(Player player) {
        if (player == null) return;

        GrimPlayer grimPlayer = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(player);
        if (grimPlayer == null) return;
        grimPlayer.wasVanillaAC = true;
    }
}
