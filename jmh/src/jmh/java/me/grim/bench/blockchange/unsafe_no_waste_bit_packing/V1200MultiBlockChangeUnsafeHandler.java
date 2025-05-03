package me.grim.bench.blockchange.unsafe_no_waste_bit_packing;

import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.netty.buffer.ByteBufHelper;
import io.netty.buffer.ByteBuf;
import me.grim.bench.blockchange.VersionedMultiBlockChangeHandler;
import me.grim.bench.util.*;

import java.util.concurrent.atomic.LongAdder;

/**
 *   52-bit header   (secX 22 | secZ 22 | secY 8)
 * + 27·n  bits per block record  (state 15 | lx,lz,ly 4)
 * = fully-dense stream copied to off-heap memory via Unsafe.
 */
public final class V1200MultiBlockChangeUnsafeHandler
        implements VersionedMultiBlockChangeHandler {


    /* ------------------------------------------------------------------
     *  ❶  one static counter, shared by all instances; temp for debugging
     * ------------------------------------------------------------------ */
    private static final LongAdder OFF_HEAP = new LongAdder();

    /** how many native bytes are currently held by queued tasks */
    public static long getNativeBytes() { return OFF_HEAP.sum(); }

    /** (optional) reset between benchmark runs */
    public static void resetNativeBytes() { OFF_HEAP.reset(); }


    /* ===== geometry ===== */
    private static final int SEC_X_BITS = 22;
    private static final int SEC_Z_BITS = 22;
    private static final int SEC_Y_BITS = 8;
    private static final int HEADER_BITS = SEC_X_BITS + SEC_Z_BITS + SEC_Y_BITS; // 52

    private static final int STATE_BITS = 15;          // raise if needed
    private static final int LOC_BITS   = 4;
    private static final int REC_BITS   = STATE_BITS + 3 * LOC_BITS;             // 27

    private static final long MASK_SEC_X = (1L << SEC_X_BITS) - 1;
    private static final long MASK_SEC_Z = (1L << SEC_Z_BITS) - 1;
    private static final long MASK_SEC_Y = (1L << SEC_Y_BITS) - 1;

    private static final int  MASK_STATE = (1 << STATE_BITS) - 1;
    private static final int  MASK_LOC   = (1 << LOC_BITS)  - 1;

    private static final int  NEAR_RANGE      = 16;
    private static final long TX_COOLDOWN_MS  = 2;

    /* ========================================================= */
    @Override
    public void handleMultiBlockChange(GrimPlayer player, PacketSendEvent event) {

        ByteBuf in = (ByteBuf) event.getByteBuf();
        long now   = System.currentTimeMillis();

        /* ---------- read vanilla header ----------------------- */
        long sectionPos = ByteBufHelper.readLong(in);
        int  recCount   = ByteBufHelper.readVarInt(in);   // 0-4095 (vanilla)

        int secX =  (int)  (sectionPos >>> 42);
        int secZ =  (int) ((sectionPos >>> 20) & 0x3FFFFF);
        int secY =  (int) ((sectionPos >>> 12) & 0xFF);

        int bx = secX << 4, by = secY << 4, bz = secZ << 4;

        /* ---------- pack into temp long[] --------------------- */
        int totalBits  = HEADER_BITS + REC_BITS * recCount;
        int totalLongs = (totalBits + 63) >>> 6;          // ceil(/64)
        long[] tmp     = new long[totalLongs];
        int cursor = 0;

        /* 52-bit header */
        long header =
                ((long) secX & MASK_SEC_X) << (SEC_Z_BITS + SEC_Y_BITS) |
                        ((long) secZ & MASK_SEC_Z) <<  SEC_Y_BITS |
                        ((long) secY & MASK_SEC_Y);
        cursor = writeBits(tmp, cursor, header, HEADER_BITS);

        boolean sendTx = false;

        /* ---------- iterate vanilla records ------------------- */
        for (int i = 0; i < recCount; i++) {

            long vanilla = readVarLong(in);
            int  local12 = (int)  (vanilla & 0xFFFL);
            int  id      = (int)  (vanilla >>> 12) & MASK_STATE;

            /* distance check (unchanged) */
            if (!sendTx) {
                int lx = (local12 >>> 8) & 0xF;
                int lz = (local12 >>> 4) & 0xF;
                int ly =  local12        & 0xF;
                int wx = bx + lx, wy = by + ly, wz = bz + lz;
                if (Math.abs(wx - player.x) < NEAR_RANGE &&
                        Math.abs(wy - player.y) < NEAR_RANGE &&
                        Math.abs(wz - player.z) < NEAR_RANGE &&
                        player.lastTransSent + TX_COOLDOWN_MS < now)
                    sendTx = true;
            }

            int rec27 = pack27(id, local12);
            cursor = writeBits(tmp, cursor, rec27, REC_BITS);
        }

        if (sendTx) {
            player.sendTransaction();
        }

        /* ------------ move blob off-heap ---------------------- */
        long bytes = (totalBits + 7) >>> 3;           // ceil(/8)
        long addr  = UnsafeUtil.alloc(bytes);

        /* ❷ account for it */
        OFF_HEAP.add(bytes);

        UnsafeUtil.copyFromLongArray(tmp, totalLongs, addr);

//        System.out.println("packed " + totalBits + " bits  =>  " + bytes + " bytes");

        /* ------------ queue the runnable ---------------------- */
        player.latencyUtils.addRealTimeTask(
                player.lastTransactionSent.get(),
                () -> {
                    decodeAndApply(player, addr, totalBits);
                    UnsafeUtil.free(addr);

                    /* ❸ release accounting */
                    OFF_HEAP.add(-bytes);
                });
    }

    /* =========================================================
     * helpers
     * ========================================================= */
    private static int pack27(int stateId, int local) {
        int lx = (local >>> 8) & 0xF;
        int lz = (local >>> 4) & 0xF;
        int ly =  local        & 0xF;
        return  (stateId & MASK_STATE) << (LOC_BITS * 3) |
                (lx      & MASK_LOC )  << (LOC_BITS * 2) |
                (lz      & MASK_LOC )  <<  LOC_BITS      |
                (ly      & MASK_LOC);
    }

    private static int writeBits(long[] a, int cur, long v, int len) {
        int idx = cur >>> 6, off = cur & 63;
        if (len != 64) v &= (1L << len) - 1;
        a[idx] |= v << off;
        int spill = (off + len) - 64;
        if (spill > 0) a[idx + 1] |= v >>> (len - spill);
        return cur + len;
    }

    private static long readBits(long base, int cur, int len) {
        int idx = cur >>> 6, off = cur & 63;
        long w  = UnsafeUtil.getLong(base + ((long) idx << 3)) >>> off;
        int spill = (off + len) - 64;
        if (spill > 0) {
            long nxt = UnsafeUtil.getLong(base + ((long) (idx + 1) << 3));
            w |= nxt << (len - spill);
        }
        return (len == 64) ? w : w & ((1L << len) - 1);
    }

    /* ----------------------- decode ------------------------- */
    private static void decodeAndApply(GrimPlayer p, long addr, int bits) {

        int cur = 0;
        long header = readBits(addr, cur, HEADER_BITS);
        cur += HEADER_BITS;

        int secY = (int) (header & MASK_SEC_Y);
        int secZ = (int) ((header >>> SEC_Y_BITS) & MASK_SEC_Z);
        int secX = (int) (header >>> (SEC_Z_BITS + SEC_Y_BITS));

        int bx = secX << 4, by = secY << 4, bz = secZ << 4;

        int recCount = (bits - HEADER_BITS) / REC_BITS;

        for (int i = 0; i < recCount; i++) {

            int rec = (int) readBits(addr, cur, REC_BITS);
            cur += REC_BITS;

            int stateId =  rec >>> (LOC_BITS * 3);
            int lx      = (rec >>> (LOC_BITS * 2)) & MASK_LOC;
            int lz      = (rec >>>  LOC_BITS     ) & MASK_LOC;
            int ly      =  rec                      & MASK_LOC;

            p.compensatedWorld.updateBlock(bx + lx, by + ly, bz + lz, stateId);
        }
    }
}