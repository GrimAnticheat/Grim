package ac.grim.grimac.platform.fabric;

import ac.grim.grimac.platform.api.PlatformPlugin;
import net.fabricmc.loader.api.ModContainer;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class FabricPlatformPlugin implements PlatformPlugin {
    private final @NotNull ModContainer modContainer;

    @Contract(pure = true)
    public FabricPlatformPlugin(@NotNull ModContainer modContainer) {
        this.modContainer = Objects.requireNonNull(modContainer);
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
