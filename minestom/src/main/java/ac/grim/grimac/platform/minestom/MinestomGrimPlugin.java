package ac.grim.grimac.platform.minestom;

import ac.grim.grimac.api.plugin.GrimPlugin;
import ac.grim.grimac.api.plugin.GrimPluginDescription;

import java.io.File;
import java.util.logging.Logger;

/**
 * Minestom-backed {@link GrimPlugin} — Grim's notion of "the plugin" on a platform with no
 * plugin container. Backed by a JUL logger and a working-directory data folder.
 */
public final class MinestomGrimPlugin implements GrimPlugin {

    private final GrimPluginDescription description = new MinestomGrimPluginDescription();
    private final Logger logger = Logger.getLogger("Grim");
    private final File dataFolder = new File("grim");

    @Override
    public GrimPluginDescription getDescription() {
        return description;
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public File getDataFolder() {
        return dataFolder;
    }
}
