package ac.grim.grimac.checks.impl.combat;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.packethandler.PacketHandlerRegistryBuilder;
import ac.grim.grimac.checks.type.PacketListener;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientAttack;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientSpectateEntity;
import org.jetbrains.annotations.NotNull;

@CheckData(name = "SelfInteract", stableKey = "grim.badpackets.self_hit", description = "Interacted with self")
public class SelfInteract extends Check implements PacketListener {
    public SelfInteract(GrimPlayer player) {
        super(player);
    }

    @Override
    public void registerPacketHandlers(@NotNull PacketHandlerRegistryBuilder registry) {
        registry.registerPreViaReceiveHandler(event -> {
            WrapperPlayClientInteractEntity packet = new WrapperPlayClientInteractEntity(event);
            onInteract(event, packet.getEntityId());
        }, PacketType.Play.Client.INTERACT_ENTITY);

        registry.registerPreViaReceiveHandler(event -> {
            WrapperPlayClientAttack packet = new WrapperPlayClientAttack(event);
            onInteract(event, packet.getEntityId());
        }, PacketType.Play.Client.ATTACK);

        registry.registerPreViaReceiveHandler(event -> {
            WrapperPlayClientSpectateEntity packet = new WrapperPlayClientSpectateEntity(event);
            onInteract(event, packet.getEntityId());
        }, PacketType.Play.Client.SPECTATE_ENTITY);
    }

    // TODO: should check for camera entity id instead of player entity id?
    private void onInteract(PacketReceiveEvent event, int entityId) {
        if (player.cameraEntity.isSelf() && entityId == player.entityID
                && flag() && shouldModifyPackets()) { // Instant ban
            event.setCancelled(true);
            player.onPacketCancel();
        }
    }
}
