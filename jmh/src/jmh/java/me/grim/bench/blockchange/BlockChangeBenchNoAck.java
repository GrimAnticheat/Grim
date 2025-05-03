package me.grim.bench.blockchange;

import me.grim.bench.util.HeapSize;
import org.openjdk.jmh.annotations.*;

import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(value = 0, jvmArgsAppend = {"-Xms2G","-Xmx2G"})
public class BlockChangeBenchNoAck extends AbstractBlockChangeBenchmark {

    /* override the hook → never clean */
    @Override
    protected void processAcks(ThreadState s, Blackhole bh) {
        s.sinceAck++;
        if (ackDelayEvents==0) return;           // nothing to do
        if (s.sinceAck < ackDelayEvents &&
                s.idx != s.wlRef.size()) return;

        int id = s.player.lastTransactionSent.get();
        if (id > s.lastAck) {
//            s.latencyUtils.handleNettySyncTransaction(id);
            bh.consume(id);
            s.lastAck = id;
        }
        s.sinceAck = 0;
    }

    @Benchmark
    @Group("mem")                    // separate group → runs after “cpu”
    public long originalMem(ThreadState s, Blackhole bh) {
        s.pickOriginal();
        run(s, bh);
        System.out.println(HeapSize.detail(s.latencyUtils.getTasksObject()));
        return HeapSize.bytes(s.latencyUtils.getTasksObject());
    }

    @Benchmark
    @Group("mem")
    public long lowMem(ThreadState s, Blackhole bh) {
        s.pickLow();
        run(s, bh);
        System.out.println(HeapSize.detail(s.latencyUtils.getTasksObject()));
        return HeapSize.bytes(s.latencyUtils.getTasksObject());
    }
}