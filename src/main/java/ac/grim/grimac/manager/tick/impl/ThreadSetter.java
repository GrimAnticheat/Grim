package ac.grim.grimac.manager.tick.impl;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.manager.tick.Tickable;
import ac.grim.grimac.predictionengine.MovementCheckRunner;
import org.bukkit.Bukkit;

public class ThreadSetter implements Tickable {
    @Override
    public void tick() {
        // Scale every 10 seconds
        if (GrimAPI.INSTANCE.getTickManager().getTick() % 200 != 0) return;

        // Set number of threads one per every 20 players, rounded up
        int targetThreads = (Bukkit.getOnlinePlayers().size() / 20) + 1;
        if (MovementCheckRunner.executor.getPoolSize() != targetThreads) {
            MovementCheckRunner.executor.setMaximumPoolSize(targetThreads);
        }
    }
}
