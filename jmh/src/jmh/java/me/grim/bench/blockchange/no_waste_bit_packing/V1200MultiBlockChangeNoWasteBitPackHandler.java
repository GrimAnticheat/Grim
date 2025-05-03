package me.grim.bench.blockchange.no_waste_bit_packing;

import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.netty.buffer.ByteBufHelper;
import io.netty.buffer.ByteBuf;
import me.grim.bench.blockchange.VersionedMultiBlockChangeHandler;

public final class V1200MultiBlockChangeNoWasteBitPackHandler
        implements VersionedMultiBlockChangeHandler {

    /* ==========================================================
     *  1.  constants – tweak here for modded platforms
     * ========================================================== */
    private static final class MBC {

        /* header layout */
        static final int SEC_X_BITS = 22;
        static final int SEC_Z_BITS = 22;
        static final int SEC_Y_BITS = 8;
        static final int COUNT_BITS = 12;            // supports 0-4095 records
        static final int HEADER_BITS =
                SEC_X_BITS + SEC_Z_BITS + SEC_Y_BITS + COUNT_BITS; // 64

        /* one block record */
        static final int STATE_BITS = 15;            // raise to 16+ on modded
        static final int LOC_BITS   = 4;             // lx,lz,ly 0-15
        static final int REC_BITS   =
                STATE_BITS + 3 * LOC_BITS;           // default 27

        /* masks */
        static final long MASK_SEC_X = (1L << SEC_X_BITS) - 1;
        static final long MASK_SEC_Z = (1L << SEC_Z_BITS) - 1;
        static final long MASK_SEC_Y = (1L << SEC_Y_BITS) - 1;
        static final long MASK_COUNT = (1L << COUNT_BITS) - 1;

        static final int  MASK_STATE = (1 << STATE_BITS) - 1;
        static final int  MASK_LOC   = (1 << LOC_BITS)  - 1;
    }

    /* range check & transaction throttle identical to old impl */
    private static final int  NEAR_RANGE = 16;
    private static final long TX_COOLDOWN_MS = 2;

    /* ==========================================================
     *  2.  public entry – called for every outgoing packet
     * ========================================================== */
    @Override
    public void handleMultiBlockChange(GrimPlayer player, PacketSendEvent event) {

        ByteBuf buf = (ByteBuf) event.getByteBuf();
        long now    = System.currentTimeMillis();

        /* ---------- 1. read vanilla header & record count ----- */
        final long sectionPos = ByteBufHelper.readLong(buf);
        final int  recCount   = ByteBufHelper.readVarInt(buf); // 0-4095

        /* ----- pre-decode section coords for distance test ---- */
        final int secX =  (int)  (sectionPos >>> 42);                 // 22 bits
        final int secZ =  (int) ((sectionPos >>> 20) & 0x3FFFFF);     // 22 bits
        final int secY =  (int) ((sectionPos >>> 12) & 0xFF);         // 8 bits

        final int baseX = secX << 4;
        final int baseY = secY << 4;
        final int baseZ = secZ << 4;

        /* ===== 2. allocate the *exact* long[] for our bit stream ===== */
        final int totalBits  = MBC.HEADER_BITS + MBC.REC_BITS * recCount;
        final int totalLongs = (totalBits + 63) >>> 6;        // ceil(/64)
        final long[] bits    = new long[totalLongs];

        /* cursors while encoding */
        int bitCursor = 0;                 // bit offset into bits[]

        /* ------------- 3. write header (64 bits, no slack) --------- */
        long header =
                ((long) secX  & MBC.MASK_SEC_X) << (64 - MBC.SEC_X_BITS)
                        | ((long) secZ  & MBC.MASK_SEC_Z) << (MBC.COUNT_BITS + MBC.SEC_Y_BITS)
                        | ((long) secY  & MBC.MASK_SEC_Y) <<  MBC.COUNT_BITS
                        |  (long) recCount & MBC.MASK_COUNT;

        bitCursor = writeBits(bits, bitCursor, header, 64);

        /* ------------- 4. iterate vanilla records ------------------ */
        boolean sendTx = false;

        for (int i = 0; i < recCount; i++) {

            long vanilla = readVarLong(buf);                   // 52+12 bits
            int  local   = (int)  (vanilla & 0xFFFL);          // 12-bit pos
            int  id      = (int)  (vanilla >>> 12) & MBC.MASK_STATE;

            /* near-player test, identical maths as old code */
            if (!sendTx) {
                int lx = (local >>> 8) & 0xF;
                int lz = (local >>> 4) & 0xF;
                int ly =  local        & 0xF;

                int wx = baseX + lx, wy = baseY + ly, wz = baseZ + lz;

                if (Math.abs(wx - player.x) < NEAR_RANGE &&
                        Math.abs(wy - player.y) < NEAR_RANGE &&
                        Math.abs(wz - player.z) < NEAR_RANGE &&
                        player.lastTransSent + TX_COOLDOWN_MS < now) {
                    sendTx = true;
                }
            }

            /* pack 27-bit record */
            int record27 = packRecord27(id, local);
            bitCursor = writeBits(bits, bitCursor, record27, MBC.REC_BITS);
        }

        /* ------------- 5. maybe push a transaction --------------- */
        if (sendTx) {
            player.sendTransaction();
        }

        /* ------------- 6. queue the runnable ---------------------- */
        player.latencyUtils.addRealTimeTask(
                player.lastTransactionSent.get(),
                () -> runDecodeAndApply(player, bits));
    }

    /* ==========================================================
     *  3. helpers  (pure statics)
     * ========================================================== */

    /** packs stateId + 12-bit local pos into 27 bits */
    private static int packRecord27(int stateId, int local12) {

        int lx = (local12 >>> 8) & 0xF;
        int lz = (local12 >>> 4) & 0xF;
        int ly =  local12        & 0xF;

        return  (stateId & MBC.MASK_STATE) << (MBC.LOC_BITS * 3) |
                (lx       & MBC.MASK_LOC ) << (MBC.LOC_BITS * 2) |
                (lz       & MBC.MASK_LOC ) <<  MBC.LOC_BITS      |
                (ly       & MBC.MASK_LOC);
    }

    /* ---------- generic bit write (little helper) --------------- */
    private static int writeBits(long[] arr, int cursor, long value, int len) {

        int idx     = cursor >>> 6;
        int bitOff  = cursor & 63;

        if (len != 64) value &= (1L << len) - 1;

        arr[idx] |= value << bitOff;

        int spill = (bitOff + len) - 64;
        if (spill > 0)
            arr[idx + 1] |= value >>> (len - spill);

        return cursor + len;
    }

    /* ---------- generic bit read --------------------------------- */
    private static long readBits(long[] arr, int cursor, int len) {

        int idx    = cursor >>> 6;
        int bitOff = cursor & 63;

        long word = arr[idx] >>> bitOff;
        int spill = (bitOff + len) - 64;
        if (spill > 0)
            word |= arr[idx + 1] << (len - spill);

        return (len == 64) ? word : word & ((1L << len) - 1);
    }

    /* ==========================================================
     *  4.  Runnable body – decode & mutate world
     * ========================================================== */
    private static void runDecodeAndApply(GrimPlayer player, long[] data) {

        int cursor = 0;

        /* -------- header -------- */
        long header = readBits(data, cursor, 64);
        cursor += 64;

        int recCount = (int) (header & MBC.MASK_COUNT);
        int secY     = (int) ((header >>> MBC.COUNT_BITS) & MBC.MASK_SEC_Y);
        int secZ     = (int) ((header >>> (MBC.COUNT_BITS + MBC.SEC_Y_BITS))
                & MBC.MASK_SEC_Z);
        int secX     = (int) (header >>> (64 - MBC.SEC_X_BITS));

        int baseX = secX << 4;
        int baseY = secY << 4;
        int baseZ = secZ << 4;

        /* -------- records -------- */
        for (int i = 0; i < recCount; i++) {

            int rec = (int) readBits(data, cursor, MBC.REC_BITS);
            cursor += MBC.REC_BITS;

            int stateId =  rec >>> (MBC.LOC_BITS * 3);
            int lx      = (rec >>> (MBC.LOC_BITS * 2)) & MBC.MASK_LOC;
            int lz      = (rec >>>  MBC.LOC_BITS     ) & MBC.MASK_LOC;
            int ly      =  rec                          & MBC.MASK_LOC;

            player.compensatedWorld.updateBlock(
                    baseX + lx, baseY + ly, baseZ + lz, stateId);
        }
    }
}
