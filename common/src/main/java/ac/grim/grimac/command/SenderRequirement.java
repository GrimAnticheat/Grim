package ac.grim.grimac.command;

import ac.grim.grimac.platform.api.sender.Sender;
import net.kyori.adventure.text.Component;
import org.incendo.cloud.processors.requirements.Requirement;
import org.jetbrains.annotations.NotNull;

public interface SenderRequirement extends Requirement<Sender, SenderRequirement> {
    @NotNull Component errorMessage(Sender sender);
}
