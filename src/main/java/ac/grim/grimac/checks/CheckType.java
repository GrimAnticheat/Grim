package ac.grim.grimac.checks;

import org.apache.commons.lang.StringUtils;

public enum CheckType {

    MOVEMENT,
    PREDICTION,
    ROTATION,
    COMBAT,
    PACKETS,
    WORLD,
    OTHER;

    public String displayName(){
        String loweredEnumName = this.name().toLowerCase();
        return StringUtils.capitalize(loweredEnumName);
    }

}
