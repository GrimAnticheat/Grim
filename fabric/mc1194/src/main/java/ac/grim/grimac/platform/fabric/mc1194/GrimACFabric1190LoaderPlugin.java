package ac.grim.grimac.platform.fabric.mc1194;

import ac.grim.grimac.platform.fabric.AbstractFabricPlatformServer;
import ac.grim.grimac.platform.api.manager.CommandAdapter;
import ac.grim.grimac.platform.fabric.mc1161.command.Fabric1161PlayerSelectorAdapter;
import ac.grim.grimac.platform.fabric.command.FabricPlayerSelectorParser;
import ac.grim.grimac.platform.fabric.manager.FabricParserDescriptorFactory;
import ac.grim.grimac.platform.fabric.mc1171.GrimACFabric1170LoaderPlugin;
import ac.grim.grimac.platform.fabric.mc1171.player.Fabric1170PlatformPlayer;
import ac.grim.grimac.platform.fabric.mc1194.convert.Fabric1190MessageUtil;
import ac.grim.grimac.platform.fabric.mc1194.entity.Fabric1194GrimEntity;
import ac.grim.grimac.platform.fabric.mc1194.player.Fabric1193PlatformInventory;
import ac.grim.grimac.platform.fabric.mc1161.player.Fabric1161PlatformInventory;
import ac.grim.grimac.platform.fabric.mc1161.util.convert.Fabric1140ConversionUtil;
import ac.grim.grimac.platform.fabric.player.FabricPlatformPlayerFactory;
import ac.grim.grimac.platform.fabric.utils.convert.IFabricConversionUtil;
import ac.grim.grimac.platform.fabric.utils.message.IFabricMessageUtil;
import ac.grim.grimac.utils.lazy.LazyHolder;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;


public class GrimACFabric1190LoaderPlugin extends GrimACFabric1170LoaderPlugin {

    public GrimACFabric1190LoaderPlugin() {
        this(
                () -> new FabricParserDescriptorFactory(
                    new FabricPlayerSelectorParser<>(Fabric1161PlayerSelectorAdapter::new)
            ),
            new FabricPlatformPlayerFactory(
                    Fabric1170PlatformPlayer::new,
                    Fabric1194GrimEntity::new,
                    PacketEvents.getAPI().getServerManager().getVersion().isNewerThan(ServerVersion.V_1_19_2)
                            ? Fabric1193PlatformInventory::new : Fabric1161PlatformInventory::new
            ),
            new Fabric1190PlatformServer(),
            new Fabric1190MessageUtil(),
            new Fabric1140ConversionUtil()
        );
    }

    protected GrimACFabric1190LoaderPlugin(
            LazyHolder<CommandAdapter> parserDescriptorFactory,
            FabricPlatformPlayerFactory platformPlayerFactory,
            AbstractFabricPlatformServer platformServer,
            IFabricMessageUtil fabricMessageUtil,
            IFabricConversionUtil fabricConversionUtil) {
        super(parserDescriptorFactory, platformPlayerFactory, platformServer, fabricMessageUtil, fabricConversionUtil);
    }

    @Override
    public ServerVersion getNativeVersion() {
        return ServerVersion.V_1_19_4;
    }
}
