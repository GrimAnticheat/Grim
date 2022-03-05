package ac.grim.grimac.checks.impl.post;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PostCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;

// Frequency BadPacketsC
@CheckData(name = "PostC")
public class PostC extends PostCheck {
    public PostC(GrimPlayer player) {
        super(player, PacketType.Play.Client.ENTITY_ACTION);
    }
}
