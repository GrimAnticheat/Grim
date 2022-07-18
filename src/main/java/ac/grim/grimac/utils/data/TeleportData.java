package ac.grim.grimac.utils.data;

import com.github.retrooper.packetevents.protocol.teleport.RelativeFlag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;

@AllArgsConstructor
@Getter
public class TeleportData {
    Location location;
    RelativeFlag flags;
    int transaction;
    @Setter
    int teleportId;

    public boolean isRelativeX() {
        return flags.isSet(RelativeFlag.X.getMask());
    }

    public boolean isRelativeY() {
        return flags.isSet(RelativeFlag.Y.getMask());
    }

    public boolean isRelativeZ() {
        return flags.isSet(RelativeFlag.Z.getMask());
    }
}
