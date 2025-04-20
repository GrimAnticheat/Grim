package ac.grim.grimac.platform.fabric.mc1214;

import ac.grim.grimac.platform.fabric.mc1194.GrimACFabric1194LoaderPlugin;
import ac.grim.grimac.platform.fabric.mc1194.entity.Fabric1194GrimEntity;
import ac.grim.grimac.platform.fabric.mc1194.player.Fabric1913PlatformInventory;
import ac.grim.grimac.platform.fabric.mc1205.Fabric1203PlatformServer;
import ac.grim.grimac.platform.fabric.mc1205.convert.Fabric1200MessageUtil;
import ac.grim.grimac.platform.fabric.mc1205.convert.Fabric1205ConversionUtil;
import ac.grim.grimac.platform.fabric.mc1214.player.Fabric1212PlatformPlayer;
import ac.grim.grimac.platform.fabric.player.FabricPlatformPlayerFactory;
import com.github.retrooper.packetevents.manager.server.ServerVersion;

public class GrimACFabric1212LoaderPlugin extends GrimACFabric1194LoaderPlugin {

    public GrimACFabric1212LoaderPlugin() {
        super(
                new FabricPlatformPlayerFactory(
                        Fabric1212PlatformPlayer::new,
                        Fabric1194GrimEntity::new,
                        Fabric1913PlatformInventory::new
                ),
                new Fabric1203PlatformServer(),
                new Fabric1200MessageUtil(),
                new Fabric1205ConversionUtil()
        );
    }

    @Override
    public ServerVersion getNativeVersion() {
        return ServerVersion.V_1_21_4;
    }
}
