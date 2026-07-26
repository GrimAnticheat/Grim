package ac.grim.grimac.platform.minestom;

import ac.grim.grimac.api.plugin.GrimPluginDescription;

import java.util.Collection;
import java.util.List;

/** Static plugin metadata for the Minestom platform. */
public final class MinestomGrimPluginDescription implements GrimPluginDescription {

    @Override
    public String getVersion() {
        return "minestom";
    }

    @Override
    public String getDescription() {
        return "GrimAC anticheat on Minestom (OnThePixel port)";
    }

    @Override
    public Collection<String> getAuthors() {
        return List.of("GrimAC", "OnThePixel");
    }
}
