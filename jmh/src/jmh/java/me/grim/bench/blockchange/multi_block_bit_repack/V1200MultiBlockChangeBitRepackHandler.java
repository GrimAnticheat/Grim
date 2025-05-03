package me.grim.bench.blockchange.multi_block_bit_repack;

import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.netty.buffer.ByteBufHelper;
import io.netty.buffer.ByteBuf;
import me.grim.bench.blockchange.VersionedMultiBlockChangeHandler;

/**
 * Vanilla-only, tested against (≤ 1.21.4) multi-block-change handler that stores every
 * block record in ONE 32-bit int.
 *
 * Assumptions that make this possible
 * -----------------------------------
 *  • Vanilla has ~26 000 block states ⇒ 15 bits are enough.
 *  • A chunk-section is 16×16×16. Y-range –2031…2031 fits in 8 bits once
 *    you divide by 16 (shift right 4).
 *  • Server ≥ 1.20 ⇒ “trustEdges” byte disappeared from the packet.
 *
 * Layout we pack into the int   (MSB → LSB)
 * ┌───────────────┬──────┬──────┬──────┬──────┐
 * │ 31……17        │16…13 │12…9 │ 8…5 │ 4…0 │
 * │ blockStateId  │ lX   │ lZ  │ lY  │ free│
 * │   15 bits     │4 bits│4bits│4bits│5bits│
 * └───────────────┴──────┴──────┴──────┴──────┘
 * lX/lY/lZ  = 0-15 local coord in the section.
 * Five LSBs are currently unused but reserved for future flags.
 *
 * Section-position long (packet header)
 * ┌──────────22─────────┬──────────22─────────┬─────8────┬─1─┬──11─┐
 * │       secX          │       secZ          │  secY    │T? │free │
 * └─────────────────────┴─────────────────────┴──────────┴───┴─────┘
 * T? = old “trustEdges” flag (present only up to 1.19.4).
 * We leave 11 spare bits for mods / palette index, etc.
 */
public final class V1200MultiBlockChangeBitRepackHandler
        implements VersionedMultiBlockChangeHandler {

    /* ---------- bit masks / shifts for the packed int ---------- */
    private static final int SHIFT_STATE = 17;        // 32-15 = 17
    private static final int MASK_STATE  = 0x7FFF;    // 15 bits

    private static final int SHIFT_LX = 13;
    private static final int SHIFT_LZ =  9;
    private static final int SHIFT_LY =  5;
    private static final int MASK_LOC = 0xF;          // 4 bits

    /* ---------- does this protocol still have trustEdges ? ----- */
    private static final boolean HAS_TRUST_EDGES =
            PacketEvents.getAPI().getServerManager().getVersion().isOlderThanOrEquals(ServerVersion.V_1_19_4);

    private static final int RANGE      = 16;
    private static final long COOLDOWN  = 2;          // ms

    @Override
    public void handleMultiBlockChange(GrimPlayer player, PacketSendEvent event) {

        ByteBuf buf = (ByteBuf) event.getByteBuf();
        long now    = System.currentTimeMillis();

        /* 1. Section-position header (64 bits) ------------------ */
        long sectionPos = ByteBufHelper.readLong(buf);

        if (HAS_TRUST_EDGES) {       // skip only when it really exists
            buf.skipBytes(1);
        }

        /* 2. Record count  + packed-int array ------------------- */
        int recordCount = ByteBufHelper.readVarInt(buf);
        int[] packed    = new int[recordCount];

        /* 3. Decode packet  (still comes as varLong per record)  */
        // Unpack section coords once for the “near player” test
        int secX =  (int)  (sectionPos >>> 42);                 // 22 bits
        int secZ =  (int) ((sectionPos >>> 20) & 0x3FFFFF);     // 22 bits
        int secY =  (int) ((sectionPos >>> 12) & 0xFF);         // 8 bits

        int baseX = secX << 4;
        int baseY = secY << 4;
        int baseZ = secZ << 4;

        boolean sendTx = false;

        for (int i = 0; i < recordCount; i++) {

            long data = readVarLong(buf);               // 52+12 bits

            int local  = (int) (data & 0xFFFL);         // 12-bit pos
            int id     = (int) (data >>> 12);           // 52-bit state → 15 ok

            packed[i] = (id << SHIFT_STATE) | local;    // store compact

            /* -------- near-player distance test -------------- */
            if (!sendTx) {
                int lx = (local >>> 8) & 0xF;
                int lz = (local >>> 4) & 0xF;
                int ly =  local        & 0xF;

                int wx = baseX + lx, wy = baseY + ly, wz = baseZ + lz;

                if (Math.abs(wx - player.x) < RANGE &&
                        Math.abs(wy - player.y) < RANGE &&
                        Math.abs(wz - player.z) < RANGE &&
                        player.lastTransSent + COOLDOWN < now) {
                    sendTx = true;
                }
            }
        }

        if (sendTx) player.sendTransaction();

        Runnable runnable = () -> {

            // unpack section once per execution
            int sX = (int)  (sectionPos >>> 42);
            int sZ = (int) ((sectionPos >>> 20) & 0x3FFFFF);
            int sY = (int) ((sectionPos >>> 12) & 0xFF);

            int bx = sX << 4, by = sY << 4, bz = sZ << 4;

            for (int rec : packed) {

                int stateId = (rec >>> SHIFT_STATE) & MASK_STATE;
                int lx      = (rec >>> SHIFT_LX)    & MASK_LOC;
                int lz      = (rec >>> SHIFT_LZ)    & MASK_LOC;
                int ly      = (rec >>> SHIFT_LY)    & MASK_LOC;

                int wx = bx + lx;
                int wy = by + ly;
                int wz = bz + lz;

//                System.out.println("X: " + wx + " Y: " + wy + " Z: " +  wz + " StateID: " +  stateId);
                player.compensatedWorld.updateBlock(wx, wy, wz, stateId);
            }
        };

//        runnable.run();

        /* 4. Queue runnable – captures only int[] + sectionPos  */
        player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), runnable);
    }
}
