package ac.grim.grimac.manager.init.start;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.command.commands.GrimVersion;
import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.utils.anticheat.LogUtil;
import org.incendo.cloud.CommandManager;

import java.util.function.Supplier;

public record CommandLoader(Supplier<CommandManager<Sender>> commandManagerSupplier) implements StartableInitable {

    // Static helper for platforms calling this manually (Fabric EntryPoint)
    public static void load(CommandManager<Sender> manager) {
        try {
            // This method call triggers the JVM to verify CommandRegister.
            // If Cloud is missing, the NoClassDefFoundError happens HERE.
            CommandRegister.registerCommands(manager);
        } catch (NoClassDefFoundError e) {
            LogUtil.error("Cloud Command Framework is missing! Grim commands are disabled.", e);
        } catch (Throwable t) {
            LogUtil.error("Failed to register commands.", t);
        }
    }

    @Override
    public void start() {
        CommandManager<Sender> commandManager = commandManagerSupplier.get();

        // If the manager failed to create (Bukkit), don't try to register
        if (commandManager == null) return;

        load(commandManager);

        // Move the update check here so it runs safely
        if (GrimAPI.INSTANCE.getConfigManager().getConfig().getBooleanElse("check-for-updates", true)) {
            GrimVersion.checkForUpdatesAsync(GrimAPI.INSTANCE.getPlatformServer().getConsoleSender());
        }
    }
}
