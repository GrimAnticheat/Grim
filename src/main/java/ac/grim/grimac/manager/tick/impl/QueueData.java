package ac.grim.grimac.manager.tick.impl;

import ac.grim.grimac.manager.tick.Tickable;
import ac.grim.grimac.predictionengine.MovementCheckRunner;
import ac.grim.grimac.utils.data.PredictionData;

public class QueueData implements Tickable {
    @Override
    public void tick() {
        while (true) {
            // Place tasks that were waiting on the server tick to "catch up" back into the queue
            PredictionData data = MovementCheckRunner.waitingOnServerQueue.poll();
            if (data == null) break;
            MovementCheckRunner.executor.runCheck(data);
        }
    }
}
