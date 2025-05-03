package me.grim.bench.blockchange.multi_block_change_long_pack;

import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.netty.buffer.ByteBufHelper;
import io.netty.buffer.ByteBuf;

public final class LegacyMultiBlockChangeHandler implements VersionedMultiBlockChangeHandler {

    @Override
    public void handleMultiBlockChange(GrimPlayer player, PacketSendEvent event) {
        throw new UnsupportedOperationException(); // todo
    }
}
