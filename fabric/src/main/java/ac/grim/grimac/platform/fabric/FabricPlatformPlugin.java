package ac.grim.grimac.platform.fabric;

import ac.grim.grimac.platform.api.PlatformPlugin;
import net.fabricmc.loader.api.ModContainer;

public class FabricPlatformPlugin implements PlatformPlugin {
    private final ModContainer modContainer;

    public FabricPlatformPlugin(ModContainer modContainer) {
        this.modContainer = modContainer;
    }

    @Override
    public boolean isEnabled() {
        // Fabric mods are always "enabled" if loaded, as there's no explicit enable/disable state
        // You can add custom logic if needed (e.g., check mod configuration)
        return true;
    }

    @Override
    public String getName() {
        // Get the mod ID (unique identifier)
        return modContainer.getMetadata().getId();
    }

    @Override
    public String getVersion() {
        // Get the mod version from metadata
        return modContainer.getMetadata().getVersion().getFriendlyString();
    }
}
