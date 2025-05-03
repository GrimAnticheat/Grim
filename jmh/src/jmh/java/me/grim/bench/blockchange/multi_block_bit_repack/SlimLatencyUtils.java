package me.grim.bench.blockchange.multi_block_bit_repack;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.LogUtil;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import ac.grim.grimac.utils.latency.CompensatedWorld;
import ac.grim.grimac.utils.latency.ILatencyUtils;

import java.util.ArrayList;

public final class SlimLatencyUtils implements ILatencyUtils {

    /* --------- task codes (fits in byte) --------------------------- */
    private static final byte T_BLOCK_UPDATE = 0;
    private static final byte T_RUNNABLE     = 1;

    /* --------- primitive ring-buffer ------------------------------- */
    private static final int   INITIAL_CAP = 256;

    private int[]   txnIds   = new int  [INITIAL_CAP];
    private byte[]  types    = new byte [INITIAL_CAP];
    private Object[]payloads = new Object[INITIAL_CAP];

    private int head = 0, tail = 0, mask = INITIAL_CAP - 1;

    /* --------- Aux list reused while draining ---------------------- */
    private final ArrayList<Runnable> runList = new ArrayList<>();

    /* --------- back-reference to player ---------------------------- */
    private final GrimPlayer player;
    public SlimLatencyUtils(GrimPlayer p) { this.player = p; }

    /* =============================================================== */
    /*  Public API                                                     */
    /* =============================================================== */

    @Override public void addRealTimeTask(int txnId, Runnable r) {
        enqueue(txnId, T_RUNNABLE, r);
    }

    public void addBlockUpdateTask(int txnId, BlockUpdateDescriptor d) {
        enqueue(txnId, T_BLOCK_UPDATE, d);
    }

    @Override public void addRealTimeTaskAsync(int txnId, Runnable r) {
        // unchanged – async path is rare, leave as original
        addRealTimeTask(txnId, r);
    }

    @Override public void handleNettySyncTransaction(int received) {

        runList.clear();

        while (head != tail) {
            int   idx   = head & mask;
            int   tid   = txnIds[idx];

            if (received + 1 < tid) break;        // too new
            if (received == tid - 1) {            // skip exactly +1
                head++; continue;
            }

            byte  type  = types[idx];
            Object obj  = payloads[idx];

            if (type == T_BLOCK_UPDATE) {
                ((BlockUpdateDescriptor) obj).apply(player.compensatedWorld);
            } else {
                runList.add((Runnable) obj);
            }
            head++;
        }

        /* run generic runnables outside of ring-buffer to release lock */
        for (Runnable r : runList) {
            try { r.run(); } catch (Exception ex) { handleErr(ex); }
        }
    }

    @Override public void clear() {
        head = tail = 0;   // cheap reset
        runList.clear();
    }

    @Override public Object getTasksObject() { return this; }

    /* =============================================================== */
    /*  Private helpers                                                */
    /* =============================================================== */

    private void enqueue(int txnId, byte type, Object payload) {
        if (player.lastTransactionReceived.get() >= txnId) {
            // execute immediately (sync path)
            if (type == T_BLOCK_UPDATE)
                ((BlockUpdateDescriptor) payload).apply(player.compensatedWorld);
            else
                ((Runnable) payload).run();
            return;
        }

        synchronized (this) {
            int nextTail = (tail + 1) & mask;
            if (nextTail == head) grow();             // full

            int idx = tail & mask;
            txnIds [idx] = txnId;
            types  [idx] = type;
            payloads[idx] = payload;
            tail = nextTail;
        }
    }

    private void grow() {                 // double capacity
        int newCap = txnIds.length << 1;
        int newMask = newCap - 1;

        int[]  newTxn = new int [newCap];
        byte[] newTyp = new byte[newCap];
        Object[] newPay = new Object[newCap];

        int sz = tail - head;
        for (int i = 0; i < sz; i++) {
            int oldIdx = (head + i) & mask;
            newTxn[i] = txnIds[oldIdx];
            newTyp[i] = types [oldIdx];
            newPay[i] = payloads[oldIdx];
        }
        head = 0; tail = sz; mask = newMask;
        txnIds = newTxn; types = newTyp; payloads = newPay;
    }

    private void handleErr(Exception e) {
        LogUtil.error("Task error for " + player.user.getName(), e);
        if (!Boolean.getBoolean("grim.disable-transaction-kick"))
            player.disconnect(MessageUtil.miniMessage(
                  MessageUtil.replacePlaceholders(player,
                      GrimAPI.INSTANCE.getConfigManager().getDisconnectPacketError())));
    }

    /* =============================================================== */
    /*  Descriptor for compact block update                            */
    /* =============================================================== */

    public interface BlockUpdateDescriptor {
        void apply(CompensatedWorld world);
    }
}