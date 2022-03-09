package ac.grim.grimac.checks.impl.post;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PostCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;

@CheckData(name = "PostE")
public class PostE extends PostCheck {
    public PostE(GrimPlayer player) {
        // 1.15+ clients send this packet whenever they want to, not aligned to any tick
        super(player, player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_15) ? PacketType.Play.Server.CHAT_MESSAGE : PacketType.Play.Client.CLICK_WINDOW);
    }
}
