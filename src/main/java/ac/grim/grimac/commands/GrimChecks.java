package ac.grim.grimac.commands;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckType;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Subcommand;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

@CommandAlias("grim|grimac")
public class GrimChecks extends BaseCommand {
    
    @Subcommand("checks")
    @CommandAlias("checks")
    @CommandPermission("grim.checks")
    public void onChecks(Player player) {
        GrimPlayer grimPlayer = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(player);
        if (grimPlayer == null) {
            player.sendMessage(MessageUtil.format(GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("player-not-found", "%prefix% &cPlayer is exempt or offline!")));
            return;
        }

        AtomicInteger totalChecks = new AtomicInteger(0);
        String typeSpace = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("checks.category-space", " ");
        String typeNone = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("checks.not-found-category", "&cNone.");
        String experimentalSymbol = grimPlayer.punishmentManager.getExperimentalSymbol();
        String separator = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("checks.separator", "&7, ");
        String formattedSeparator = MessageUtil.format(separator);

        // Make this async to be 100% sure that it will not cause any lag
        CompletableFuture.supplyAsync(() ->{
            List<String> messageLines = new ArrayList<>();
            messageLines.add(MessageUtil.format(typeSpace));
            for (CheckType type : CheckType.values()){
                if (type != CheckType.OTHER) {
                    List<Check> checks = grimPlayer.checkManager.getChecksByType(type);
                    totalChecks.set(totalChecks.get() + checks.size());

                    messageLines.add(MessageUtil.format(GrimAPI.INSTANCE.getConfigManager().getConfig()
                            .getStringElse("checks.type", " &8» &f%checkType% checks:")
                            .replace("%checkType%", type.displayName())));

                    if (checks.isEmpty()) {
                        messageLines.add(MessageUtil.format(typeNone));
                    } else {
                        // First enabled, then exempted, then disabled checks
                        // Then sort alphabetically
                        checks.sort((c1, c2) -> {
                            if (c1.isEnabled() != c2.isEnabled()) {
                                return Boolean.compare(c2.isEnabled(), c1.isEnabled());
                            }

                            if (c1.isExempted() != c2.isExempted()) {
                                return Boolean.compare(c1.isExempted(), c2.isExempted());
                            }

                            return c1.getCheckName().compareToIgnoreCase(c2.getCheckName());
                        });
                        StringBuilder checkList = new StringBuilder();
                        for (Check check : checks) {
                            if (check.isEnabled()) {
                                if (check.isExempted()) {
                                    checkList.append(MessageUtil.format(GrimAPI.INSTANCE.getConfigManager().getConfig()
                                            .getStringElse("checks.check.exempted", "&e%checkName%%experimental%")
                                            .replace("%checkName%", check.getCheckName())
                                            .replace("%experimental%", check.isExperimental() ? experimentalSymbol : "") + separator));
                                } else {
                                    checkList.append(MessageUtil.format(GrimAPI.INSTANCE.getConfigManager().getConfig()
                                            .getStringElse("checks.check.enabled", "&a%checkName%%experimental%")
                                            .replace("%checkName%", check.getCheckName())
                                            .replace("%experimental%", check.isExperimental() ? experimentalSymbol : "") + separator));
                                }
                            } else {
                                checkList.append(MessageUtil.format(GrimAPI.INSTANCE.getConfigManager().getConfig()
                                        .getStringElse("checks.check.disabled", "&c%checkName%%experimental%")
                                        .replace("%checkName%", check.getCheckName())
                                        .replace("%experimental%", check.isExperimental() ? experimentalSymbol : "") + separator));
                            }
                        }
                        String line = checkList.toString();
                        if (line.endsWith(formattedSeparator)) {
                            line = line.substring(0, line.length() - formattedSeparator.length());
                        }
                        messageLines.add(line);
                    }
                    messageLines.add(MessageUtil.format(typeSpace));
                }
            }
            return messageLines;
        }).exceptionally(e -> {
            e.printStackTrace();
            player.sendMessage(MessageUtil.format(GrimAPI.INSTANCE.getConfigManager().getConfig()
                    .getStringElse("checks.not-found-total", "&cNo checks found.") + " (Check console)"));
            return null;
        }).thenAccept(messageLines -> {
            if (messageLines != null) {
                if (totalChecks.get() != 0) {
                    String space = GrimAPI.INSTANCE.getConfigManager().getConfig()
                            .getStringElse("checks.space", "&7======================");
                    messageLines.add(0, MessageUtil.format(space));
                    messageLines.add(MessageUtil.format(GrimAPI.INSTANCE.getConfigManager().getConfig()
                            .getStringElse("checks.space", space)));


                    player.sendMessage(String.join("\n", messageLines));
                } else {
                    player.sendMessage(MessageUtil.format(GrimAPI.INSTANCE.getConfigManager().getConfig()
                            .getStringElse("checks.not-found-total", "&cNo checks found.")));
                }
            }
        });
    }
}
