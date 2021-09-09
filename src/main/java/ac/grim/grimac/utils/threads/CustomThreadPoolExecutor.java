package ac.grim.grimac.utils.threads;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.predictionengine.MovementCheckRunner;
import ac.grim.grimac.utils.data.PredictionData;

import java.util.concurrent.*;

public class CustomThreadPoolExecutor extends ThreadPoolExecutor {
    private static double computeTime = 0;
    // Assume predictions take 1 millisecond (they should take 0.3 ms)
    private static double longComputeTime = 1e6;

    public CustomThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
    }

    public void runCheck(PredictionData data) {
        long startTime = System.nanoTime();
        CompletableFuture.runAsync(() -> data.player.movementCheckRunner.check(data), this).whenComplete((s, t) -> {
            if (!data.isCheckNotReady) {
                long timeTaken = System.nanoTime() - startTime;
                computeTime = (computeTime * 499 / 500d) + (timeTaken * (1 / 500d));
                longComputeTime = (computeTime * 2499 / 2500d) + (timeTaken * (1 / 2500d));
            }
            if (t != null) {
                t.printStackTrace();
            }

            if (!data.isCheckNotReady) {
                queueNext(data.player);
            } else {
                MovementCheckRunner.waitingOnServerQueue.add(data);
            }
        });
    }

    // If the last task was finished and there is another task to run -> run the next task
    // If the last task was finished and there are no more tasks -> let tasksNotFinished signal to immediately add to thread pool on new task
    // If the last task wasn't finished because the server hasn't ticked relevant packets -> add the prediction data back to the queue
    // If there is an exception, just queue the next data
    public void queueNext(GrimPlayer player) {
        if (player.tasksNotFinished.getAndDecrement() > 1) {
            PredictionData nextData;

            // Stop running checks if this player is offline
            if (!player.bukkitPlayer.isOnline()) return;

            // We KNOW that there is data in the queue
            // However the other thread increments this value BEFORE adding it to the LinkedQueue
            // Meaning it could increment the value, we read the queue, and it hasn't been added yet
            // So we have to loop until it's added
            do {
                nextData = player.queuedPredictions.poll();
            } while (nextData == null);

            PredictionData finalNextData = nextData;
            runCheck(finalNextData);
        }
    }

    public double getComputeTime() {
        return computeTime;
    }

    public double getLongComputeTime() {
        return longComputeTime;
    }

    @Override
    protected void beforeExecute(Thread t, Runnable r) {
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        if (t != null) {
            t.printStackTrace();
        }
    }
}
