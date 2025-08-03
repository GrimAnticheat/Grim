package me.grim.bench.blockchange;

import ac.grim.grimac.events.packets.worldreader.BasePacketWorldReader;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketSendEvent;

public abstract class AbstractBenchmarkBlockChangeHandler extends BasePacketWorldReader {

    public static final int RANGE = 16;
    public static final long TRANSACTION_COOLDOWN_MS = 2; // In milliseconds

    @Override
    public abstract void handleBlockChange(GrimPlayer player, PacketSendEvent event);
    @Override
    public abstract void handleMultiBlockChange(GrimPlayer player, PacketSendEvent event);
}
