package ac.grim.grimac.checks.impl.post;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PostCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;

@CheckData(name = "PostH")
public class PostH extends PostCheck {
    public PostH(GrimPlayer player) {
        super(player, PacketType.Play.Client.USE_ITEM);
    }
}
