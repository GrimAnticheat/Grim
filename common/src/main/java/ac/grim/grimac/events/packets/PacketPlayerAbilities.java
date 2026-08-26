package ac.grim.grimac.events.packets;

import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.PacketHandlerRegistry;
import ac.grim.grimac.checks.type.PacketReceiveListener;
import ac.grim.grimac.checks.type.PacketSendListener;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerAbilities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerAbilities;
import org.jetbrains.annotations.NotNull;

// The client can send ability packets out of order due to Mojang's excellent netcode design.
// We must delay the second ability packet until the tick after the first is received
// Else the player will fly for a tick, and we won't know about it, which is bad.
public class PacketPlayerAbilities extends Check implements PacketReceiveListener, PacketSendListener {

    private boolean lastSentPlayerCanFly = false;
    private int maxFlyingPing = 1000;

    public PacketPlayerAbilities(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_ABILITIES) {
            WrapperPlayClientPlayerAbilities abilities = new WrapperPlayClientPlayerAbilities(event);
            player.isFlying = abilities.isFlying() && player.canFly;
        }
    }

    @Override
    public void registerSend(@NotNull PacketHandlerRegistry<PacketSendEvent> registry) {
        registry.registerHandler(event -> {
            WrapperPlayServerPlayerAbilities abilities = new WrapperPlayServerPlayerAbilities(event);
            player.sendTransaction();

            if (lastSentPlayerCanFly && !abilities.isFlightAllowed()) {
                int noFlying = player.lastTransactionSent.get();
                if (maxFlyingPing != -1) {
                    player.runNettyTaskInMs(() -> {
                        if (player.lastTransactionReceived.get() < noFlying) {
                            player.getSetbackTeleportUtil().executeViolationSetback();
                        }
                    }, maxFlyingPing);
                }
            }

            lastSentPlayerCanFly = abilities.isFlightAllowed();

            player.latencyUtils.addRealTimeTask(player.lastTransactionSent.get(), () -> {
                player.canFly = abilities.isFlightAllowed();
                player.isFlying = abilities.isFlying();
            });
        }, PacketType.Play.Server.PLAYER_ABILITIES);
    }

    @Override
    public void onReload(@NotNull ConfigManager config) {
        maxFlyingPing = config.getIntElse("max-ping-out-of-flying", 1000);
    }

}
