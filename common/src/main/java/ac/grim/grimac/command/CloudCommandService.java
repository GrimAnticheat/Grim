package ac.grim.grimac.command;

import ac.grim.grimac.command.commands.*;
import ac.grim.grimac.command.handler.GrimCommandFailureHandler;
import ac.grim.grimac.platform.api.command.CommandService;
import ac.grim.grimac.platform.api.manager.cloud.CloudCommandAdapter;
import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import io.leangen.geantyref.TypeToken;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.format.NamedTextColor;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.exception.InvalidSyntaxException;
import org.incendo.cloud.key.CloudKey;
import org.incendo.cloud.processors.requirements.RequirementApplicable;
import org.incendo.cloud.processors.requirements.RequirementApplicable.RequirementApplicableFactory;
import org.incendo.cloud.processors.requirements.RequirementPostprocessor;
import org.incendo.cloud.processors.requirements.Requirements;

import java.util.Locale;
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
        new GrimHistoryMigrate().register(commandManager, commandAdapter);
        new GrimHistoryCopy().register(commandManager, commandAdapter);
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
        registerInvalidSyntaxHandler(commandManager);
        commandsRegistered = true;
    }

    private void registerInvalidSyntaxHandler(CommandManager<Sender> commandManager) {
        commandManager.exceptionController().registerHandler(InvalidSyntaxException.class, context -> {
            Sender sender = context.context().sender();
            if (isHistoryInput(context.context().rawInput().input())) {
                sender.sendMessage(Component.text("Invalid history syntax.", NamedTextColor.RED));
                sender.sendMessage(Component.text("Use: /grim history <player> [page <N>]", NamedTextColor.GRAY));
                sender.sendMessage(Component.text("Use: /grim history <player> session <N|latest> [page <N>] [-d] [-v]", NamedTextColor.GRAY));
                sender.sendMessage(Component.text("Tip: /grim history <player> session shows filter and detail options.", NamedTextColor.GRAY));
                sender.sendMessage(Component.text("Use /grim history player <player> ... for names that collide with history subcommands.", NamedTextColor.GRAY));
                return;
            }
            sender.sendMessage(Component.text(context.exception().correctSyntax(), NamedTextColor.RED));
        });
    }

    private static boolean isHistoryInput(String rawInput) {
        String input = rawInput.strip();
        if (input.startsWith("/")) input = input.substring(1).strip();
        String[] tokens = input.toLowerCase(Locale.ROOT).split("\\s+");
        return tokens.length >= 2
                && (tokens[0].equals("grim") || tokens[0].equals("grimac"))
                && (tokens[1].equals("history") || tokens[1].equals("hist"));
    }

    protected <E extends Exception> void registerExceptionHandler(CommandManager<Sender> commandManager, Class<E> ex, Function<E, ComponentLike> toComponent) {
        commandManager.exceptionController().registerHandler(ex,
                (c) -> c.context().sender().sendMessage(toComponent.apply(c.exception()).asComponent().colorIfAbsent(NamedTextColor.RED))
        );
    }
}
