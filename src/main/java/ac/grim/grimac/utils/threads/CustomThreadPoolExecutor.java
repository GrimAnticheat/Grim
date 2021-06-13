package ac.grim.grimac.utils.threads;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class CustomThreadPoolExecutor extends ThreadPoolExecutor {
    //HashMap<Runnable, Long> predictionTime = new HashMap<>();

    public CustomThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
    }

    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        //predictionTime.put(r, System.nanoTime());
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        //long nanoTime = System.nanoTime() - predictionTime.remove(r);

        // Safe value to remove when the check was ran before it was ready to
        //if (nanoTime > 200000)
        //Bukkit.broadcastMessage("Time to check player (nanos): " + nanoTime);

        if (t != null) {
            t.printStackTrace();
        }
    }
}
