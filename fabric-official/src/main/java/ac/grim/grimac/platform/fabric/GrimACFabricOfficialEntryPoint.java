package ac.grim.grimac.platform.fabric;

import ac.grim.grimac.platform.fabric.loader.Grim26ChainEntryPoint;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

import java.util.List;
import java.util.logging.Logger;

// Scaffold entrypoint for the 26.X branch. Discovers grim26ChainLoad entrypoints so
// the chain-loader contract participates structurally, but does NOT bring up the
// full GrimAC platform — that requires MC-typed sources (CommandSourceStack,
// MinecraftServer, ServerLifecycleEvents) which need real 26.X mappings to compile.
public class GrimACFabricOfficialEntryPoint implements PreLaunchEntrypoint, ModInitializer {

    private static final Logger LOGGER = Logger.getLogger("grimac-fabric-official");

    @Override
    public void onPreLaunch() {
    }

    @Override
    public void onInitialize() {
        FabricLoader loader = FabricLoader.getInstance();
        List<Grim26ChainEntryPoint> entries =
                loader.getEntrypoints("grim26ChainLoad", Grim26ChainEntryPoint.class);
        entries.sort((a, b) ->
                b.getNativeVersion().getProtocolVersion() - a.getNativeVersion().getProtocolVersion());

        LOGGER.info("GrimAC (fabric-official) initialized in scaffold mode on "
                + loader.getEnvironmentType() + "; chain participants registered: " + entries.size()
                + ". Anticheat is inactive on 26.X until per-version sources land.");
    }
}
