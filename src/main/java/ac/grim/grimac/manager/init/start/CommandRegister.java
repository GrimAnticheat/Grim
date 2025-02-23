package ac.grim.grimac.manager.init.start;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.commands.*;
import ac.grim.grimac.manager.init.Initable;
import ac.grim.grimac.platform.api.sender.Sender;
import org.bukkit.Bukkit;
import org.incendo.cloud.CommandManager;

import java.util.function.Supplier;

public class CommandRegister implements Initable {

    private final Supplier<CommandManager<Sender>> commandManagerSupplier;

    public CommandRegister(Supplier<CommandManager<Sender>> commandManagerSupplier) {
        this.commandManagerSupplier = commandManagerSupplier;
    }

    @Override
    public void start() {
        // This does not make Grim require paper
        // It only enables new features such as asynchronous tab completion on paper
        CommandManager<Sender> commandManager = commandManagerSupplier.get();

        new GrimPerf().register(commandManager);
        new GrimDebug().register(commandManager);
        new GrimAlerts().register(commandManager);
        new GrimProfile().register(commandManager);
        new GrimSendAlert().register(commandManager);
        new GrimHelp().register(commandManager);
        new GrimReload().register(commandManager);
        new GrimSpectate().register(commandManager);
        new GrimStopSpectating().register(commandManager);
        new GrimLog().register(commandManager);
        new GrimVerbose().register(commandManager);
        new GrimVersion().register(commandManager);
        new GrimDump().register(commandManager);
        new GrimBrands().register(commandManager);

//        commandManager.getCommandCompletions().registerCompletion("stopspectating", GrimStopSpectating.completionHandler);

        if (GrimAPI.INSTANCE.getConfigManager().getConfig().getBooleanElse("check-for-updates", true)) {
            GrimVersion.checkForUpdatesAsync(null);
        }
    }
}
