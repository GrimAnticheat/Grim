package ac.grim.grimac.manager.init.start;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.commands.GrimDebug;
import ac.grim.grimac.commands.GrimPerf;
import ac.grim.grimac.manager.init.Initable;
import co.aikar.commands.PaperCommandManager;

public class CommandRegister implements Initable {
    @Override
    public void start() {
        // This does not make Grim require paper
        // It only enables new features such as asynchronous tab completion on paper
        PaperCommandManager commandManager = new PaperCommandManager(GrimAPI.INSTANCE.getPlugin());

        commandManager.enableUnstableAPI("brigadier");

        commandManager.registerCommand(new GrimPerf());
        commandManager.registerCommand(new GrimDebug());
    }
}
