package ac.grim.grimac.checks;

import org.apache.commons.lang3.StringUtils;

public enum CheckType {
    MOVEMENT,
    ROTATION,
    COMBAT,
    PACKETS,
    WORLD,
    OTHER;

    @Override
    public String toString() {
        return StringUtils.capitalize(this.name().toLowerCase());
    }

}
