package ac.grim.grimac.command.handler;

import ac.grim.grimac.command.SenderRequirement;
import ac.grim.grimac.platform.api.sender.Sender;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.processors.requirements.RequirementFailureHandler;
import org.jetbrains.annotations.NotNull;

public class GrimCommandFailureHandler implements RequirementFailureHandler<Sender, SenderRequirement> {
    @Override
    public void handleFailure(@NotNull CommandContext<Sender> context, @NotNull SenderRequirement requirement) {
        context.sender().sendMessage(requirement.errorMessage(context.sender()));
    }
}
