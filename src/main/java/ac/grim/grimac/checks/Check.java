package ac.grim.grimac.checks;

import ac.grim.grimac.GrimAC;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// Inspired heavily by https://github.com/HawkAnticheat/Hawk/blob/master/src/me/islandscout/hawk/check/Check.java
public class Check {
    protected static GrimAC grim;
    protected final Map<UUID, Long> lastFlagTimes = new HashMap<>();
    // TODO: Write the base check class
    protected boolean enabled;
    protected int cancelThreshold;
    protected int flagThreshold;
    protected double vlPassMultiplier;
    protected long flagCooldown; //in milliseconds
    protected String permission;
    protected String name;
    protected String configPath;
    protected String flag;
    protected List<String> punishCommands;

    /**
     * Default values set in these constructors. Configuration may override them.
     *
     * @param name             name of check
     * @param enabled          enable check
     * @param cancelThreshold  VL required to cancel
     * @param flagThreshold    VL required to flag
     * @param vlPassMultiplier VL pass multiplier (eg: 0.95)
     * @param flagCooldown     flag cooldown duration (in milliseconds)
     * @param flag             flag message
     * @param punishCommands   list of commands to run
     */
    /*Check(String name, boolean enabled, int cancelThreshold, int flagThreshold, double vlPassMultiplier, long flagCooldown, String flag, List<String> punishCommands) {

    }*/
}
