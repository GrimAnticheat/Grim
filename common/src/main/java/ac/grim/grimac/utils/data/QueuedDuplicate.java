package ac.grim.grimac.utils.data;

import com.github.retrooper.packetevents.protocol.world.Location;
import org.jetbrains.annotations.NotNull;

public record QueuedDuplicate(boolean onGround, @NotNull Location location) {}
