package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketReceiveListener;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientUseItem;

@CheckData(name = "BadPacketsJ", stableKey = "grim.badpackets.use_item_rotation_mismatch", description = "Rotation in use item packet did not match tick rotation")
public class BadPacketsJ extends Check implements PacketReceiveListener {

    private float yaw;
    private float pitch;
    private int rotations;

    public BadPacketsJ(GrimPlayer player) {
        super(player);
    }

    @Override
    public boolean isApplicable() {
        return player.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_21)
                && PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_21);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!player.cameraEntity.isSelf()) {
            this.rotations = 0;
            return;
        }

        if (event.getPacketType() == PacketType.Play.Client.USE_ITEM) {
            WrapperPlayClientUseItem packet = new WrapperPlayClientUseItem(event);

            float yaw = packet.getYaw();
            float pitch = packet.getPitch();

            if (this.rotations > 0 && (this.yaw != yaw || this.pitch != pitch)) {
                if (!player.canSkipTicks() || this.yaw != player.yaw || this.pitch != player.pitch) {
                    while (this.rotations-- > 0) flag();
                }
                this.rotations = 0;
            }

            if (this.rotations != Integer.MAX_VALUE)
                this.rotations++;
            this.yaw = yaw;
            this.pitch = pitch;
        }

        if (this.rotations > 0 && isTickPacket(event.getPacketType())) {
            if (this.yaw != player.yaw || this.pitch != player.pitch) {
                // due to tick skipping, the rotations sent could be last tick's
                boolean allowLast = player.canSkipTicks()
                        && (event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION
                        || event.getPacketType() == PacketType.Play.Client.PLAYER_ROTATION);
                if (!allowLast || this.yaw != player.lastYaw || this.pitch != player.lastPitch) {
                    while (this.rotations-- > 0) flag();
                }
            }
            this.rotations = 0;
        }
    }
}
