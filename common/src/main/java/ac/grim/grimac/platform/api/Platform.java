package ac.grim.grimac.platform.api;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum Platform {

    FABRIC("fabric"),
    BUKKIT("bukkit"),
    FOLIA("folia");

    @Getter private final String name;

}
