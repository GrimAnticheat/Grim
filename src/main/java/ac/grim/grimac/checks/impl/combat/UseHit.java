package ac.grim.grimac.checks.impl.combat;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;

@CheckData(name = "UseHit")
public class UseHit extends Check implements PacketCheck {
    public boolean isUsingItem;
    public int movementPacketsSinceChange;

    public UseHit(final GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        // This check hasn't been tested on versions older before 1.17
        if (player.getClientVersion().isOlderThan(ClientVersion.V_1_17)) return;
        
        if (!player.disableGrim && event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            if (isUsingItem && movementPacketsSinceChange > 1) {
                WrapperPlayClientInteractEntity action = new WrapperPlayClientInteractEntity(event);
                if (flag(true, false, "action=" + action.getAction().name() + " hand=" + action.getHand().name() + " sin=" + movementPacketsSinceChange)) {
                    if (shouldModifyPackets()) {
                        event.setCancelled(true);
                        player.onPacketCancel();
                    }
                }
            }
        }
    }
}
