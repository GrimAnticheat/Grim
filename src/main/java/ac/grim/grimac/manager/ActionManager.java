package ac.grim.grimac.manager;

import ac.grim.grimac.checks.AbstractPacketCheck;
import ac.grim.grimac.checks.PacketHandlerRegistry;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import lombok.Getter;

@Getter
public class ActionManager extends AbstractPacketCheck {
    private boolean attacking = false;
    private long lastAttack = 0;

    public ActionManager(GrimPlayer player) {
        super(player);
    }

    @Override
    protected void registerReceiveHandlers(PacketHandlerRegistry<PacketReceiveEvent> registry) {
        registry.registerHandler(event -> {
            if (!isTickPacketIncludingNonMovement(event.getPacketType())) return;
            WrapperPlayClientInteractEntity action = new WrapperPlayClientInteractEntity(event);
            if (action.getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
                player.totalFlyingPacketsSent = 0;
                attacking = true;
                lastAttack = System.currentTimeMillis();
            }
        }, PacketType.Play.Client.INTERACT_ENTITY);
        registry.registerHandler(event -> {
            player.totalFlyingPacketsSent++;
            attacking = false;
        });
    }

    public boolean hasAttackedSince(long time) {
        return System.currentTimeMillis() - lastAttack < time;
    }
}
