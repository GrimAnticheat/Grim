package ac.grim.grimac.command;

import ac.grim.grimac.platform.api.sender.Sender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.suggestion.Suggestion;
import org.incendo.cloud.suggestion.SuggestionProvider;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CommandUtils {

    public static SuggestionProvider<Sender> fromStrings(String... strings) {
        List<Suggestion> suggestions = Arrays.stream(strings).map(Suggestion::suggestion).toList();
        return new SuggestionProvider<>() {
            @Override
            public @NonNull CompletableFuture<? extends @NonNull Iterable<? extends @NonNull Suggestion>> suggestionsFuture(@NonNull CommandContext context, @NonNull CommandInput input) {
                return CompletableFuture.completedFuture(suggestions);
            }
        };
    }

}
