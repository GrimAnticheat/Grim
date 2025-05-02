package me.grim.bench.blockchange;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.latency.ILatencyUtils;
import be.seeseemelk.mockbukkit.MockBukkit;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.util.EventCreationUtil;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerMultiBlockChange;
import io.netty.buffer.ByteBuf;
import me.grim.bench.blockchange.original.OriginalBlockChangeHandler;
import me.grim.bench.blockchange.original.OriginalLatencyUtils;
import me.grim.bench.blockchange.original_low_hanging_fruit.LowHangingFruitBlockChangeHandler;
import me.grim.bench.blockchange.original_low_hanging_fruit.LowHangingFruitLatencyUtils;
import me.grim.bench.setup.MockFactory;
import me.grim.bench.setup.TestPacketEventsBuilder;
import me.grim.bench.util.HeapSize;
import org.bukkit.plugin.Plugin;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(value = 1, jvmArgsAppend = {"-Xms2G", "-Xmx2G"})
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
public class ReusingWrapperBenchmarkFixed {

    // ────────────────── Work-load description ──────────────────
    enum WorkloadType { SINGLE_BLOCK, MULTI_BLOCK, OTHER_TASK }
    record WorkloadItem(WorkloadType type, int multiBlockCount) {}

    // ──────────────────── Benchmark parameters ─────────────────
    @Param({"1000"})  private int    totalEventsPerOp;
    @Param({"100"})   private int    blocksPerMultiChange;
    @Param({"0.6"})   private double multiBlockProbability;
    @Param({"0.1"})   private double singleBlockProbability;
    @Param({"50"})    private int    ackDelayEvents;

    // ───────────────────── Shared work-load ────────────────────
    private List<WorkloadItem> sharedWorkload;

    @Setup(Level.Trial)
    public synchronized void setupTrial() {
        try {
            if (!MockBukkit.isMocked()) MockBukkit.mock();
            Plugin plugin = MockBukkit.createMockPlugin("packetevents_global");
            PacketEvents.setAPI(TestPacketEventsBuilder.build(plugin));
            com.github.retrooper.packetevents.protocol.packettype.PacketType.prepare(); // Prevents race condition from printing graph while running
            new WrapperPlayServerBlockChange(new Vector3i(0,0,0), 0);
        } catch (Exception e) {
            throw new RuntimeException("Failed PacketEvents setup", e);
        }

        if (sharedWorkload != null) return;

        sharedWorkload = new ArrayList<>(totalEventsPerOp);
        Random r = new Random(4321);
        for (int i = 0; i < totalEventsPerOp; i++) {
            double v = r.nextDouble();
            WorkloadType t = v < multiBlockProbability ? WorkloadType.MULTI_BLOCK
                    : (v < multiBlockProbability + singleBlockProbability ? WorkloadType.SINGLE_BLOCK
                    : WorkloadType.OTHER_TASK);
            sharedWorkload.add(new WorkloadItem(t, blocksPerMultiChange));
        }
        Collections.shuffle(sharedWorkload, r);
    }

    // ─────────────────────── Thread state ──────────────────────
    @State(Scope.Thread)
    public static class ThreadState {

        GrimPlayer                                        player;
        ILatencyUtils                                     latencyUtils;
        AbstractBenchmarkBlockChangeHandler               blockChangeHandler;

        // Reusable wrappers for *building* ByteBufs
        WrapperPlayServerBlockChange                      singleBlockWrapper;
        WrapperPlayServerMultiBlockChange                 multiBlockWrapper;
        WrapperPlayServerMultiBlockChange.EncodedBlock[]  reusableMultiBlockArray;

        // Pre-generated buffers (one per work-item)
        ByteBuf[]                                         preBufs;

        // Position / ACK tracking
        int workloadIndex;
        int eventsSinceLastAck;
        int lastAckedTransactionId;

        List<WorkloadItem> sharedWorkloadRef;

        private static final Runnable OTHER_TASK_RUNNABLE = () -> Blackhole.consumeCPU(10);

