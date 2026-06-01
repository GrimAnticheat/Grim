package ac.grim.grimac.platform.fabric.mc261;

import ac.grim.grimac.platform.fabric.GrimACFabricLoaderPlugin;
import ac.grim.grimac.platform.fabric.player.FabricPlatformPlayerFactory;
import com.github.retrooper.packetevents.manager.server.ServerVersion;

// Concrete chain entry for the 26.1.X family. Empirically verified mojmap-stable
// across 26.1 / 26.1.1 / 26.1.2 (zero method signature drift on 300 sampled
// classes; six critical classes — MinecraftServer, ServerPlayer, ServerLevel,
// CommandSourceStack, Player, Entity — bit-identical signatures). When 26.2
// drops a method-signature change a new mc262 sibling will be added.
public class Fabric261LoaderPlugin extends GrimACFabricLoaderPlugin {

    public Fabric261LoaderPlugin() {
        super(
                new FabricPlatformPlayerFactory(
                        Fabric261PlatformPlayer::new,
                        Fabric261GrimEntity::new,
                        Fabric261PlatformInventory::new
                ),
                new Fabric261PlatformServer(),
                new Fabric261MessageUtil(),
                new Fabric261ConversionUtil()
        );
    }

    @Override
    public ServerVersion getNativeVersion() {
        return ServerVersion.V_26_1;
    }
}
