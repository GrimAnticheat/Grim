package ac.grim.grimac.platform.fabric.mc1205;

import ac.grim.grimac.platform.fabric.command.FabricPlayerSelectorParser;
import ac.grim.grimac.platform.fabric.manager.FabricParserDescriptorFactory;
import ac.grim.grimac.platform.fabric.mc1194.GrimACFabric1190LoaderPlugin;
import ac.grim.grimac.platform.fabric.mc1194.player.Fabric1193PlatformInventory;
import ac.grim.grimac.platform.fabric.mc1205.convert.Fabric1200MessageUtil;
import ac.grim.grimac.platform.fabric.mc1205.convert.Fabric1205ConversionUtil;
import ac.grim.grimac.platform.fabric.mc1194.entity.Fabric1194GrimEntity;
import ac.grim.grimac.platform.fabric.mc1205.player.Fabric1202PlatformPlayer;
import ac.grim.grimac.platform.fabric.mc1161.command.Fabric1161PlayerSelectorAdapter;
import ac.grim.grimac.platform.fabric.player.FabricPlatformPlayerFactory;
import com.github.retrooper.packetevents.manager.server.ServerVersion;

public class GrimACFabric1200LoaderPlugin extends GrimACFabric1190LoaderPlugin {

    public GrimACFabric1200LoaderPlugin() {
        // This slice is 1.20.5-only (see fabric.mod.json); use the newest branches without FabricPacketEventsAPI
        // so compile stays independent of remapped packetevents-fabric package names.
        super(
                () -> new FabricParserDescriptorFactory(
                        new FabricPlayerSelectorParser<>(Fabric1161PlayerSelectorAdapter::new)
                ),
                new FabricPlatformPlayerFactory(
                        Fabric1202PlatformPlayer::new,
                        Fabric1194GrimEntity::new,
                        Fabric1193PlatformInventory::new
                ),
                new Fabric1203PlatformServer(),
                new Fabric1200MessageUtil(),
                new Fabric1205ConversionUtil()
        );
    }

    @Override
    public ServerVersion getNativeVersion() {
        return ServerVersion.V_1_20_5;
    }
}
