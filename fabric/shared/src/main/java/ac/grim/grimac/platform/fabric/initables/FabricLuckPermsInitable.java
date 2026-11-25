package ac.grim.grimac.platform.fabric.initables;

import ac.grim.grimac.manager.init.OptionalReflectiveInitable;
import net.fabricmc.loader.api.FabricLoader;

public final class FabricLuckPermsInitable extends OptionalReflectiveInitable {
    private static final String LUCKPERMS_MOD_ID = "luckperms";
    private static final String HANDLER_CLASS =
            "ac.grim.grimac.platform.fabric.initables.FabricLuckPermsHandler";

    public FabricLuckPermsInitable() {
        super(HANDLER_CLASS, "Error when initializing LuckPerms hook");
    }

    @Override
    protected boolean isAvailable() {
        return FabricLoader.getInstance().isModLoaded(LUCKPERMS_MOD_ID);
    }
}
