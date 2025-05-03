package me.grim.bench.blockchange.multi_block_change_long_pack;

import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import me.grim.bench.blockchange.VersionedMultiBlockChangeHandler;

public final class LegacyMultiBlockChangeHandler implements VersionedMultiBlockChangeHandler {

    @Override
    public void handleMultiBlockChange(GrimPlayer player, PacketSendEvent event) {
        throw new UnsupportedOperationException(); // todo
    }
}
