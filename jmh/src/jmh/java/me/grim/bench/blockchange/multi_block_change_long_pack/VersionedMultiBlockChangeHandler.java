package me.grim.bench.blockchange.multi_block_change_long_pack;

import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import io.netty.buffer.ByteBuf;

public interface VersionedMultiBlockChangeHandler {

    static final int RANGE = 16;
    static final long TRANSACTION_COOLDOWN_MS = 2; // In milliseconds

    public void handleMultiBlockChange(GrimPlayer player, PacketSendEvent event);
    public default long readVarLong(ByteBuf buf) {
        long value = 0;
        int size = 0;
        int b;
        while (((b = buf.readByte()) & 0x80) == 0x80) {
            value |= (long) (b & 0x7F) << (size++ * 7);
        }
        return value | ((long) (b & 0x7F) << (size * 7));
    }
}
