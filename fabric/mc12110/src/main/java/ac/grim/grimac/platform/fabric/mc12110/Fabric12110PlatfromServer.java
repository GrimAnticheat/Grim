package ac.grim.grimac.platform.fabric.mc12110;

import ac.grim.grimac.platform.fabric.mc1205.Fabric1203PlatformServer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public class Fabric12110PlatfromServer extends Fabric1203PlatformServer {

    @Override
    public void registerOutgoingPluginChannel(String name) {
        if (Objects.equals(name, "BungeeCord")) {
            name = "bungeecord:main";
        }

        var key = ResourceLocation.tryParse(name);
        PayloadTypeRegistry.playS2C().register(new CustomPacketPayload.Type<>(key), GrimACFabric12110MessagePayload.CODEC);
    }
}
