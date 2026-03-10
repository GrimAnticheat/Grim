package ac.grim.grimac.command;

import ac.grim.grimac.command.commands.*;
import ac.grim.grimac.command.handler.GrimCommandFailureHandler;
import ac.grim.grimac.platform.api.command.CommandService;
import ac.grim.grimac.platform.api.manager.cloud.CloudCommandAdapter;
import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import io.leangen.geantyref.TypeToken;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.format.NamedTextColor;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.exception.InvalidSyntaxException;
import org.incendo.cloud.key.CloudKey;
import org.incendo.cloud.processors.requirements.RequirementApplicable;
import org.incendo.cloud.processors.requirements.RequirementApplicable.RequirementApplicableFactory;
import org.incendo.cloud.processors.requirements.RequirementPostprocessor;
import org.incendo.cloud.processors.requirements.Requirements;

import java.util.function.Function;
import java.util.function.Supplier;

public class CloudCommandService implements CommandService {

    public static final CloudKey<Requirements<Sender, SenderRequirement>> REQUIREMENT_KEY
            = CloudKey.of("requirements", new TypeToken<>() {});

    public static final RequirementApplicableFactory<Sender, SenderRequirement> REQUIREMENT_FACTORY
            = RequirementApplicable.factory(REQUIREMENT_KEY);

    private boolean commandsRegistered = false;

    private final Supplier<CommandManager<Sender>> commandManagerSupplier;
    private final CloudCommandAdapter commandAdapter;

    public CloudCommandService(Supplier<CommandManager<Sender>> commandManagerSupplier, CloudCommandAdapter commandAdapter) {
        this.commandManagerSupplier = commandManagerSupplier;
        this.commandAdapter = commandAdapter;
    }

    public void registerCommands() {
        if (commandsRegistered) return;
        CommandManager<Sender> commandManager = commandManagerSupplier.get();
        new GrimPerf().register(commandManager, commandAdapter);
        new GrimDebug().register(commandManager, commandAdapter);
        new GrimAlerts().register(commandManager, commandAdapter);
        new GrimProfile().register(commandManager, commandAdapter);
        new GrimSendAlert().register(commandManager, commandAdapter);
        new GrimHelp().register(commandManager, commandAdapter);
        new GrimHistory().register(commandManager, commandAdapter);
        new GrimReload().register(commandManager, commandAdapter);
        new GrimSpectate().register(commandManager, commandAdapter);
        new GrimStopSpectating().register(commandManager, commandAdapter);
        new GrimLog().register(commandManager, commandAdapter);
        new GrimVerbose().register(commandManager, commandAdapter);
        new GrimVersion().register(commandManager, commandAdapter);
        new GrimDump().register(commandManager, commandAdapter);
        new GrimBrands().register(commandManager, commandAdapter);
        new GrimList().register(commandManager, commandAdapter);
        new GrimTestWebhook().register(commandManager, commandAdapter);

        final RequirementPostprocessor<Sender, SenderRequirement>
                senderRequirementPostprocessor = RequirementPostprocessor.of(
                REQUIREMENT_KEY,
                new GrimCommandFailureHandler()
        );
        commandManager.registerCommandPostProcessor(senderRequirementPostprocessor);
        registerExceptionHandler(commandManager, InvalidSyntaxException.class, e -> MessageUtil.miniMessage(e.correctSyntax()));
        commandsRegistered = true;
    }

    protected <E extends Exception> void registerExceptionHandler(CommandManager<Sender> commandManager, Class<E> ex, Function<E, ComponentLike> toComponent) {
        commandManager.exceptionController().registerHandler(ex,
                (c) -> c.context().sender().sendMessage(toComponent.apply(c.exception()).asComponent().colorIfAbsent(NamedTextColor.RED))
        );
    }
}
