package ac.grim.grimac.utils.data;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.math.Vector3dm;
import com.github.retrooper.packetevents.protocol.teleport.RelativeFlag;
import com.github.retrooper.packetevents.util.Vector3d;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
public class TeleportData {
    Vector3d location;
    Vector3d velocity;
    RelativeFlag flags;
    @Setter
    int transaction;
    @Setter
    int teleportId;

    public void modifyVector(GrimPlayer player, Vector3dm vector) {
        final boolean isStupidTeleportSystem = player.supportsEndTick();
        if (!isStupidTeleportSystem) {
            if (!isRelativeX()) {
                vector.setX(0);
            }

            if (!isRelativeY()) {
                vector.setY(0);
                player.lastWasClimbing = 0; // Vertical movement reset
                player.canSwimHop = false; // Vertical movement reset
            }

            if (!isRelativeZ()) {
                vector.setZ(0);
            }
        }

        if (velocity != null && isStupidTeleportSystem) {
            // WHAT WAS MOJANG THINKING MAKING TELEPORTS A REPLACEMENT PACKET FOR EXPLOSION VELOCITY /s
            if (isRelativeDeltaX()) {
                vector.setX(vector.getX() + velocity.getX());
            } else {
                vector.setX(velocity.getX());
            }

            if (isRelativeDeltaY()) {
                vector.setY(vector.getY() + velocity.getY());
            } else {
                vector.setY(velocity.getY());
                // Is this correct? Don't know don't care.
                player.lastWasClimbing = 0; // Vertical movement reset
                player.canSwimHop = false; // Vertical movement reset
            }

            if (isRelativeDeltaZ()) {
                vector.setZ(vector.getZ() + velocity.getZ());
            } else {
                vector.setZ(velocity.getZ());
            }
        }
    }

    public boolean isRelativeVelocity() {
        return isRelativeDeltaX() || isRelativeDeltaY() || isRelativeDeltaZ();
    }

    public boolean isRelativeDeltaX() {
        return flags.has(RelativeFlag.DELTA_X);
    }

    public boolean isRelativeDeltaY() {
        return flags.has(RelativeFlag.DELTA_Y);
    }

    public boolean isRelativeDeltaZ() {
        return flags.has(RelativeFlag.DELTA_Z);
    }

    public boolean isRelativePos() {
        return isRelativeX() || isRelativeY() || isRelativeZ();
    }

    public boolean isRelativeX() {
        return flags.has(RelativeFlag.X.getMask());
    }

    public boolean isRelativeY() {
        return flags.has(RelativeFlag.Y.getMask());
    }

    public boolean isRelativeZ() {
        return flags.has(RelativeFlag.Z.getMask());
    }
}
