package ac.grim.grimac.checks.impl.combat;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;

@CheckData(name = "KillAuraG", description = "Sustained one-target spam without look variation", decay = 0.02, experimental = true)
public class KillAuraG extends Check implements PostPredictionCheck {
    private int lastEntity = Integer.MIN_VALUE;
    private int sameTargetStreak;
    private float lastYaw;

    public KillAuraG(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.INTERACT_ENTITY) {
            return;
        }

        WrapperPlayClientInteractEntity interact = new WrapperPlayClientInteractEntity(event);
        if (interact.getAction() != WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
            return;
        }

        int entity = interact.getEntityId();
        if (entity == lastEntity && Math.abs(player.yaw - lastYaw) < 0.005F) {
            if (++sameTargetStreak > 16) {
                flagAndAlert("entity=" + entity + ", streak=" + sameTargetStreak);
            }
        } else {
            sameTargetStreak = Math.max(0, sameTargetStreak - 1);
            reward();
        }

        lastEntity = entity;
        lastYaw = player.yaw;
    }
}
