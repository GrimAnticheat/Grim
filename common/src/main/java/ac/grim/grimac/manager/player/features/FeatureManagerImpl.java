package ac.grim.grimac.manager.player.features;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.api.feature.FeatureManager;
import ac.grim.grimac.api.feature.FeatureState;
import ac.grim.grimac.manager.player.features.types.ExemptElytraFeature;
import ac.grim.grimac.manager.player.features.types.ExperimentalChecksFeature;
import ac.grim.grimac.manager.player.features.types.GrimFeature;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.common.ConfigReloadObserver;
import com.google.common.collect.ImmutableSet;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class FeatureManagerImpl implements FeatureManager, ConfigReloadObserver {

    static {
        FeatureBuilder builder = new FeatureBuilder();
        builder.register(new ExperimentalChecksFeature());
        builder.register(new ExemptElytraFeature());
        FEATURES = builder.buildMap();
    }

    @Getter
    private static final Map<String, GrimFeature> FEATURES;

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
