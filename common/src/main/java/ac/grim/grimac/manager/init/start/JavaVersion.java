package ac.grim.grimac.manager.init.start;

import ac.grim.grimac.manager.init.Initable;
import ac.grim.grimac.utils.anticheat.LogUtil;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JavaVersion implements Initable {

    @Override
    public void start() {
        // Stolen from Via, stolen from Paper
        String javaVersion = System.getProperty("java.version");
        Matcher matcher = Pattern.compile("(?:1\\.)?(\\d+)").matcher(javaVersion);
        if (!matcher.find()) {
            LogUtil.error("Failed to determine Java version; could not parse: " + javaVersion);
            return;
        }

        String versionString = matcher.group(1);
        int version;
        try {
            version = Integer.parseInt(versionString);
        } catch (NumberFormatException e) {
            LogUtil.error("Failed to determine Java version; could not parse: " + versionString);
            e.printStackTrace();
            return;
        }

        if (version < 17) {
            LogUtil.warn("You are running an outdated Java version, please update it to at least Java 17 (your version is " + javaVersion + ").");
            LogUtil.warn("GrimAC will no longer support this version of Java in a future release.");
            LogUtil.warn("See https://github.com/GrimAnticheat/Grim/wiki/Updating-to-Java-17 for more information.");
        }
    }
}
