package ac.grim.grimac.utils.latency;

public interface ILatencyUtils {
    /**
     * Adds a task to be executed when the corresponding transaction ACK is received.
     *
     * @param transaction The transaction ID this task is associated with.
     * @param runnable The task to execute.
     */
    void addRealTimeTask(int transaction, Runnable runnable);

    /**
     * Adds a task to be executed asynchronously via the player's event loop
     * when the corresponding transaction ACK is received.
     * (Note: Benchmark might simplify/ignore the async part unless specifically testing event loop contention)
     *
     * @param transaction The transaction ID this task is associated with.
     * @param runnable The task to execute.
     */
    void addRealTimeTaskAsync(int transaction, Runnable runnable);

    /**
     * Processes received transaction ACKs and runs associated tasks.
     *
     * @param receivedTransactionId The ID of the transaction ACK received from the client.
     */
    void handleNettySyncTransaction(int receivedTransactionId);
}
