package ac.grim.grimac.checks.impl.badpackets;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.CheckType;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;

@CheckData(name = "BadPacketsH", description = "Did not swing for attack", checkType = CheckType.PACKETS)
public class BadPacketsH extends Check implements PacketCheck {

    // 1.9 packet order: INTERACT -> ANIMATION
    // 1.8 packet order: ANIMATION -> INTERACT
    // I personally think 1.8 made much more sense. You swing and THEN you hit!
    private boolean sentAnimation = player.getClientVersion().isNewerThan(ClientVersion.V_1_8);

    public BadPacketsH(final GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.ANIMATION) {
            sentAnimation = true;
        } else if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity packet = new WrapperPlayClientInteractEntity(event);
            if (packet.getAction() != WrapperPlayClientInteractEntity.InteractAction.ATTACK) return;

            // There is a "bug" in ViaRewind
            // 1.8 packet order: ANIMATION -> INTERACT
            // 1.9 packet order: INTERACT -> ANIMATION
            // ViaRewind, on 1.9+ servers, delays a 1.8 client's ANIMATION to be after INTERACT (but before flying).
            // Which means we see 1.9 packet order for 1.8 clients
            // Due to ViaRewind also delaying the swings, we then see packet order above 20CPS like:
            // INTERACT -> INTERACT -> ANIMATION -> ANIMATION
            // I will simply disable this check for 1.8- clients on 1.9+ servers as I can't be bothered to find a way around this.
            // Stop supporting such old clients on modern servers!
            if (player.getClientVersion().isOlderThan(ClientVersion.V_1_9)
                    && PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_9)) return;

            if (!sentAnimation && flagAndAlert() && shouldModifyPackets()) {
                event.setCancelled(true);
                player.onPacketCancel();
            }

            sentAnimation = false;
        }
    }
}