        @TearDown(Level.Iteration)
        public void clean() {
            if (((ByteBuf) singleBlockWrapper.buffer).refCnt() > 0)
                ((ByteBuf) singleBlockWrapper.buffer).release();
            if (((ByteBuf) multiBlockWrapper.buffer ).refCnt() > 0)
                ((ByteBuf) multiBlockWrapper.buffer ).release();
            for (ByteBuf b : preBufs)
                if (b != null && b.refCnt() > 0) b.release();
        }

        // ─────────────── per-iteration pre-computation ───────────────
        @Setup(Level.Iteration)
        public void setupIterationBase(ReusingWrapperBenchmarkFixed bench) {
            player             = MockFactory.newMockPlayer();
            sharedWorkloadRef  = bench.sharedWorkload;

            Object channel = player.user.getChannel();

            // --- create wrappers once ---
            singleBlockWrapper = new WrapperPlayServerBlockChange(new Vector3i(0,0,0), 0);
            singleBlockWrapper.prepareForSend(channel,false,false);

            reusableMultiBlockArray = new WrapperPlayServerMultiBlockChange.EncodedBlock[bench.blocksPerMultiChange];
            for (int i=0;i<reusableMultiBlockArray.length;i++)
                reusableMultiBlockArray[i] = new WrapperPlayServerMultiBlockChange.EncodedBlock(0,0,0,0);

            multiBlockWrapper = new WrapperPlayServerMultiBlockChange(new Vector3i(0,0,0),null,reusableMultiBlockArray);
            multiBlockWrapper.prepareForSend(channel,false,false);

            // --- pre-compute all buffers ---
            preBufs = new ByteBuf[sharedWorkloadRef.size()];
            Random rnd = new Random(System.identityHashCode(this)+777);

            for (int i=0;i<sharedWorkloadRef.size();i++) {
                WorkloadItem wi = sharedWorkloadRef.get(i);
                switch (wi.type()) {

                    case SINGLE_BLOCK -> {
                        Vector3i pos = generateNearbyPosition(player,rnd);
                        int id       = rnd.nextInt(500)+1;
                        singleBlockWrapper.setBlockPosition(pos);
                        singleBlockWrapper.setBlockID(id);
                        singleBlockWrapper.write();
                        preBufs[i] = ((ByteBuf)singleBlockWrapper.buffer).duplicate().retain();
                    }

                    case MULTI_BLOCK -> {
                        Vector3i chunkPos = new Vector3i((int)player.x>>4,(int)player.y>>4,(int)player.z>>4);
                        multiBlockWrapper.setChunkPosition(chunkPos);

                        for (int j=0;j<wi.multiBlockCount();j++) {
                            Vector3i bp = generateNearbyPosition(player,rnd);
                            int bid     = rnd.nextInt(500)+1;
                            reusableMultiBlockArray[j].setX(bp.getX());
                            reusableMultiBlockArray[j].setY(bp.getY());
                            reusableMultiBlockArray[j].setZ(bp.getZ());
                            reusableMultiBlockArray[j].setBlockId(bid);
                        }
                        multiBlockWrapper.write();
                        preBufs[i] = ((ByteBuf)multiBlockWrapper.buffer).duplicate().retain();
                    }

                    case OTHER_TASK -> preBufs[i] = null; // nothing to buffer
                }
            }
            resetCounters();
        }

        @TearDown(Level.Iteration)
        public void releasePrecomputed() {
            for (ByteBuf b : preBufs)
                if (b != null && b.refCnt() > 0) b.release();
        }

        // ---- helpers ----------
        private void resetCounters() {
            workloadIndex = 0;
            eventsSinceLastAck = 0;
            lastAckedTransactionId = -1;
        }

        private static Vector3i generateNearbyPosition(GrimPlayer p, Random r) {
            int dx = r.nextInt(32)-16, dy = r.nextInt(32)-16, dz = r.nextInt(32)-16;
            return new Vector3i((int)p.x+dx, Math.max(0,(int)p.y+dy), (int)p.z+dz);
        }

