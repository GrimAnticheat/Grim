package ac.grim.grimac.checks.impl.movement;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;

@CheckData(name = "NoSlowB", setback = 5)
public class NoSlowB extends Check implements PacketCheck {

    public NoSlowB(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.ENTITY_ACTION) {
            WrapperPlayClientEntityAction.Action action = new WrapperPlayClientEntityAction(event).getAction();
            if (action != WrapperPlayClientEntityAction.Action.START_SPRINTING) return;

            // Players can sprint if they're able to fly (MCP)
            if (player.canFly) return;

            if (player.food < 6.0F) {
                flagWithSetback();
                alert("");
            } else {
                reward();
            }
        }
    }
}