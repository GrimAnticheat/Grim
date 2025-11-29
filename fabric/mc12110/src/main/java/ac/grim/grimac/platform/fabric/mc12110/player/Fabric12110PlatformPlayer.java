package ac.grim.grimac.platform.fabric.mc12110.player;

import ac.grim.grimac.platform.fabric.mc12110.GrimACFabric12110MessagePayload;
import ac.grim.grimac.platform.fabric.mc1216.player.Fabric1212PlatformPlayer;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class Fabric12110PlatformPlayer extends Fabric1212PlatformPlayer {

    public Fabric12110PlatformPlayer(ServerPlayer player) {
        super(player);
    }

    @Override
    public void sendPluginMessage(String channelName, byte[] message) {
        if (channelName.equals("BungeeCord")) {
            channelName = "bungeecord:main";
        }

        ClientboundCustomPayloadPacket packet = new ClientboundCustomPayloadPacket(new GrimACFabric12110MessagePayload(
                ResourceLocation.tryParse(channelName),
                message
        ));
        this.fabricPlayer.connection.send(packet);
    }
}