        // ---------- handler / latency utils selection ----------
        void useOriginal() {
            latencyUtils      = new OriginalLatencyUtils(player);
            blockChangeHandler= new OriginalBlockChangeHandler();
            initLatency();
        }
        void useLowHanging() {
            latencyUtils      = new LowHangingFruitLatencyUtils(player);
            blockChangeHandler= new LowHangingFruitBlockChangeHandler();
            initLatency();
        }
        private void initLatency() {
            MockFactory.set(player,"latencyUtils", latencyUtils);
            latencyUtils.clear();
            player.lastTransactionSent.set(0);
            player.lastTransactionReceived.set(-1);
            player.lastTransSent = 0L;
            resetCounters();
        }
    }

    // ───────────────────── Benchmark methods ─────────────────────
    @Benchmark
    @Group("cpu")                    // ← keep your existing CPU test
    public void originalCpu(ThreadState s, Blackhole bh) {
        s.useOriginal();
        runWorkloadLoop(s, bh);
    }

    @Benchmark
    @Group("cpu")
    public void lowCpu(ThreadState s, Blackhole bh) {
        s.useLowHanging();
        runWorkloadLoop(s, bh);
    }

    /* ─────  MEMORY-SIZE ONLY  ───── */
    @Benchmark
    @Group("mem")                    // separate group → runs after “cpu”
    public long originalMem(ThreadState s, Blackhole bh) {
        s.useOriginal();
        runWorkloadLoop(s, bh);
        System.out.println(HeapSize.detail(s.latencyUtils.getTasksObject()));
        return HeapSize.bytes(s.latencyUtils.getTasksObject());
    }

    @Benchmark
    @Group("mem")
    public long lowMem(ThreadState s, Blackhole bh) {
        s.useLowHanging();
        runWorkloadLoop(s, bh);
        System.out.println(HeapSize.detail(s.latencyUtils.getTasksObject()));
        return HeapSize.bytes(s.latencyUtils.getTasksObject());
    }

    // ─────────────────── core loop (unchanged) ───────────────────
    private void runWorkloadLoop(ThreadState s, Blackhole bh) {
        for (int i=0;i<totalEventsPerOp;i++) {
            if (s.workloadIndex >= s.sharedWorkloadRef.size()) s.workloadIndex = 0;
            WorkloadItem item = s.sharedWorkloadRef.get(s.workloadIndex);
            ByteBuf      buf  = s.preBufs[s.workloadIndex];
            s.workloadIndex++;

            if (item.type() == WorkloadType.OTHER_TASK) {
                s.latencyUtils.addRealTimeTask(s.player.lastTransactionSent.get(),
                        () -> Blackhole.consumeCPU(20));
            } else {
                // packet items: build event around a retained duplicate
                ByteBuf dup = buf.retainedDuplicate();
                try {
                    PacketSendEvent pse = EventCreationUtil.createSendEvent(
                            s.player.user.getChannel(),
                            s.player.user,
                            s.player,
                            dup,
                            false);
                    s.blockChangeHandler.onPacketSend(pse);
                    bh.consume(pse);
                } finally {
                    if (dup.refCnt()>0) dup.release();
                }
            }
            processAcksIfNeeded(s,bh);
        }
    }

    // ───────────── ACK logic (identical to original) ─────────────
    private void processAcksIfNeeded(ThreadState s, Blackhole bh) {
        s.eventsSinceLastAck++;
        if (s.eventsSinceLastAck >= ackDelayEvents ||
                s.workloadIndex == s.sharedWorkloadRef.size()) {

            int currentSentId = s.player.lastTransactionSent.get();
            if (currentSentId > s.lastAckedTransactionId) {
                s.latencyUtils.handleNettySyncTransaction(currentSentId);
                bh.consume(currentSentId);
                s.lastAckedTransactionId = currentSentId;
            }
            s.eventsSinceLastAck = 0;
        }
    }
}