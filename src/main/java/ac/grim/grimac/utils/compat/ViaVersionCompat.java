package ac.grim.grimac.utils.compat;

public class ViaVersionCompat {
    public static boolean hasViaVersion;

    static {
        try {
            Class.forName("com.viaversion.viaversion.api.Via");
            hasViaVersion = true;
        } catch (ClassNotFoundException e) {
            hasViaVersion = false;
        }
    }
}
