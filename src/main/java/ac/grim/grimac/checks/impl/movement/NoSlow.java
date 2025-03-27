package ac.grim.grimac.checks.impl.movement;

import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;

@CheckData(name = "NoSlow", description = "Was not slowed while using an item", setback = 5)
public class NoSlow extends Check implements PostPredictionCheck {
    double offsetToFlag;
    double bestOffset = 1;
    // The player sends that they switched items the next tick if they switch from an item that can be used
    // to another item that can be used.  What the fuck mojang.  Affects 1.8 (and most likely 1.7) clients.
    public boolean didSlotChangeLastTick = false;
    public boolean flaggedLastTick = false;
    // Counter to track consecutive ticks where item use state changed (to detect patterns)
    private int itemUseStateChangeTicks = 0;
    // Track if the user was using an item in the previous tick
    private boolean wasUsingItemLastTick = false;

    public NoSlow(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPredictionComplete(final PredictionComplete predictionComplete) {
        if (!predictionComplete.isChecked()) return;

        // Track item use state changes to detect patterns
        boolean isUsingItemNow = player.packetStateData.isSlowedByUsingItem();
        if (wasUsingItemLastTick != isUsingItemNow) {
            itemUseStateChangeTicks++;
        } else {
            itemUseStateChangeTicks = 0;
        }
        wasUsingItemLastTick = isUsingItemNow;

        // If the player was using an item for certain, and their predicted velocity had a flipped item
        if (player.packetStateData.isSlowedByUsingItem() || itemUseStateChangeTicks >= 3) {
            // 1.8 users are not slowed the first tick they use an item, strangely
            if (player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_8) && didSlotChangeLastTick) {
                didSlotChangeLastTick = false;
                flaggedLastTick = false;
            }

            // Flag even if the player isn't "using" an item now but has been rapidly toggling item use state
            // This catches the exploit pattern where players toggle item use every other tick
            if (bestOffset > offsetToFlag || (itemUseStateChangeTicks >= 3 && bestOffset > offsetToFlag * 1.5)) {
                if (flaggedLastTick) {
                    flagAndAlertWithSetback();
                }
                flaggedLastTick = true;
            } else {
                reward();
                flaggedLastTick = false;
            }
        }
        bestOffset = 1;
    }

    public void handlePredictionAnalysis(double offset) {
        bestOffset = Math.min(bestOffset, offset);
    }

    @Override
    public void onReload(ConfigManager config) {
        offsetToFlag = config.getDoubleElse(getConfigName() + ".threshold", 0.001);
    }
}
