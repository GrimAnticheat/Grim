package ac.grim.grimac.platform.api.manager.cloud;

import ac.grim.grimac.platform.api.command.PlayerSelector;
import ac.grim.grimac.platform.api.manager.CommandAdapter;
import ac.grim.grimac.platform.api.sender.Sender;
import org.incendo.cloud.parser.ParserDescriptor;
import org.incendo.cloud.suggestion.SuggestionProvider;

/**
 * Acts as a bridge between platform-agnostic command definitions
 * and platform-specific parsers, suggestion providers, etc.
 */
public interface CloudCommandAdapter extends CommandAdapter {
    /**
     * Provides a parser descriptor for a single player selector (@p, player name).
     *
     * @return The parser descriptor.
     */
    ParserDescriptor<Sender, PlayerSelector> singlePlayerSelectorParser();

    /**
     * Provides a suggestion provider that lists all online players.
     * <p>
     * Platform-specific implementations should handle things like
     * vanished players.
     *
     * @return The suggestion provider.
     */
    SuggestionProvider<Sender> onlinePlayerSuggestions();
}
