package ac.grim.grimac.platform.bukkit.manager;

import ac.grim.grimac.platform.api.command.PlayerSelector;
import ac.grim.grimac.platform.api.manager.cloud.CloudPlatformCommandArguments;
import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.platform.bukkit.command.BukkitPlayerSelectorParser;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.incendo.cloud.bukkit.BukkitCommandContextKeys;
import org.incendo.cloud.parser.ParserDescriptor;
import org.incendo.cloud.suggestion.Suggestion;
import org.incendo.cloud.suggestion.SuggestionProvider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class BukkitCloudPlatformCommandArguments implements CloudPlatformCommandArguments {

    private final BukkitPlayerSelectorParser<Sender> bukkitPlayerSelectorParser = new BukkitPlayerSelectorParser<>();

    @Override
    public ParserDescriptor<Sender, PlayerSelector> singlePlayerSelectorParser() {
        return bukkitPlayerSelectorParser.descriptor();
    }

    @Override
    public SuggestionProvider<Sender> onlinePlayerSuggestions() {
        return (context, input) -> {
            Collection<? extends Player> players = Bukkit.getOnlinePlayers();
            List<Suggestion> suggestions = new ArrayList<>(players.size());

            Player sender = context.get(BukkitCommandContextKeys.BUKKIT_COMMAND_SENDER) instanceof Player player ? player : null;

            for (Player player : players) {
                if (sender == null || sender.canSee(player)) {
                    suggestions.add(Suggestion.suggestion(player.getName()));
                }
            }

            return CompletableFuture.completedFuture(suggestions);
        };
    }
}
