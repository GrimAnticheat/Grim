package ac.grim.grimac.checks.impl.combat;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;

@CheckData(name = "KillAuraJ", description = "Attack cadence locked to narrow intervals", decay = 0.02, experimental = true)
public class KillAuraJ extends Check implements PostPredictionCheck {
    private long lastAttack;
    private int samples;
    private int narrow;

    public KillAuraJ(GrimPlayer player) {
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

        long now = System.currentTimeMillis();
        if (lastAttack != 0L) {
            long interval = now - lastAttack;
            samples++;
            if (interval >= 95 && interval <= 105) {
                narrow++;
            }
            if (samples >= 15) {
                if (narrow >= 13) {
                    flagAndAlert("narrow=" + narrow + "/" + samples);
                } else {
                    reward();
                }
                samples = 0;
                narrow = 0;
            }
        }
        lastAttack = now;
    }
}
