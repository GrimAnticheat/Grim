package ac.grim.grimac.manager.player.features;

import ac.grim.grimac.manager.player.features.types.GrimFeature;
import ac.grim.grimac.utils.anticheat.LogUtil;
import com.google.common.collect.ImmutableMap;

import java.util.regex.Pattern;

public class FeatureBuilder {

    private final ImmutableMap.Builder<String, GrimFeature> mapBuilder = ImmutableMap.builder();

    private static final Pattern VALID = Pattern.compile("[a-zA-Z0-9_]{1,64}");

    public <T extends GrimFeature> void register(T feature) {
        if (!VALID.matcher(feature.getName()).matches()) {
            LogUtil.error("Invalid feature name: " + feature.getName());
            return;
        }
        mapBuilder.put(feature.getName(), feature);
    }

    public ImmutableMap<String, GrimFeature> buildMap() {
        return mapBuilder.build();
    }

}
