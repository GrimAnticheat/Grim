package ac.grim.grimac.manager.init.start;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.commands.*;
import ac.grim.grimac.manager.init.Initable;
import co.aikar.commands.PaperCommandManager;
import org.bukkit.Bukkit;
import github.scarsz.configuralize.DynamicConfig;

public class CommandRegister implements Initable {
    @Override
    public void start() {
        // This does not make Grim require paper
        // It only enables new features such as asynchronous tab completion on paper
        PaperCommandManager commandManager = new PaperCommandManager(GrimAPI.INSTANCE.getPlugin());

        commandManager.registerCommand(new GrimPerf());
        commandManager.registerCommand(new GrimDebug());
        commandManager.registerCommand(new GrimAlerts());
        commandManager.registerCommand(new GrimProfile());
        commandManager.registerCommand(new GrimSendAlert());
        commandManager.registerCommand(new GrimHelp());
        commandManager.registerCommand(new GrimReload());
        commandManager.registerCommand(new GrimSpectate());
        commandManager.registerCommand(new GrimStopSpectating());
        commandManager.registerCommand(new GrimLog());
        commandManager.registerCommand(new GrimVerbose());

        commandManager.registerCommand(new GrimVersion());
        commandManager.registerCommand(new GrimDump());
        commandManager.registerCommand(new GrimBrands());

        commandManager.getCommandCompletions().registerCompletion("stopspectating", GrimStopSpectating.completionHandler);

        if (GrimAPI.INSTANCE.getConfigManager().getConfig().getBooleanElse("check-for-updates", true)) {
            GrimVersion.checkForUpdatesAsync(Bukkit.getConsoleSender());
        }
        commandManager.registerCommand(new GrimChecks());
    }
}
