package ac.grim.grimac.checks.impl.prediction;

import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.api.events.CompletePredictionEvent;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.CheckType;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import org.bukkit.Bukkit;

import java.util.concurrent.atomic.AtomicInteger;

@CheckData(name = "Simulation", decay = 0.02, checkType = CheckType.MOVEMENT)
public class OffsetHandler extends Check implements PostPredictionCheck {
    // Config
    double setbackDecayMultiplier;
    double threshold;
    double immediateSetbackThreshold;
    double maxAdvantage;
    double maxCeiling;
    double setbackViolationThreshold;

    // Current advantage gained
    double advantageGained = 0;

    private static final AtomicInteger flags = new AtomicInteger(0);

    public OffsetHandler(GrimPlayer player) {
        super(player);
    }

    public void onPredictionComplete(final PredictionComplete predictionComplete) {
        if (!predictionComplete.isChecked()) return;

        double offset = predictionComplete.getOffset();

        CompletePredictionEvent completePredictionEvent = new CompletePredictionEvent(player, this, "", offset);
        Bukkit.getPluginManager().callEvent(completePredictionEvent);

        if (completePredictionEvent.isCancelled()) return;

        // Short circuit out flag call
        if ((offset >= threshold || offset >= immediateSetbackThreshold) && flag()) {
            advantageGained += offset;

            boolean isSetback = (advantageGained >= maxAdvantage || offset >= immediateSetbackThreshold)
                                && !isNoSetbackPermission()
                                && violations >= setbackViolationThreshold;
            giveOffsetLenienceNextTick(offset);

            if (isSetback) {
                player.getSetbackTeleportUtil().executeViolationSetback();
            }

            violations++;

            synchronized (flags) {
                int flagId = (flags.get() & 255) + 1; // 1-256 as possible values

                String humanFormattedOffset;
                if (offset < 0.001) { // 1.129E-3
                    humanFormattedOffset = String.format("%.4E", offset);
                    // Squeeze out an extra digit here by E-03 to E-3
                    humanFormattedOffset = humanFormattedOffset.replace("E-0", "E-");
                } else {
                    // 0.00112945678 -> .001129
                    humanFormattedOffset = String.format("%6f", offset);
                    // I like the leading zero, but removing it lets us add another digit to the end
                    humanFormattedOffset = humanFormattedOffset.replace("0.", ".");
                }

                if (alert(humanFormattedOffset + " /gl " + flagId)) {
                    flags.incrementAndGet(); // This debug was sent somewhere
                    predictionComplete.setIdentifier(flagId);
                }
            }

            advantageGained = Math.min(advantageGained, maxCeiling);
        } else {
            advantageGained *= setbackDecayMultiplier;
        }

        removeOffsetLenience();
    }

    private void giveOffsetLenienceNextTick(double offset) {
        // Don't let players carry more than 1 offset into the next tick
        // (I was seeing cheats try to carry 1,000,000,000 offset into the next tick!)
        //
        // This value so that setting back with high ping doesn't allow players to gather high client velocity
        double minimizedOffset = Math.min(offset, 1);

        // Normalize offsets
        player.uncertaintyHandler.lastHorizontalOffset = minimizedOffset;
        player.uncertaintyHandler.lastVerticalOffset = minimizedOffset;
    }

    private void removeOffsetLenience() {
        player.uncertaintyHandler.lastHorizontalOffset = 0;
        player.uncertaintyHandler.lastVerticalOffset = 0;
    }

    @Override
    public void onReload(ConfigManager config) {
        setbackDecayMultiplier = config.getDoubleElse("Simulation.setback-decay-multiplier", 0.999);
        threshold = config.getDoubleElse("Simulation.threshold", 0.001);
        immediateSetbackThreshold = config.getDoubleElse("Simulation.immediate-setback-threshold", 0.1);
        maxAdvantage = config.getDoubleElse("Simulation.max-advantage", 1);
        maxCeiling = config.getDoubleElse("Simulation.max-ceiling", 4);
        setbackViolationThreshold = config.getDoubleElse("Simulation.setback-violation-threshold", 1);
        if (maxAdvantage == -1) maxAdvantage = Double.MAX_VALUE;
        if (immediateSetbackThreshold == -1) immediateSetbackThreshold = Double.MAX_VALUE;
    }

    public boolean doesOffsetFlag(double offset) {
        return offset >= threshold;
    }
}
