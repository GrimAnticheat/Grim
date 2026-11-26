package ac.grim.grimac.utils.latency;

import ac.grim.grimac.checks.GrimProcessor;
import ac.grim.grimac.checks.type.PreViaPacketSendListener;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerCamera;
import lombok.Getter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CompensatedCameraEntity extends GrimProcessor implements PreViaPacketSendListener {
    private @NotNull PacketEntity camera;
    private @Nullable PacketEntity splitCamera;

    @Getter
    private boolean isSelf;

    public CompensatedCameraEntity(@NotNull GrimPlayer player) {
        super(player);
        reset();
    }

    @Override
    public void onPreViaPacketSend(@NotNull PacketSendEvent event) {
        if (event.getPacketType() != PacketType.Play.Server.CAMERA) return;
        int camera = new WrapperPlayServerCamera(event).getCameraId();
        player.sendTransaction();

        player.addRealTimeTaskNow(() -> {
            PacketEntity entity = player.compensatedEntities.getEntity(camera);
            if (entity == null || entity == this.camera) return;
            this.splitCamera = this.camera;
            this.camera = entity;
            update();
        });

        player.addRealTimeTaskNext(() -> {
            if (this.splitCamera == null) return;
            this.splitCamera = null;
            update();
        });
    }

    @Contract(mutates = "this")
    public void reset() {
        this.camera = player.compensatedEntities.self;
        this.splitCamera = null;
        update();
    }

    public @Nullable PacketEntity getSinglePossibilityOrNull() {
        return splitCamera == null ? camera : null;
    }

    public PacketEntity[] getPossibilities() {
        return splitCamera == null
                ? new PacketEntity[] { camera }
                : new PacketEntity[] { camera, splitCamera};
    }

    @Contract(mutates = "this")
    private void update() {
        PacketEntity self = player.compensatedEntities.self;
        isSelf = camera == self && (splitCamera == null || splitCamera == self);
    }
}
