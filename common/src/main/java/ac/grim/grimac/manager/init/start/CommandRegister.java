package ac.grim.grimac.manager.init.start;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.command.commands.GrimAlerts;
import ac.grim.grimac.command.commands.GrimBrands;
import ac.grim.grimac.command.commands.GrimDebug;
import ac.grim.grimac.command.commands.GrimDump;
import ac.grim.grimac.command.commands.GrimHelp;
import ac.grim.grimac.command.commands.GrimLog;
import ac.grim.grimac.command.commands.GrimPerf;
import ac.grim.grimac.command.commands.GrimProfile;
import ac.grim.grimac.command.commands.GrimReload;
import ac.grim.grimac.command.commands.GrimSendAlert;
import ac.grim.grimac.command.commands.GrimSpectate;
import ac.grim.grimac.command.commands.GrimStopSpectating;
import ac.grim.grimac.command.commands.GrimVerbose;
import ac.grim.grimac.command.commands.GrimVersion;
import ac.grim.grimac.manager.init.Initable;
import ac.grim.grimac.platform.api.sender.Sender;
import org.incendo.cloud.CommandManager;

import java.util.function.Supplier;

public class CommandRegister implements Initable {

    private static boolean commandsRegistered = false;
    private final Supplier<CommandManager<Sender>> commandManagerSupplier;

    public CommandRegister(Supplier<CommandManager<Sender>> commandManagerSupplier) {
        this.commandManagerSupplier = commandManagerSupplier;
    }

    // Public static method that can be called on platforms where command must be registered earlier than InitManager.load()
    public static void registerCommands(CommandManager<Sender> commandManager) {
        if (commandsRegistered) return;
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
        commandsRegistered = true;
    }

    @Override
    public void start() {
        // This does not make Grim require paper
        // It only enables new features such as asynchronous tab completion on paper
        CommandManager<Sender> commandManager = commandManagerSupplier.get();
        registerCommands(commandManager);

        if (GrimAPI.INSTANCE.getConfigManager().getConfig().getBooleanElse("check-for-updates", true)) {
            GrimVersion.checkForUpdatesAsync(GrimAPI.INSTANCE.getPlatformServer().getConsoleSender());
        }
    }
}
