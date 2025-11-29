package ac.grim.grimac.platform.fabric.mc1218;

import ac.grim.grimac.platform.fabric.mc1205.Fabric1203PlatformServer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class Fabric1218PlatformServer extends Fabric1203PlatformServer {

    @Override
    public void registerOutgoingPluginChannel(@NotNull String name) {
        if (Objects.equals(name, "BungeeCord")) {
            name = "bungeecord:main";
        }

        var key = ResourceLocation.tryParse(name);
        PayloadTypeRegistry.playS2C().register(new CustomPacketPayload.Type<>(key), GrimACFabric1218MessagePayload.codec(key));
    }
}
