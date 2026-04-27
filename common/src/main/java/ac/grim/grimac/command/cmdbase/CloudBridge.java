package ac.grim.grimac.command.cmdbase;

import ac.grim.grimac.api.command.CommandSender;
import ac.grim.grimac.api.command.builder.ArgumentParser;
import ac.grim.grimac.api.command.builder.AsyncSuggestionProvider;
import ac.grim.grimac.api.command.builder.GrimCommand;
import ac.grim.grimac.api.command.builder.GrimCommandContext;
import ac.grim.grimac.api.command.builder.GrimCommandInput;
import ac.grim.grimac.api.command.builder.ParseResult;
import ac.grim.grimac.api.command.builder.SuggestionProvider;
import ac.grim.grimac.command.SenderRequirement;
import ac.grim.grimac.platform.api.sender.Sender;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.parser.ParserDescriptor;
import org.incendo.cloud.processors.requirements.Requirements;
import org.incendo.cloud.suggestion.BlockingSuggestionProvider;
import org.incendo.cloud.suggestion.Suggestion;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Translates the api-public {@link GrimCommand.Spec} into Cloud command
 * registrations. Wraps api-public {@link ArgumentParser}s as Cloud parsers and
 * adapts {@link SuggestionProvider}/{@link AsyncSuggestionProvider} to Cloud's
 * suggestion contract.
 *
 * <p>Returns a {@link AbstractCommandRegistrar.Registration} handle so the
 * {@link CommandRegistryImpl} can sweep on plugin disable.
 */
public final class CloudBridge {

    private final CommandManager<Sender> manager;

    public CloudBridge(@NotNull CommandManager<Sender> manager) {
        this.manager = manager;
    }

    public @NotNull AbstractCommandRegistrar.Registration register(@NotNull GrimCommand.Spec spec) {
        Command.Builder<Sender> builder = manager.commandBuilder(spec.rootName(), spec.rootAliases());

        // Apply each step (literals + arguments) to the builder.
        for (GrimCommand.Step step : spec.steps()) {
            if (step instanceof GrimCommand.LiteralStep lit) {
                builder = builder.literal(lit.name(), lit.aliases());
            } else if (step instanceof GrimCommand.ArgumentStep arg) {
                builder = applyArgument(builder, arg);
            }
        }

        // Flags.
        for (Map.Entry<String, GrimCommand.FlagSpec> e : spec.flags().entrySet()) {
            builder = applyFlag(builder, e.getValue());
        }

        // Permission → single-permission requirement.
        if (spec.permission() != null) {
            List<SenderRequirement> reqs = new ArrayList<>();
            reqs.add(new PermissionSenderRequirement(spec.permission()));
            builder = builder.apply(ac.grim.grimac.command.CloudCommandService.REQUIREMENT_FACTORY
                    .create(Requirements.of(reqs)));
        }

        // Handler — sync or async, mutually exclusive (Builder.build() ensured one was set).
        if (spec.futureHandler() != null) {
            var future = spec.futureHandler();
            builder = builder.futureHandler(ctx -> future.apply(new CloudGrimContext(ctx)));
        } else {
            var sync = spec.handler();
            builder = builder.handler(ctx -> sync.accept(new CloudGrimContext(ctx)));
        }

        manager.command(builder);
        return new RegisteredRoot(spec.rootName(), spec.rootAliases());
    }

    private <T> Command.Builder<Sender> applyArgument(Command.Builder<Sender> b, GrimCommand.ArgumentStep step) {
        @SuppressWarnings("unchecked")
        ArgumentParser<T> p = (ArgumentParser<T>) step.parser();
        ParserDescriptor<Sender, T> descriptor = ParserDescriptor.of(toCloud(p), p.valueType());
        BlockingSuggestionProvider<Sender> sugg = (ctx, in) -> mapSuggestions(p.suggestions(new CloudGrimContext(ctx), in.peekString()));
        return step.required()
                ? b.required(step.name(), descriptor, sugg)
                : b.optional(step.name(), descriptor, sugg);
    }

