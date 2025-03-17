package ac.grim.grimac.command.requirements;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.command.SenderRequirement;
import ac.grim.grimac.platform.api.sender.Sender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.context.CommandContext;

public final class PlayerSenderRequirement implements SenderRequirement {

    public static final PlayerSenderRequirement PLAYER_SENDER_REQUIREMENT = new PlayerSenderRequirement();

    @Override
    public @NonNull String errorMessage() {
        return GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("cannot-run-on-self", "%prefix% &cThis command can only be used by players!");
    }

    @Override
    public boolean evaluateRequirement(@NonNull CommandContext<Sender> commandContext) {
        return commandContext.sender().isPlayer();
    }
}
