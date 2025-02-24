package ac.grim.grimac;

import ac.grim.grimac.api.GrimPlugin;
import ac.grim.grimac.api.GrimPluginDescription;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;
import java.util.logging.Logger;

public class BasicGrimPlugin implements GrimPlugin {
    private final Logger logger;
    private final File dataFolder;
    private final BasicGrimPluginDescription description;

    public BasicGrimPlugin(Logger logger, File dataFolder, String version, String description, List<String> authors) {
        this.logger = logger;
        this.dataFolder = dataFolder;
        this.description = new BasicGrimPluginDescription(version, description, authors);
    }

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

    private static class BasicGrimPluginDescription implements GrimPluginDescription {
        private final String version;
        private final String description;
        private final List<String> authors;

        public BasicGrimPluginDescription(String version, String description, List<String> authors) {
            this.version = version;
            this.description = description;
            this.authors = authors;
        }

        @Override
        public String getVersion() {
            return version;
        }

        @Override
        public String getDescription() {
            return description;
        }

        @Override
        public @NotNull List<String> getAuthors() {
            return authors;
        }
    }
}
