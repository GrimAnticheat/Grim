package ac.grim.legacyac.check.impl;

import ac.grim.legacyac.LegacyAntiCheatPlugin;
import ac.grim.legacyac.check.Check;
import ac.grim.legacyac.data.PlayerData;
import ac.grim.legacyac.network.frame.MovementFrame;
import java.util.Locale;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.util.Vector;

public final class VelocityCheck extends Check {
    public VelocityCheck(LegacyAntiCheatPlugin plugin) {
        super(plugin, "Velocity");
    }

    public void onVelocity(PlayerVelocityEvent event, PlayerData data) {
        if (!isEnabled()) {
            return;
        }

        Vector velocity = event.getVelocity();
        if (velocity == null) {
            return;
        }

        double xz = Math.sqrt(velocity.getX() * velocity.getX() + velocity.getZ() * velocity.getZ());
        double minExpectedXZ = plugin.getConfig().getDouble("checks.Velocity.min-expected-xz", 0.08D);
        if (xz < minExpectedXZ && Math.abs(velocity.getY()) < 0.05D) {
            return;
        }

        // Legacy fallback path compatibility; new logic uses tx-confirmed VelocitySample.
        int ticks = plugin.getConfig().getInt("checks.Velocity.window-ticks", 8);
        data.armVelocityWindow(velocity, ticks);
    }

    public void onMove(PlayerMoveEvent event, PlayerData data) {
        if (event.getTo() == null) {
            return;
        }
        MovementFrame frame = new MovementFrame(System.nanoTime(), event.getTo().getX(), event.getTo().getY(), event.getTo().getZ(), event.getTo().getYaw(), event.getTo().getPitch(), event.getPlayer().isOnGround(), true, true, MovementFrame.Source.BUKKIT_MOVE_EVENT);
        onMovementFrame(event.getPlayer(), frame, data);
    }

    public void onMovementFrame(Player player, MovementFrame frame, PlayerData data) {
        if (!isEnabled() || isExempt(player, data)) {
            return;
        }

        PlayerData.VelocitySample sample = data.getCurrentVelocitySample();
        if (sample == null) {
            return;
        }

        double expectedXZ = Math.sqrt(sample.getVx() * sample.getVx() + sample.getVz() * sample.getVz());
        double expectedY = Math.abs(sample.getVy());
        double observedXZ = data.getLastDeltaXZ();
        double observedY = Math.abs(data.getLastDeltaY());
        double offset = Math.abs(observedXZ - expectedXZ) + Math.abs(observedY - expectedY);

        if (sample.hasFlag(PlayerData.VelocitySample.FLAG_PRE_ACK) && !sample.hasFlag(PlayerData.VelocitySample.FLAG_POST_ACK)) {
            sample.observeTick(offset);
            double responseThreshold = Math.max(0.03D, expectedXZ * 0.20D);
            int delayedKbTicks = plugin.getConfig().getInt("checks.Velocity.delayed-kb-ticks",
                plugin.getConfig().getInt("checks.Velocity.window-ticks", 3));
            sample.recordObservedMotion(observedXZ, responseThreshold, delayedKbTicks);
        }

        double minScoreToFlag = plugin.getConfig().getDouble("checks.Velocity.min-score-to-flag",
            plugin.getConfig().getDouble("checks.Velocity.buffer", 1.2D));
        int minSamples = plugin.getConfig().getInt("checks.Velocity.min-samples", 2);

        if (sample.hasFlag(PlayerData.VelocitySample.FLAG_FIRST_CONFIRMED) && sample.getTicksObserved() == 1) {
            double firstStageScore = Math.max(0.0D, sample.getMinOffset() - 0.03D);
            if (firstStageScore > 0.0D) {
                slideAndAddScore(data, firstStageScore, 0.7D);
            }
        }

        if (!sample.hasFlag(PlayerData.VelocitySample.FLAG_LIKELY_CONFIRMED)) {
            return;
        }

        if (sample.getTicksObserved() < minSamples) {
            return;
        }

        double likelyStageScore = Math.max(0.0D, sample.getMinOffset() - 0.025D);
        double buffer = slideAndAddScore(data, likelyStageScore, plugin.getConfig().getDouble("checks.Velocity.window-weight", 1.0D));

        if (sample.hasFlag(PlayerData.VelocitySample.FLAG_DELAYED_KB_PATTERN)) {
            buffer = slideAndAddScore(data, 0.35D, 1.0D);
            if (data.isDebugEnabled()) {
                plugin.getLogger().info("[GLAC-DEBUG] " + player.getName()
                    + " Velocity DELAYED_KB_PATTERN silentTicks=" + sample.getInitialSilentTicks()
                    + " maxObsXZ=" + fmt(sample.getMaxObservedHorizontal()));
            }
        }

        if (data.isDebugEnabled()) {
            plugin.getLogger().info("[GLAC-DEBUG] " + player.getName()
                + " Velocity txWindow pre=" + sample.getPreTxId()
                + " post=" + sample.getPostTxId()
                + " ticks=" + sample.getTicksObserved()
                + " minOffset=" + fmt(sample.getMinOffset())
                + " score=" + fmt(likelyStageScore)
                + " flags=" + sample.getStateFlags());
        }

        if (buffer > minScoreToFlag) {
            flag(player, data, likelyStageScore,
                "preTx=" + sample.getPreTxId()
                    + " postTx=" + sample.getPostTxId()
                    + " minOffset=" + fmt(sample.getMinOffset())
                    + " ticks=" + sample.getTicksObserved()
                    + " flags=" + sample.getStateFlags());
        }
    }

    private String fmt(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }
}
