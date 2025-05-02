package me.grim.bench.blockchange;

import ac.grim.grimac.events.packets.worldreader.BasePacketWorldReader;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketSendEvent;

public abstract class AbstractBenchmarkBlockChangeHandler extends BasePacketWorldReader {
    @Override
    public abstract void handleBlockChange(GrimPlayer player, PacketSendEvent event);
    @Override
    public abstract void handleMultiBlockChange(GrimPlayer player, PacketSendEvent event);
}
