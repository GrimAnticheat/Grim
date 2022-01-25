package ac.grim.grimac.utils.latency;

import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;

import java.util.ArrayList;
import java.util.List;

public class CompensatedFireworks extends PostPredictionCheck {
    // As this is sync to one player, this does not have to be concurrent
    List<Integer> activeFireworks = new ArrayList<>();
    List<Integer> fireworksToRemoveNextTick = new ArrayList<>();

    GrimPlayer player;

    public CompensatedFireworks(GrimPlayer player) {
        super(player);
        this.player = player;
    }

    @Override
    public void onPredictionComplete(final PredictionComplete predictionComplete) {
        // Remove all the fireworks that were removed in the last tick
        // Remember to remove with an int not an Integer
        for (int i : fireworksToRemoveNextTick) {
            activeFireworks.remove(i);
        }
        fireworksToRemoveNextTick.clear();
    }

    public void addNewFirework(int entityID) {
        activeFireworks.add(entityID);
    }

    public void removeFirework(int entityID) {
        fireworksToRemoveNextTick.add(entityID);
    }

    public int getMaxFireworksAppliedPossible() {
        return activeFireworks.size();
    }
}
