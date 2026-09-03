package ac.grim.grimac.checks.impl.movement;

import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PostPredictionListener;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import org.jetbrains.annotations.NotNull;

@CheckData(name = "NoSlow", stableKey = "grim.movement.noslow", description = "Was not slowed while using an item", setback = 5)
public class NoSlow extends Check implements PostPredictionListener {
    // The player sends that they switched items the next tick if they switch from an item that can be used
    // to another item that can be used.  What the fuck Mojang.  Affects 1.8 (and most likely 1.7) clients.
    public boolean didSlotChangeLastTick = false;
    public int flaggedMainHandSlotLastTick = -1;
    private boolean needFlag;
    private double offsetToFlag;
    private double bestOffset = 1;

    public NoSlow(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPredictionComplete(final PredictionComplete predictionComplete) {
        if (needFlag && flaggedMainHandSlotLastTick == player.packetStateData.lastSlotSelected
                && player.packetStateData.isSlowedByUsingItem() && !didSlotChangeLastTick) {
            flagWithSetback();
        }

        needFlag = false;

        int flaggedMainHandSlotThisTick = -1;
        // If the player was using an item for certain, and their predicted velocity had a flipped item
        if (predictionComplete.isChecked() && player.packetStateData.isSlowedByUsingItem()) {
            if (bestOffset > offsetToFlag) {
                int slot = player.packetStateData.lastSlotSelected;
                if (player.packetStateData.itemInUseHand == InteractionHand.OFF_HAND) {
                    flagWithSetback();
                } else if (player.packetStateData.getSlowedByUsingItemSlot() == player.packetStateData.lastSlotSelected) {
                    flaggedMainHandSlotThisTick = slot;
                    if (flaggedMainHandSlotLastTick == flaggedMainHandSlotThisTick) {
                        flagWithSetback();
                    } else {
                        needFlag = true;
                    }
                }
            } else {
                reward();
            }
        }

        flaggedMainHandSlotLastTick = flaggedMainHandSlotThisTick;
        bestOffset = 1;
        didSlotChangeLastTick = false;
    }

    public void handlePredictionAnalysis(double offset) {
        bestOffset = Math.min(bestOffset, offset);
    }

    @Override
    public void onReload(@NotNull ConfigManager config) {
        offsetToFlag = config.getDoubleElse(getConfigName() + ".threshold", 0.001);
    }
}
