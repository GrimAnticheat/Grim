package ac.grim.grimac.utils.viaversion;

import ac.grim.grimac.utils.anticheat.LogUtil;
import ac.grim.grimac.utils.reflection.ReflectionUtils;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ViaVersionUtil {
    public static final boolean isAvailable = ReflectionUtils.hasClass("com.viaversion.viaversion.api.Via");
    public static final boolean hasViaBackwards = ViaVersionUtil.isAvailable && ReflectionUtils.hasClass("com.viaversion.viabackwards.ViaBackwards");

    static {
        if (!isAvailable && ReflectionUtils.hasClass("us.myles.ViaVersion.api.Via")) {
            LogUtil.error("Using unsupported ViaVersion 4.0 API, update ViaVersion to 5.0");
        }
    }

    public static void injectHooks() {
        if (isAvailable) ViaVersionHooks.load();
    }
}
