package ac.grim.bukkit.manager;

import ac.grim.bukkit.GrimACBukkitLoaderPlugin;
import ac.grim.grimac.platform.api.manager.MessagePlaceHolderManager;
import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class BukkitMessagePlaceHolderManager implements MessagePlaceHolderManager {
    @Override
    public @NonNull String replacePlaceholders(@Nullable Sender sender, @NonNull String string) {
        if (!MessageUtil.hasPlaceholderAPI) return string;
        CommandSender commandSender = sender == null ? null : GrimACBukkitLoaderPlugin.PLUGIN.getBukkitSenderFactory().unwrap(sender);
        return PlaceholderAPI.setPlaceholders(sender instanceof OfflinePlayer ? (OfflinePlayer) commandSender : null, string);
    }
}
