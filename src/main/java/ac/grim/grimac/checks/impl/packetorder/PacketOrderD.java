package ac.grim.grimac.checks.impl.packetorder;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;

@CheckData(name = "PacketOrderD")
public class PacketOrderD extends Check implements PacketCheck {
    public PacketOrderD(GrimPlayer player) {
        super(player);
    }
}
