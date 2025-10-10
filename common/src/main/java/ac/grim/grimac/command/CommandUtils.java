package ac.grim.grimac.command;

import ac.grim.grimac.platform.api.sender.Sender;
import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.suggestion.Suggestion;
import org.incendo.cloud.suggestion.SuggestionProvider;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@UtilityClass
public class CommandUtils {
    @Contract("_ -> new")
    public static @NotNull SuggestionProvider<Sender> fromStrings(@NotNull String @NotNull ... strings) {
        List<Suggestion> suggestions = new ArrayList<>(strings.length);
        for (String s : strings) suggestions.add(Suggestion.suggestion(s));
        return new SenderSuggestionProvider(Collections.unmodifiableList(suggestions));
    }

    @RequiredArgsConstructor
    private class SenderSuggestionProvider implements SuggestionProvider<Sender> {
        private final List<Suggestion> suggestions;

        @Override
        public @NotNull CompletableFuture<? extends @NotNull Iterable<? extends @NotNull Suggestion>> suggestionsFuture(@NotNull CommandContext context, @NotNull CommandInput input) {
            return CompletableFuture.completedFuture(suggestions);
        }
    }
}
