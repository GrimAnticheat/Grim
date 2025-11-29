package ac.grim.grimac.platform.fabric.mc12110;

import ac.grim.grimac.platform.fabric.command.FabricPlayerSelectorParser;
import ac.grim.grimac.platform.fabric.manager.FabricParserDescriptorFactory;
import ac.grim.grimac.platform.fabric.mc1194.entity.Fabric1194GrimEntity;
import ac.grim.grimac.platform.fabric.mc1194.player.Fabric1193PlatformInventory;
import ac.grim.grimac.platform.fabric.mc1205.convert.Fabric1200MessageUtil;
import ac.grim.grimac.platform.fabric.mc1205.convert.Fabric1205ConversionUtil;
import ac.grim.grimac.platform.fabric.mc12110.player.Fabric12110PlatformPlayer;
import ac.grim.grimac.platform.fabric.mc1216.GrimACFabric1212LoaderPlugin;
import ac.grim.grimac.platform.fabric.mc1216.player.Fabric1215PlatformInventory;
import ac.grim.grimac.platform.fabric.player.FabricPlatformPlayerFactory;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;

public class GrimACFabric12110LoaderPlugin extends GrimACFabric1212LoaderPlugin {

    public GrimACFabric12110LoaderPlugin() {
        super(
                new FabricParserDescriptorFactory(
                        new FabricPlayerSelectorParser<>(ac.grim.grimac.platform.fabric.mc1216.command.Fabric1212PlayerSelectorAdapter::new)
                ),
                new FabricPlatformPlayerFactory(
                        Fabric12110PlatformPlayer::new,
                        Fabric1194GrimEntity::new,
                        PacketEvents.getAPI().getServerManager().getVersion().isNewerThan(ServerVersion.V_1_21_4)
                                ? Fabric1215PlatformInventory::new : Fabric1193PlatformInventory::new
                ),
                new Fabric12110PlatfromServer(),
                new Fabric1200MessageUtil(),
                PacketEvents.getAPI().getServerManager().getVersion().isNewerThan(ServerVersion.V_1_21_5)
                        ? new ac.grim.grimac.platform.fabric.mc1216.convert.Fabric1216ConversionUtil() : new Fabric1205ConversionUtil()
        );
    }

    @Override
    public ServerVersion getNativeVersion() {
        return ServerVersion.V_1_21_10;
    }
}
