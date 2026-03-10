package ac.grim.grimac.platform.bukkit.manager;

import ac.grim.grimac.platform.api.command.PlayerSelector;
import ac.grim.grimac.platform.api.manager.cloud.CloudCommandAdapter;
import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.platform.bukkit.command.BukkitPlayerSelectorParser;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.bukkit.BukkitCommandContextKeys;
import org.incendo.cloud.parser.ParserDescriptor;
import org.incendo.cloud.suggestion.Suggestion;
import org.incendo.cloud.suggestion.SuggestionProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class BukkitParserDescriptorFactory implements CloudCommandAdapter {

    // Parser is kept as a field because its stateless
    private final BukkitPlayerSelectorParser<Sender> bukkitPlayerSelectorParser = new BukkitPlayerSelectorParser<>();

    @Override
    public ParserDescriptor<Sender, PlayerSelector> singlePlayerSelectorParser() {
        return bukkitPlayerSelectorParser.descriptor();
    }

    // TODO (Cross-platform) better + brigadier suggestions
    @Override
    public SuggestionProvider<Sender> onlinePlayerSuggestions() {
        return (context, input) -> {
            List<Suggestion> suggestions = new ArrayList<>();

            for(Player player : Bukkit.getOnlinePlayers()) {
                CommandSender bukkit = context.get(BukkitCommandContextKeys.BUKKIT_COMMAND_SENDER);
                if (!(bukkit instanceof Player) || ((Player)bukkit).canSee(player)) {
                    suggestions.add(Suggestion.suggestion(player.getName()));
                }
            }

            return CompletableFuture.completedFuture(suggestions);
        };
    }
}
