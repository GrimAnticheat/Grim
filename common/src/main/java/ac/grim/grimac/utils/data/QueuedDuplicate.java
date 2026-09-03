package ac.grim.grimac.utils.data;

import com.github.retrooper.packetevents.protocol.world.Location;

public record QueuedDuplicate(Location location, boolean onGround) {}
