package ac.grim.grimac.checks.impl.post;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PostCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;

// Frequency BadPacketsE
@CheckData(name = "PostD")
public class PostD extends PostCheck {
    public PostD(GrimPlayer player) {
        super(player, PacketType.Play.Client.HELD_ITEM_CHANGE);
    }
}
