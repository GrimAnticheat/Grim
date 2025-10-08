package ac.grim.grimac.manager.player.features.types;

import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.api.feature.FeatureState;
import ac.grim.grimac.player.GrimPlayer;

public class ForceStuckSpeedFeature implements GrimFeature {

    @Override
    public String getName() {
        return "ForceStuckSpeed";
    }

    @Override
    public void setState(GrimPlayer player, ConfigManager config, FeatureState state) {
        switch (state) {
            case ENABLED -> player.setForceStuckSpeed(true);
            case DISABLED -> player.setForceStuckSpeed(false);
            default -> player.setForceStuckSpeed(isEnabledInConfig(player, config));
        }
    }

    @Override
    public boolean isEnabled(GrimPlayer player) {
        return player.isForceStuckSpeed();
    }

    @Override
    public boolean isEnabledInConfig(GrimPlayer player, ConfigManager config) {
        return config.getBooleanElse("force-stuck-speed", true);
    }

}
