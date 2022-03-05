package ac.grim.grimac.checks.impl.post;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PostCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;

@CheckData(name = "PostG")
public class PostG extends PostCheck {
    public PostG(GrimPlayer player) {
        super(player, PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT);
    }
}
