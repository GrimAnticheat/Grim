package me.grim.bench.blockchange;

import org.openjdk.jmh.annotations.*;

import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(value = 0, jvmArgsAppend = {"-Xms2G","-Xmx2G"})
public class BlockChangeBenchCPU extends me.grim.bench.blockchange.AbstractBlockChangeBenchmark {

    @Benchmark @Group("cpu")
    public void original(ThreadState s, Blackhole bh) {
        s.pickOriginal();
        run(s,bh);
    }

    @Benchmark @Group("cpu")
    public void low(ThreadState s, Blackhole bh) {
        s.pickLow();
        run(s,bh);
    }
    /* uses default processAcks → cleans queue every ackDelayEvents */
}