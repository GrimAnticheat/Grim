package ac.grim.grimac.checks.impl.combat;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;

@CheckData(name = "KillAuraC", description = "Unnaturally consistent attack timing", decay = 0.02, experimental = true)
public class KillAuraC extends Check implements PostPredictionCheck {
    private long lastAttackTime;
    private int samples;
    private int consistentIntervals;

    public KillAuraC(GrimPlayer player) {
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

        final long now = System.currentTimeMillis();
        if (lastAttackTime != 0L) {
            final long interval = now - lastAttackTime;
            if (interval >= 45 && interval <= 65) {
                consistentIntervals++;
            }
            samples++;

            if (samples >= 12) {
                if (consistentIntervals >= 10) {
                    flagAndAlert("consistent=" + consistentIntervals + "/" + samples);
                } else {
                    reward();
                }
                samples = 0;
                consistentIntervals = 0;
            }
        }

        lastAttackTime = now;
    }
}
