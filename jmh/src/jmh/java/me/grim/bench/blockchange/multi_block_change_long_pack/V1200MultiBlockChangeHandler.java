package me.grim.bench.blockchange.multi_block_change_long_pack;

import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.netty.buffer.ByteBufHelper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerMultiBlockChange;
import io.netty.buffer.ByteBuf;

public final class V1200MultiBlockChangeHandler implements VersionedMultiBlockChangeHandler {

    private static final int  RANGE     = 16;
    private static final long COOLDOWN  = 2;          // ms

    @Override
    public void handleMultiBlockChange(GrimPlayer player, PacketSendEvent event) {
        ByteBuf buf = (ByteBuf) event.getByteBuf();
        long    now = System.currentTimeMillis();

        /* 1. raw section-position (one 64-bit value) */
        long sectionPos = ByteBufHelper.readLong(buf);

        // Do this for 1.16 < version <= 1.19.4
//        ByteBufHelper.skipBytes(buf, 1);              // trustEdges (boolean)

        /* 2. record count + storage (one long per record) */
        int   count   = ByteBufHelper.readVarInt(buf);
        long[] recArr = new long[count];

        /* 3. near-player test needs section coords once */
        int sX = (int) (sectionPos >>> 42);
        int sY = (int) (sectionPos << 44 >>> 44);
        int sZ = (int) (sectionPos << 22 >>> 42);

        int baseX = sX << 4;
        int baseY = sY << 4;
        int baseZ = sZ << 4;

        boolean sendTx = false;

        for (int i = 0; i < count; i++) {
            long data = readVarLong(buf);
            recArr[i] = data;

            if (!sendTx) {
                int local = (int) (data & 0xFFFL);
                int x = baseX + ((local >>> 8) & 0xF);
                int y = baseY +  (local        & 0xF);
                int z = baseZ + ((local >>> 4) & 0xF);

                if (Math.abs(x - player.x) < RANGE &&
                        Math.abs(y - player.y) < RANGE &&
                        Math.abs(z - player.z) < RANGE &&
                        player.lastTransSent + COOLDOWN < now) {
                    sendTx = true;
                }
            }
        }

        if (sendTx) {
            player.sendTransaction();
        }

        /* 4. queue task – captures only long[] + long sectionPos */
        player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> {

            // local copy
            int chunkSecX  = (int) (sectionPos >>> 42);
            int chunkSecY  = (int) (sectionPos << 44 >>> 44);
            int chunkSecZ  = (int) (sectionPos << 22 >>> 42);

            for (long d : recArr) {
                short position = (short) (d & 0xFFFL);
                int id     = (int) (d >>> 12);

                int x = (chunkSecX << 4) + ((position >>> 8) & 0xF);
                int y = (chunkSecY << 4) +  (position        & 0xF);
                int z = (chunkSecZ << 4) + ((position >>> 4) & 0xF);

                player.compensatedWorld.updateBlock(x, y, z, id);
            }
        });
    }
}
