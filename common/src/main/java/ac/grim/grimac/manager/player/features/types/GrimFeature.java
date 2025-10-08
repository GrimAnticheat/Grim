package ac.grim.grimac.manager.player.features.types;

import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.api.feature.FeatureState;
import ac.grim.grimac.player.GrimPlayer;

public interface GrimFeature {
    String getName();

    void setState(GrimPlayer player, ConfigManager config, FeatureState state);

    boolean isEnabled(GrimPlayer player);

    boolean isEnabledInConfig(GrimPlayer player, ConfigManager config);
}
