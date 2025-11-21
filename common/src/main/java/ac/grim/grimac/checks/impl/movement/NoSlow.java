package ac.grim.grimac.checks.impl.movement;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;

@CheckData(name = "NoSlow", description = "Was not slowed while using an item", setback = 5)
public class NoSlow extends Check implements PostPredictionCheck {
    // The player sends that they switched items the next tick if they switch from an item that can be used
    // to another item that can be used.  What the fuck mojang.  Affects 1.8 (and most likely 1.7) clients.
    public boolean didSlotChangeLastTick = false;
    public boolean alternativeNoslowFix;
    public boolean flaggedLastTick = false;
    double offsetToFlag;
    double bestOffset = 1;
    private int buffer;

    public NoSlow(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPredictionComplete(final PredictionComplete predictionComplete) {
        if (!predictionComplete.isChecked()) return;

        // If the player was using an item for certain, and their predicted velocity had a flipped item
        if (alternativeNoslowFix) {
            if (player.packetStateData.isSlowedByUsingItem() && buffer++ > 1) {
                // 1.8 users are not slowed the first tick they use an item, strangely
                if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_8) && didSlotChangeLastTick) {
                    didSlotChangeLastTick = false;
                    flaggedLastTick = false;
                }

                if (bestOffset > offsetToFlag) {
                    if (flaggedLastTick) {
                        GrimAPI.INSTANCE.getItemResetHandler().resetItemUsage(player.platformPlayer);
                        if (buffer > 6) {
                            flagAndAlert("buffer=" + buffer);
                        }
                    }
                    flaggedLastTick = true;
                } else {
                    reward();
                    flaggedLastTick = true;
                    buffer = 0;
                }
            }
            bestOffset = 1;
        }
        else {
            if (player.packetStateData.isSlowedByUsingItem()) {
                // 1.8 users are not slowed the first tick they use an item, strangely
                if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_8) && didSlotChangeLastTick) {
                    didSlotChangeLastTick = false;
                    flaggedLastTick = false;
                }

                if (bestOffset > offsetToFlag) {
                    if (flaggedLastTick) {
                        flagAndAlert();
                        GrimAPI.INSTANCE.getItemResetHandler().resetItemUsage(player.platformPlayer);
                    }
                    flaggedLastTick = true;
                } else {
                    reward();
                    flaggedLastTick = false;
                }
            }
            bestOffset = 1;
        }
    }

    public void handlePredictionAnalysis(double offset) {
        bestOffset = Math.min(bestOffset, offset);
    }

    @Override
    public void onReload(ConfigManager config) {
        offsetToFlag = config.getDoubleElse(getConfigName() + ".threshold", 0.001);
        alternativeNoslowFix = config.getBooleanElse(getConfigName() + ".mitigateTicksNoslow", false);
    }
}
