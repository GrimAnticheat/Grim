package me.grim.bench.util;

import org.openjdk.jol.info.GraphLayout;

/**
 * Utility that measures the retained heap of ONE object graph.
 * No agents, no VM flags, safe to call from inside a benchmark.
 */
public final class HeapSize {

    /** bytes retained *by and beneath* the given instance */
    public static long bytes(Object root) {
        return GraphLayout.parseInstance(root).totalSize();
    }

    /** human-readable multi-line breakdown (optional) */
    public static String detail(Object root) {
        return GraphLayout.parseInstance(root).toFootprint();
    }
}