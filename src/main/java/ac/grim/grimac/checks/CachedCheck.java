package ac.grim.grimac.checks;

import ac.grim.grimac.AbstractCheck;
import ac.grim.grimac.manager.CheckManager;
import ac.grim.grimac.manager.LastInstanceManager;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.LambdaUtils;
import ac.grim.grimac.utils.latency.CompensatedFireworks;
import lombok.Getter;

import java.util.function.Function;

public class CachedCheck<T extends AbstractCheck> {
    @Getter
    private boolean isImportant, isDisabled;
    private Function<GrimPlayer, ? extends T> instanceFunction;
    @Getter
    private CheckCategory<T> checkCategory;
    @Getter
    private String permission, checkName, configName;

    public CachedCheck(Class<? extends T> aClass, boolean isImportant) throws Throwable {
        this.isImportant = isImportant;
        if (aClass == CompensatedFireworks.class) {
            this.instanceFunction = player -> (T) player.compensatedFireworks;
        } else if (aClass == LastInstanceManager.class) {
            this.instanceFunction = player -> (T) player.lastInstanceManager;
        } else {
            this.instanceFunction = LambdaUtils.createArgConstructorForClass(aClass, GrimPlayer.class);
        }
        if (aClass.isAnnotationPresent(CheckData.class)) {
            CheckData checkData = aClass.getAnnotation(CheckData.class);
            this.checkName = checkData.name();
            this.permission = "grimac.bypass." + this.checkName.toLowerCase(); //grimac.bypass.simulation etc.
            this.configName = checkData.configName();
        } else {
            this.checkName = aClass.getSimpleName();
        }
    }

    public CachedCheck(Class<? extends T> aClass) throws Throwable {
        this(aClass, false);
    }

    public CachedCheck<T> checkCategory(CheckCategory<T> checkCategory) {
        this.checkCategory = checkCategory;
        return this;
    }

    public void setDisabled(boolean disabled) {
        this.isDisabled = disabled;
    }

    public T apply(GrimPlayer grimPlayer) {
        return this.instanceFunction.apply(grimPlayer);
    }
}
