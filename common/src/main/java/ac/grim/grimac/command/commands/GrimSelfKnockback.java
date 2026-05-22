package ac.grim.grimac.command.commands;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.command.BuildableCommand;
import ac.grim.grimac.command.CloudCommandService;
import ac.grim.grimac.command.requirements.PlayerSenderRequirement;
import ac.grim.grimac.platform.api.entity.GrimEntity;
import ac.grim.grimac.platform.api.manager.cloud.CloudCommandAdapter;
import ac.grim.grimac.platform.api.player.PlatformPlayer;
import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityVelocity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.description.Description;
import org.jetbrains.annotations.NotNull;

public class GrimSelfKnockback implements BuildableCommand {
    private static final Vector3d SELF_KNOCKBACK = new Vector3d(1.25D, 2.42D, 1.25D);

    @Override
    public void register(CommandManager<Sender> commandManager, CloudCommandAdapter adapter) {
        commandManager.command(
                commandManager.commandBuilder("grim", "grimac")
                        .literal("selfkb", Description.of("Send a self knockback packet"))
                        .permission("grim.knockback")
                        .handler(this::handleSelfKnockback)
                        .apply(CloudCommandService.REQUIREMENT_FACTORY.create(PlayerSenderRequirement.PLAYER_SENDER_REQUIREMENT))
        );
    }

    private void handleSelfKnockback(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        GrimPlayer grimPlayer = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(sender.getUniqueId());
        if (grimPlayer == null) {
            sender.sendMessage(MessageUtil.getParsedComponent(sender, "sender-not-found", "%prefix% &cYou cannot be exempt to use this command!"));
            return;
        }

        boolean inVehicle = grimPlayer.inVehicle();
        PlatformPlayer platformPlayer = sender.getPlatformPlayer();
        if (inVehicle && platformPlayer != null) {
            GrimEntity vehicle = platformPlayer.getVehicle();
            if (vehicle != null) {
                vehicle.addVelocity(SELF_KNOCKBACK);
            }
        }

        int targetEntityId = inVehicle ? grimPlayer.getRidingVehicleId() : grimPlayer.entityID;
        grimPlayer.runSafely(() -> grimPlayer.user.sendPacket(new WrapperPlayServerEntityVelocity(targetEntityId, SELF_KNOCKBACK)));

        sender.sendMessage(Component.text()
                .append(Component.text("Sent knockback packet to ", NamedTextColor.GRAY))
                .append(Component.text(inVehicle ? "your vehicle" : "your player entity", NamedTextColor.WHITE))
                .append(Component.text(".", NamedTextColor.GRAY))
                .build());
    }
}
