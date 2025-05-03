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
import me.grim.bench.blockchange.AbstractBenchmarkBlockChangeHandler;
import me.grim.bench.blockchange.original.OriginalBlockChangeHandler;
import me.grim.bench.blockchange.original.OriginalLatencyUtils;
import me.grim.bench.blockchange.original_low_hanging_fruit.LowHangingFruitBlockChangeHandler;
import me.grim.bench.blockchange.original_low_hanging_fruit.LowHangingFruitLatencyUtils;
import me.grim.bench.setup.MockFactory;
import me.grim.bench.setup.TestPacketEventsBuilder;
import org.bukkit.plugin.Plugin;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 *  Abstract template for every Block-Change benchmark variant.
 *  Sub-classes only have to:
 *     ① add their own @Benchmark methods (CPU / mem / whatever)
 *     ② optionally override {@link #processAcks(ThreadState, Blackhole)}.
 */
@State(Scope.Benchmark)
public class AbstractBlockChangeBenchmark {

    /* ───────── original parameter section ───────── */
    @Param({"1000"})  protected int    totalEventsPerOp;
    @Param({"100"})   protected int    blocksPerMultiChange;
    @Param({"0.6"})   protected double multiBlockProbability;
    @Param({"0.1"})   protected double singleBlockProbability;
    @Param({"50"})    protected int    ackDelayEvents;

    /* ───────── workload description (unchanged) ─── */
    enum WorkloadType { SINGLE_BLOCK, MULTI_BLOCK, OTHER_TASK }
    record WorkloadItem(WorkloadType type,int multiCnt){}

    private List<WorkloadItem> sharedWorkload;

    /* ───────── Trial setup (unchanged) ──────────── */
    @Setup(Level.Trial)
    public synchronized void trial() {
        try {
            if (!MockBukkit.isMocked()) MockBukkit.mock();
            Plugin p = MockBukkit.createMockPlugin("pe_global");
            PacketEvents.setAPI(TestPacketEventsBuilder.build(p));
            com.github.retrooper.packetevents.protocol.packettype.PacketType.prepare();
        } catch (Exception e) { throw new RuntimeException(e); }

        if (sharedWorkload != null) return;
        sharedWorkload = new ArrayList<>(totalEventsPerOp);
        Random r = new Random(4321);
        for (int i=0;i<totalEventsPerOp;i++) {
            double v = r.nextDouble();
            WorkloadType t = v < multiBlockProbability ? WorkloadType.MULTI_BLOCK
                         : (v < multiBlockProbability+singleBlockProbability ? WorkloadType.SINGLE_BLOCK
                                                                             : WorkloadType.OTHER_TASK);
            sharedWorkload.add(new WorkloadItem(t, blocksPerMultiChange));
        }
        Collections.shuffle(sharedWorkload, r);
    }

    /* ───────────── ThreadState (mostly unchanged) ───────────── */
    @State(Scope.Thread)
    public static class ThreadState {

        GrimPlayer  player;
        ILatencyUtils latencyUtils;
        AbstractBenchmarkBlockChangeHandler handler;

        WrapperPlayServerBlockChange            single;
        WrapperPlayServerMultiBlockChange       multi;
        WrapperPlayServerMultiBlockChange.EncodedBlock[] arr;

        ByteBuf[] preBufs;

        int idx, sinceAck, lastAck;
        List<WorkloadItem> wlRef;

        @TearDown(Level.Iteration)
        public void tear() {
            if (single.buffer!=null) ((ByteBuf)single.buffer).release();
            if (multi.buffer !=null) ((ByteBuf)multi.buffer ).release();
            for (ByteBuf b : preBufs) if (b!=null && b.refCnt()>0) b.release();
            MockFactory.releaseMockPlayer(player);
        }

        @Setup(Level.Iteration)
        public void iter(AbstractBlockChangeBenchmark bench){
            wlRef = bench.sharedWorkload;
            player = MockFactory.newMockPlayer();
            MockFactory.addMockPlayer(player.user, player);

            Object ch = player.user.getChannel();
            single = new WrapperPlayServerBlockChange(new Vector3i(0,0,0),0);
            single.prepareForSend(ch,false,false);

            arr = new WrapperPlayServerMultiBlockChange.EncodedBlock[
                     bench.blocksPerMultiChange];
            for(int i=0;i<arr.length;i++)
                arr[i]=new WrapperPlayServerMultiBlockChange.EncodedBlock(0,0,0,0);
            multi = new WrapperPlayServerMultiBlockChange(new Vector3i(0,0,0),null,arr);
            multi.prepareForSend(ch,false,false);

            preBufs = new ByteBuf[wlRef.size()];
            Random rnd = new Random(System.identityHashCode(this));
            for (int i=0;i<wlRef.size();i++) {
                WorkloadItem wi = wlRef.get(i);
                switch (wi.type()) {
                    case SINGLE_BLOCK -> {
                        ByteBuf buffer = (ByteBuf) single.buffer;
                        int originalWriterIndex = buffer.writerIndex();
                        single.setBlockPosition(randPos(player,rnd));
                        single.setBlockID(rnd.nextInt(500)+1);
                        single.write();
                        buffer.writerIndex(originalWriterIndex);
                        preBufs[i]=buffer.retainedDuplicate();
                    }
                    case MULTI_BLOCK -> {
                        ByteBuf buffer = (ByteBuf) multi.buffer;
                        int originalWriterIndex = buffer.writerIndex();
                        Vector3i chunk = new Vector3i(((int)player.x)>>4,
                                                      ((int)player.y)>>4,
                                                      ((int)player.z)>>4);
                        multi.setChunkPosition(chunk);
                        for(int j=0;j<wi.multiCnt();j++){
                            Vector3i p = randPos(player,rnd);
                            arr[j].setX(p.getX()); arr[j].setY(p.getY());
                            arr[j].setZ(p.getZ());
                            arr[j].setBlockId(rnd.nextInt(500)+1);
                        }
                        multi.write();
                        buffer.writerIndex(originalWriterIndex);
                        preBufs[i]=buffer.retainedDuplicate();
                    }
                    case OTHER_TASK -> preBufs[i]=null;
                }
            }
            pickOriginal();     // default
        }

        /* tiny helpers */
        private static Vector3i randPos(GrimPlayer p, Random r){
            int dx=r.nextInt(32)-16, dy=r.nextInt(32)-16, dz=r.nextInt(32)-16;
            return new Vector3i((int)p.x+dx, Math.max(0,(int)p.y+dy), (int)p.z+dz);
        }

        /* choose util / handler for each @Benchmark call */
        void pickOriginal(){
            latencyUtils=new OriginalLatencyUtils(player);
            handler     =new OriginalBlockChangeHandler();
            MockFactory.set(player,"latencyUtils",latencyUtils);
            latencyUtils.clear();
            idx=sinceAck=lastAck=0;
        }
        void pickLow(){
            latencyUtils=new LowHangingFruitLatencyUtils(player);
            handler     =new LowHangingFruitBlockChangeHandler();
            MockFactory.set(player,"latencyUtils",latencyUtils);
            latencyUtils.clear();
            idx=sinceAck=lastAck=0;
        }
    }

    /* ─────────── the shared hot-loop (calls abstract hook) ─────────── */
    protected final void run(ThreadState s, Blackhole bh) {
        for (int n=0;n<totalEventsPerOp;n++) {
            if (s.idx==s.wlRef.size()) s.idx=0;
            WorkloadItem wi = s.wlRef.get(s.idx);
            ByteBuf buf     = s.preBufs[s.idx];
            s.idx++;

            if (wi.type()==WorkloadType.OTHER_TASK) {
                s.latencyUtils.addRealTimeTask(
                    s.player.lastTransactionSent.get(),
                    ()->Blackhole.consumeCPU(20));
            } else {
                ByteBuf dup = buf.retainedDuplicate();
                try {
                    PacketSendEvent ev = EventCreationUtil.createSendEvent(
                            s.player.user.getChannel(),
                            s.player.user,
                            s.player,
                            dup,false);
                    s.handler.onPacketSend(ev);
                    bh.consume(ev);
                } finally { dup.release(); }
            }
            processAcks(s,bh);          // ← hook below
        }
    }

    /* ---------- SUBCLASS HOOK ---------- */
    protected void processAcks(ThreadState s, Blackhole bh) {
        s.sinceAck++;
        if (ackDelayEvents==0) return;           // nothing to do
        if (s.sinceAck < ackDelayEvents &&
            s.idx != s.wlRef.size()) return;

        int id = s.player.lastTransactionSent.get();
        if (id > s.lastAck) {
            s.latencyUtils.handleNettySyncTransaction(id);
            bh.consume(id);
            s.lastAck = id;
        }
        s.sinceAck = 0;
    }
}