    private Command.Builder<Sender> applyFlag(Command.Builder<Sender> b, GrimCommand.FlagSpec flag) {
        if (flag.parser() == null) {
            return b.flag(manager.flagBuilder(flag.name()).withAliases(flag.aliases()));
        }
        return b.flag(buildValueFlag(flag));
    }

    private <T> org.incendo.cloud.parser.flag.CommandFlag.Builder<Sender, T> buildValueFlag(GrimCommand.FlagSpec flag) {
        @SuppressWarnings("unchecked")
        ArgumentParser<T> p = (ArgumentParser<T>) flag.parser();
        return manager.<T>flagBuilder(flag.name())
                .withAliases(flag.aliases())
                .withComponent(ParserDescriptor.of(toCloud(p), p.valueType()));
    }

    /** Wraps an api-public ArgumentParser as a Cloud parser. */
    public static <T> @NotNull org.incendo.cloud.parser.ArgumentParser<Sender, T> toCloud(@NotNull ArgumentParser<T> facade) {
        return (cloudCtx, cloudInput) -> {
            ReadingInput input = new ReadingInput(cloudInput);
            ParseResult<T> r = facade.parse(new CloudGrimContext(cloudCtx), input);
            return r.isOk()
                    ? ArgumentParseResult.success(r.value())
                    : ArgumentParseResult.failure(new IllegalArgumentException(r.error()));
        };
    }

    private static List<? extends Suggestion> mapSuggestions(List<String> raw) {
        List<Suggestion> out = new ArrayList<>(raw.size());
        for (String s : raw) out.add(Suggestion.suggestion(s));
        return out;
    }

    private final class RegisteredRoot implements AbstractCommandRegistrar.Registration {
        private final List<String> names;
        private boolean cleared = false;

        RegisteredRoot(String primary, String[] aliases) {
            this.names = new ArrayList<>(aliases.length + 1);
            names.add(primary);
            names.addAll(Arrays.asList(aliases));
        }

        @Override
        public void unregister() {
            if (cleared) return;
            cleared = true;
            for (String name : names) {
                try {
                    manager.deleteRootCommand(name);
                } catch (org.incendo.cloud.CloudCapability.CloudCapabilityMissingException ignored) {
                    // Platform doesn't support deletion; let it stick around.
                }
            }
        }
    }

    /** Adapts Cloud's CommandContext to the api-public GrimCommandContext. */
    private static final class CloudGrimContext implements GrimCommandContext {
        private final CommandContext<Sender> wrapped;

        CloudGrimContext(CommandContext<Sender> wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public @NotNull CommandSender sender() {
            return wrapped.sender();
        }

        @Override
        public <T> @NotNull T get(@NotNull String key) {
            return wrapped.get(key);
        }

        @Override
        public <T> @NotNull Optional<T> optional(@NotNull String key) {
            return wrapped.optional(key);
        }

        @Override
        public boolean flag(@NotNull String name) {
            return wrapped.flags().contains(name);
        }

        @Override
        public <T> @NotNull Optional<T> flagValue(@NotNull String name) {
            T value = wrapped.flags().get(name);
            return Optional.ofNullable(value);
        }
    }

    /**
     * Adapts Cloud's CommandInput to the api-public GrimCommandInput. Reads
     * tokens via {@code readString()} / {@code peekString()} which are space-
     * delimited.
     */
    private static final class ReadingInput implements GrimCommandInput {
        private final CommandInput wrapped;

        ReadingInput(CommandInput wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public @NotNull String peekString() {
            return wrapped.peekString();
        }

        @Override
        public @NotNull String readString() {
            return wrapped.readString();
        }

        @Override
        public @NotNull String remaining() {
            return wrapped.remainingInput();
        }

        @Override
        public boolean isEmpty() {
            return wrapped.isEmpty();
        }
    }
}
