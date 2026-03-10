package ac.grim.grimac.checks.impl.combat;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;

@CheckData(name = "KillAuraD", description = "Entity switching without any rotation change", decay = 0.02, experimental = true)
public class KillAuraD extends Check implements PostPredictionCheck {
    private int lastEntity = Integer.MIN_VALUE;
    private float lastYaw;
    private float lastPitch;
    private int streak;

    public KillAuraD(GrimPlayer player) {
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

        final int entityId = interact.getEntityId();
        if (entityId != lastEntity && lastEntity != Integer.MIN_VALUE) {
            final boolean sameYaw = Math.abs(player.yaw - lastYaw) < 1.0E-4F;
            final boolean samePitch = Math.abs(player.pitch - lastPitch) < 1.0E-4F;
            if (sameYaw && samePitch && ++streak > 6) {
                flagAndAlert("entity=" + entityId + ", last=" + lastEntity + ", streak=" + streak);
            } else if (!sameYaw || !samePitch) {
                streak = Math.max(0, streak - 1);
                reward();
            }
        }

        lastEntity = entityId;
        lastYaw = player.yaw;
        lastPitch = player.pitch;
    }
}
