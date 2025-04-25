package ac.grim.grimac.command;

import ac.grim.grimac.platform.api.sender.Sender;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.processors.requirements.Requirement;

public interface SenderRequirement extends Requirement<Sender, SenderRequirement> {
    @NonNull Component errorMessage(Sender sender);
}
