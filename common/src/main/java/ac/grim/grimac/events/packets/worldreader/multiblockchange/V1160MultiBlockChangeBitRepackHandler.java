package ac.grim.grimac.events.packets.worldreader.multiblockchange;

import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.netty.buffer.ByteBufHelper;
import io.netty.buffer.ByteBuf;

/**
 * <p>
 * Minecraft’s MultiBlockChange packet batches many block updates in one shot:
 * the server sends a 64-bit section coordinate, an optional trustEdges flag,
 * a VarInt count, then that many VarLong-encoded block changes
 * (<em>52 bits</em> of global blockStateId + <em>12 bits</em> of local X/Z/Y
 * within the 16×16×16 section).
 * </p>
 *
 * <p>
 * To shrink our on-heap footprint for deferred tasks, we immediately repack
 * each VarLong into one 32-bit <code>int</code>, while still using the
 * vanilla 64-bit header for coords.
 * </p>
 *
 * <h3>Vanilla “on-the-wire” format</h3>
 * <pre>
 * 1) sectionEncodedPosition : Long
 * 2) [trustEdges?           : Boolean]   // only on protocol ≤1.19.4
 * 3) recordCount            : VarInt
 * 4) records[]              : recordCount × VarLong
 *
 *    Each VarLong is bits:
 *     [63……12] blockStateId (52 bits)
 *     [11……8]  localX       (4 bits, 0–15)
 *     [7……4]   localZ       (4 bits, 0–15)
 *     [3……0]   localY       (4 bits, 0–15)
 * </pre>
 *
 * <h3>Vanilla 64-bit section header “encodedPosition”</h3>
 * <pre>
 * bits   63……42   41……20    19……0
 *       ┌────────┬─────────┬────────┐
 *       │  secX  │  secZ   │  secY  │
 *       │ (22b)  │ (22b)   │ (20b)  │
 *       └────────┴─────────┴────────┘
 *
 *   secX = encoded >> 42
 *   secZ = encoded << 22 >> 42
 *   secY = encoded << 44 >> 44
 * </pre>
 *
 * <h3>Our 32-bit repacked block record (MSB → LSB)</h3>
 * <pre>
 * bits  31…17   16…12   11…8   7…4   3…0
 *      ┌───────┬───────┬───────┬──────┬─────┐
 *      │ state │ spare │  lX   │  lZ  │ lY  │
 *      │ (15b) │ (5b)  │ (4b)  │ (4b) │(4b) │
 *      └───────┴───────┴───────┴──────┴─────┘
 *
 *  • state = (data >>> 12) & 0x7FFF
 *  • spare = bits 12–16 (unused, reserved for flags)
 *  • lX    = (data >>>  8) & 0xF
 *  • lZ    = (data >>>  4) & 0xF
 *  • lY    =  data          & 0xF
 *
 *  Then pack:  packed = (state << 17) | ((lX<<8)|(lZ<<4)|lY)
 * </pre>
 */
public final class V1160MultiBlockChangeBitRepackHandler
        implements ac.grim.grimac.events.packets.worldreader.multiblockchange.VersionedMultiBlockChangeHandler {

    /* ---------- bit masks / shifts for the packed int ---------- */
    private static final int SHIFT_STATE = 17;        // 32-15 = 17
    private static final int MASK_STATE  = 0x7FFF;    // 15 bits

    static final int MASK_LOCAL  = 0xFFF;  // 12 bits

    /* ---------- does this protocol still have trustEdges ? ----- */
    private static final boolean HAS_TRUST_EDGES =
            PacketEvents.getAPI().getServerManager().getVersion().isOlderThanOrEquals(ServerVersion.V_1_19_4);

    @Override
    public void handleMultiBlockChange(GrimPlayer player, PacketSendEvent event) {
        // PE resets writer index for us, we don't have to call buffer.writerIndex(originalWriterIndex)
        ByteBuf buf = (ByteBuf) event.getByteBuf();

        /* 1. Section-position header (64 bits) ------------------ */
        long sectionEncodedPosition = ByteBufHelper.readLong(buf);

        if (HAS_TRUST_EDGES) {       // skip only when it really exists
            buf.skipBytes(1);
        }

        /* 2. Record count  + packed-int array ------------------- */
        int recordCount = ByteBufHelper.readVarInt(buf);
        int[] packed    = new int[recordCount];

        /* 3. Decode packet  (still comes as varLong per record)  */
        // Unpack section coords once for the “near player” test
        int secX = (int) (sectionEncodedPosition >> 42);
        int secZ = (int) (sectionEncodedPosition << 22 >> 42);
        int secY = (int) (sectionEncodedPosition << 44 >> 44);

        int baseX = secX << 4;
        int baseY = secY << 4;
        int baseZ = secZ << 4;

        boolean sendTx = false;

        // Use this to not spam the player with transactions if one has been sent within COOLDOWN
        long now = System.currentTimeMillis();
        for (int i = 0; i < recordCount; i++) {

            long data = readVarLong(buf);               // 52+12 bits

            int local  = (int) (data & 0xFFFL);         // 12-bit pos

            packed[i] = repackFromLong(data);

            /* -------- near-player distance test -------------- */
            if (!sendTx) {
                int lx = (local >>> 8) & 0xF;
                int lz = (local >>> 4) & 0xF;
                int ly =  local        & 0xF;

                int wx = baseX + lx, wy = baseY + ly, wz = baseZ + lz;

                if (Math.abs(wx - player.x) < RANGE &&
                        Math.abs(wy - player.y) < RANGE &&
                        Math.abs(wz - player.z) < RANGE &&
                        player.lastTransSent + TRANSACTION_COOLDOWN_MS < now) {
                    sendTx = true;
                }
            }
        }

        if (sendTx)
            player.sendTransaction();

        /* 4. Queue runnable – captures only int[] + sectionPos  */
        player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> {

            // unpack section once per execution
            int sX = (int) (sectionEncodedPosition >> 42);
            int sY = (int) (sectionEncodedPosition << 44 >> 44);
            int sZ = (int) (sectionEncodedPosition << 22 >> 42);

            int bx = sX << 4, by = sY << 4, bz = sZ << 4;

            for (int rec : packed) {
                int stateId = (rec >>> SHIFT_STATE) & MASK_STATE;
                int lx      = (rec >>> 8)    & 0xF;
                int lz      = (rec >>> 4)    & 0xF;
                int ly      = rec            & 0xF;

                int wx = bx + lx;
                int wy = by + ly;
                int wz = bz + lz;

                player.compensatedWorld.updateBlock(wx, wy, wz, stateId);
            }
        });
    }

    public int repackFromLong(long data) {
        // 1) extract the 15-bit state from bits 12.. (original >> 12)
        int blockState = (int)((data >>> 12) & MASK_STATE);

        // 2) extract the 12-bit local from bits 0..11
        int local = (int)( data         & MASK_LOCAL);

        // 3) glue them together
        return (blockState << SHIFT_STATE) | local;
    }
}
