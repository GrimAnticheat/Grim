package ac.grim.grimac.manager.tick.impl;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.manager.tick.Tickable;
import ac.grim.grimac.predictionengine.MovementCheckRunner;
import ac.grim.grimac.utils.math.GrimMath;

public class ThreadSetter implements Tickable {
    @Override
    public void tick() {
        // Scale every 10 seconds
        if (GrimAPI.INSTANCE.getTickManager().getTick() % 200 != 0) return;

        // Take samples over 2500 predictions to find how long they take - measured in nanoseconds
        // Multiply this by 20 as there are 20 predictions in a second
        // Multiply this again by the number of players that we not exempt
        double nano = MovementCheckRunner.executor.getLongComputeTime() * 20 * GrimAPI.INSTANCE.getPlayerDataManager().size();
        // Convert this into seconds
        double seconds = nano / 1e9;

        // Set number of threads the estimated usage + 30% for safety + rounded up
        int targetThreads = GrimMath.ceil(seconds * 1.3);
        if (targetThreads != 0 && MovementCheckRunner.executor.getPoolSize() != targetThreads) {
            MovementCheckRunner.executor.setMaximumPoolSize(targetThreads);
        }
    }
}
