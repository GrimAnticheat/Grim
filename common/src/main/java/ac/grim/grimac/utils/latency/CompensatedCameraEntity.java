package ac.grim.grimac.utils.latency;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerCamera;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public class CompensatedCameraEntity extends Check implements PacketCheck {
    private final ArrayDeque<PacketEntity> entities = new ArrayDeque<>(1);

    public CompensatedCameraEntity(GrimPlayer player) {
        super(player);
        entities.add(player.compensatedEntities.self);
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() != PacketType.Play.Server.CAMERA) return;
        int camera = new WrapperPlayServerCamera(event).getCameraId();
        player.sendTransaction();

        player.addRealTimeTaskNow(() -> {
            PacketEntity entity = player.compensatedEntities.getEntity(camera);
            if (entity != null) {
                entities.add(entity);
            }
        });

        player.addRealTimeTaskNext(() -> {
            while (entities.size() > 1) {
                entities.poll();
            }

            if (entities.isEmpty()) {
                entities.add(player.compensatedEntities.self);
            }
        });
    }

    public boolean isSelf() {
        PacketEntity self = player.compensatedEntities.self;
        for (PacketEntity entity : entities) {
            if (entity != self) {
                return false;
            }
        }

        return true;
    }

    public List<PacketEntity> getPossibilities() {
        return new ArrayList<>(entities);
    }
}
