package ac.grim.grimac.checks.impl.post;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PostCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;

// Frequency BadPacketsB
@CheckData(name = "PostB")
public class PostB extends PostCheck {
    public PostB(GrimPlayer player) {
        // Exempt for 1.7-1.8 clients on 1.9+ servers because ViaVersion messes with packet order
        super(player, player.getClientVersion().isOlderThan(ClientVersion.V_1_9) && PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_9)
                ? PacketType.Play.Server.CHAT_MESSAGE : PacketType.Play.Client.ANIMATION);
    }
}
