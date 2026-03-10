package ac.grim.grimac.manager.player.features;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.api.feature.FeatureManager;
import ac.grim.grimac.api.feature.FeatureState;
import ac.grim.grimac.manager.player.features.types.*;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.common.ConfigReloadObserver;
import com.google.common.collect.ImmutableSet;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class FeatureManagerImpl implements FeatureManager, ConfigReloadObserver {

    private static final Map<String, GrimFeature> FEATURES;

    /// @deprecated use {@link #getFeatures()}
    @Contract(pure = true)
    @Deprecated
    public static Map<String, GrimFeature> getFEATURES() {
        return getFeatures();
    }

    @Contract(pure = true)
    public static Map<String, GrimFeature> getFeatures() {
        return FEATURES;
    }

    static {
        FeatureBuilder builder = new FeatureBuilder();
        builder.register(new ExperimentalChecksFeature());
        builder.register(new ExemptElytraFeature());
        builder.register(new ForceStuckSpeedFeature());
        builder.register(new ForceSlowMovementFeature());
        FEATURES = builder.buildMap();
    }

    private final Map<String, FeatureState> states = new HashMap<>();

    private final GrimPlayer player;

    public FeatureManagerImpl(GrimPlayer player) {
        this.player = player;
        for (GrimFeature value : FEATURES.values()) states.put(value.getName(), FeatureState.UNSET);
    }

    @Override
    public Collection<String> getFeatureKeys() {
        return ImmutableSet.copyOf(FEATURES.keySet());
    }

    @Override
    public @Nullable FeatureState getFeatureState(String key) {
        return states.get(key);
    }

    @Override
    public boolean isFeatureEnabled(String key) {
        GrimFeature feature = FEATURES.get(key);
        if (feature == null) return false;
        return feature.isEnabled(player);
    }

    @Override
    public boolean setFeatureState(String key, FeatureState tristate) {
        GrimFeature feature = FEATURES.get(key);
        if (feature == null) return false;
        states.put(key, tristate);
        return true;
    }

    @Override
    public void reload() {
        onReload(GrimAPI.INSTANCE.getExternalAPI().getConfigManager());
    }

    @Override
    public void onReload(ConfigManager config) {
        for (Map.Entry<String, FeatureState> entry : states.entrySet()) {
            String key = entry.getKey();
            FeatureState state = entry.getValue();
            GrimFeature feature = FEATURES.get(key);
            if (feature == null) continue;
            feature.setState(player, config, state);
        }
    }

}
