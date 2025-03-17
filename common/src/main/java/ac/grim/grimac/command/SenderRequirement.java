package ac.grim.grimac.command;

import ac.grim.grimac.platform.api.sender.Sender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.processors.requirements.Requirement;

public interface SenderRequirement extends Requirement<Sender, SenderRequirement> {
    @NonNull String errorMessage();
}
