package ac.grim.grimac.events.packets;

import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.checks.GrimProcessor;
import ac.grim.grimac.checks.packethandler.PacketHandlerRegistryBuilder;
import ac.grim.grimac.checks.type.PacketListener;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerAbilities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerAbilities;
import org.jetbrains.annotations.NotNull;

// The client can send ability packets out of order due to Mojang's excellent netcode design.
// We must delay the second ability packet until the tick after the first is received
// Else the player will fly for a tick, and we won't know about it, which is bad.
public class PacketPlayerAbilities extends GrimProcessor implements PacketListener {

    private boolean lastSentPlayerCanFly = false;
    private int maxFlyingPing = 1000;

    public PacketPlayerAbilities(GrimPlayer player) {
        super(player);
    }

    @Override
    public void registerPacketHandlers(@NotNull PacketHandlerRegistryBuilder registry) {
        registry.registerReceiveHandler((event, wrapper) -> {
            WrapperPlayClientPlayerAbilities abilities = wrapper.get();
            player.isFlying = abilities.isFlying() && player.canFly;
        }, PacketType.Play.Client.PLAYER_ABILITIES);

        registry.registerSendHandler((event, wrapper) -> {
            WrapperPlayServerPlayerAbilities abilities = wrapper.get();
            player.sendTransaction();

            int transaction = player.lastTransactionSent.get();

            if (lastSentPlayerCanFly && !abilities.isFlightAllowed()) {
                if (maxFlyingPing != -1) {
                    player.runNettyTaskInMs(() -> {
                        if (player.lastTransactionReceived.get() < transaction) {
                            player.getSetbackTeleportUtil().executeViolationSetback();
                        }
                    }, maxFlyingPing);
                }
            }

            lastSentPlayerCanFly = abilities.isFlightAllowed();

            player.latencyUtils.addRealTimeTask(transaction, () -> {
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
