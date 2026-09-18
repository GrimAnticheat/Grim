package ac.grim.grimac.platform.fabric.command;

import ac.grim.grimac.command.CloudCommandService;
import ac.grim.grimac.platform.api.command.CommandService;
import ac.grim.grimac.platform.api.manager.cloud.CloudPlatformCommandArguments;
import ac.grim.grimac.platform.api.sender.Sender;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.SenderMapper;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.fabric.FabricServerCommandManager;
import org.jetbrains.annotations.NotNull;

public final class FabricCommandServiceFactory {

    private FabricCommandServiceFactory() {
    }

    public static CommandService create(CloudPlatformCommandArguments commandArguments) {
        @SuppressWarnings({"rawtypes", "unchecked"})
        CommandManager<@NotNull Sender> manager = new FabricServerCommandManager(
                ExecutionCoordinator.simpleCoordinator(),
                SenderMapper.identity()
        );
        return new CloudCommandService(() -> manager, commandArguments);
    }
}
