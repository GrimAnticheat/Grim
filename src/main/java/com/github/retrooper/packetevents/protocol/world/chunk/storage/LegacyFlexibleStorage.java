//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//
package com.github.retrooper.packetevents.protocol.world.chunk.storage;

import java.util.Arrays;

public class LegacyFlexibleStorage extends BaseStorage {
    private final long[] data;
    private final int bitsPerEntry;
    private final int size;
    private final long maxEntryValue;

    public LegacyFlexibleStorage(int bitsPerEntry, int size) {
        this(bitsPerEntry, new long[roundToNearest(size * bitsPerEntry, 64) / 64]);
    }

    public LegacyFlexibleStorage(int bitsPerEntry, long[] data) {
        if (bitsPerEntry < 4) {
            bitsPerEntry = 4;
        }

        this.bitsPerEntry = bitsPerEntry;
        this.data = Arrays.copyOf(data, data.length);
        this.size = this.data.length * 64 / this.bitsPerEntry;
        this.maxEntryValue = (1L << this.bitsPerEntry) - 1L;
    }

    private static int roundToNearest(int value, int roundTo) {
        if (roundTo == 0) {
            return 0;
        } else if (value == 0) {
            return roundTo;
        } else {
            if (value < 0) {
                roundTo *= -1;
            }

            int remainder = value % roundTo;
            return remainder != 0 ? value + roundTo - remainder : value;
        }
    }

    public int get(int index) {
        int bitIndex = index * this.bitsPerEntry;
        int startIndex = bitIndex / 64;
        int endIndex = ((index + 1) * this.bitsPerEntry - 1) / 64;
        int startBitSubIndex = bitIndex % 64;
        if (startIndex == endIndex) {
            return (int)(this.data[startIndex] >>> startBitSubIndex & this.maxEntryValue);
        } else {
            int endBitSubIndex = 64 - startBitSubIndex;
            return (int)((this.data[startIndex] >>> startBitSubIndex | this.data[endIndex] << endBitSubIndex) & this.maxEntryValue);
        }
    }

    public void set(int index, int value) {
        int bitIndex = index * this.bitsPerEntry;
        int startIndex = bitIndex / 64;
        int endIndex = ((index + 1) * this.bitsPerEntry - 1) / 64;
        int startBitSubIndex = bitIndex % 64;
        this.data[startIndex] = this.data[startIndex] & ~(this.maxEntryValue << startBitSubIndex) | ((long)value & this.maxEntryValue) << startBitSubIndex;
        if (startIndex != endIndex) {
            int endBitSubIndex = 64 - startBitSubIndex;
            int j1 = this.bitsPerEntry - endBitSubIndex; // Fix from the Forge MDK 1.12.2
            this.data[endIndex] = this.data[endIndex] >>> j1 << j1 | ((long)value & this.maxEntryValue) >> endBitSubIndex;
        }

    }

    public long[] getData() {
        return this.data;
    }

    public int getBitsPerEntry() {
        return this.bitsPerEntry;
    }

    public int getSize() {
        return this.size;
    }

    public long getMaxEntryValue() {
        return this.maxEntryValue;
    }
}
