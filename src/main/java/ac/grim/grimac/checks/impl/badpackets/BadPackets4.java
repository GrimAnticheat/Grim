package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;

@CheckData(name = "BadPackets4")
public class BadPackets4 extends Check implements PacketCheck {
    public BadPackets4(final GrimPlayer player) {
        super(player);
    }
}
