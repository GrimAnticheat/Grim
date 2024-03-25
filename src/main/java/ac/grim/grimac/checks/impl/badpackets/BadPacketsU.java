package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.impl.crash.CrashI;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;

/**
 * @see CrashI
 */
@CheckData(name = "BadPacketsU")
public class BadPacketsU extends Check implements PacketCheck {
    public BadPacketsU(GrimPlayer player) {
        super(player);
    }

    public void handleColorSign(PacketReceiveEvent event) {
        if (flagAndAlert() && shouldModifyPackets()) {
            event.setCancelled(true);
            player.onPacketCancel();
        }
    }

}
