package ac.grim.grimac.utils.data;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.world.Location;

public record QueuedDuplicate(PacketReceiveEvent event, boolean onGround, Location location) {}
