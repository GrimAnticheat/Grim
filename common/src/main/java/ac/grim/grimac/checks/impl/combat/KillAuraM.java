package ac.grim.grimac.checks.impl.combat;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;

@CheckData(name = "KillAuraM", description = "Attack burst immediately after tick start repeatedly", decay = 0.02, experimental = true)
public class KillAuraM extends Check implements PostPredictionCheck {
    private int packetsIntoTick;
    private int earlyAttackStreak;

    public KillAuraM(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (isTickPacket(event.getPacketType())) {
            packetsIntoTick = 0;
            return;
        }

        packetsIntoTick++;
        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity interact = new WrapperPlayClientInteractEntity(event);
            if (interact.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK && packetsIntoTick <= 2) {
                if (++earlyAttackStreak > 9) {
                    flagAndAlert("streak=" + earlyAttackStreak + ", packets=" + packetsIntoTick);
                }
            } else {
                earlyAttackStreak = Math.max(0, earlyAttackStreak - 1);
                reward();
            }
        }
    }
}
