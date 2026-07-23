package ac.grim.grimac.utils.data;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.world.Location;
import org.jetbrains.annotations.NotNull;

public record QueuedDuplicate(@NotNull PacketReceiveEvent event, boolean onGround, @NotNull Location location) {}
