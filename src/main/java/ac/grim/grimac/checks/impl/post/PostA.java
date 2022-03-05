package ac.grim.grimac.checks.impl.post;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PostCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;

// Frequency BadPacketsA
@CheckData(name = "PostA")
public final class PostA extends PostCheck {
    public PostA(final GrimPlayer player) {
        super(player, PacketType.Play.Client.PLAYER_DIGGING);
    }
}