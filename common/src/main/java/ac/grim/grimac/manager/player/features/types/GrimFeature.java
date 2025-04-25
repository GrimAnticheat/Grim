package ac.grim.grimac.manager.player.features.types;

import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.api.feature.FeatureState;
import ac.grim.grimac.player.GrimPlayer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public abstract class GrimFeature {

    private final String name;

    public abstract void setState(GrimPlayer player, ConfigManager config, FeatureState state);

    public abstract boolean isEnabled(GrimPlayer player);

    public abstract boolean isEnabledInConfig(GrimPlayer player, ConfigManager config);

}
