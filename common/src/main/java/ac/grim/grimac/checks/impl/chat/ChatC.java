package ac.grim.grimac.checks.impl.chat;

import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.api.storage.verbose.Verbose;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.impl.multiactions.MultiActionsC;
import ac.grim.grimac.checks.packethandler.PacketHandlerRegistryBuilder;
import ac.grim.grimac.checks.type.PacketListener;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientChatCommand;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientChatCommandUnsigned;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientChatMessage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;
import java.util.regex.Pattern;

@CheckData(name = "ChatC", stableKey = "grim.chat.moving_while_chatting", description = "Moving while chatting", experimental = true)
public class ChatC extends Check implements PacketListener {
    private static final Verbose V = Verbose.of("sprinting={bool}, sneaking={bool}, input={bool}");

    public ChatC(GrimPlayer player) {
        super(player);
    }

    // optionally allow cheats like autogg
    private @Nullable Predicate<String> exemptRegex;

    @Override
    public void registerPacketHandlers(@NotNull PacketHandlerRegistryBuilder registry) {
        registry.registerPreViaReceiveHandler(
                event -> check(new WrapperPlayClientChatMessage(event).getMessage(), event),
                PacketType.Play.Client.CHAT_MESSAGE
        );
        registry.registerPreViaReceiveHandler(
                event -> check("/" + new WrapperPlayClientChatCommandUnsigned(event).getCommand(), event),
                PacketType.Play.Client.CHAT_COMMAND_UNSIGNED
        );
        registry.registerPreViaReceiveHandler(
                event -> check("/" + new WrapperPlayClientChatCommand(event).getCommand(), event),
                PacketType.Play.Client.CHAT_COMMAND
        );
    }

    private void check(String message, PacketReceiveEvent event) {
        if (exemptRegex != null && exemptRegex.test(message)) {
            return;
        }

        boolean sprinting = MultiActionsC.isVerboseSprinting(player);
        boolean sneaking = MultiActionsC.isVerboseSneaking(player);
        boolean input = MultiActionsC.isVerboseInput(player);
        if ((sprinting || sneaking || input)
                && flag(V.write(verbose()).bool(sprinting).bool(sneaking).bool(input))
                && shouldModifyPackets()) {
            event.setCancelled(true);
            player.onPacketCancel();
        }
    }

    @Override
    public void onReload(@NotNull ConfigManager config) {
        String regexString = config.getStringElse(getConfigName() + ".exempt-regex", null);
        exemptRegex = regexString == null ? null : Pattern.compile(regexString).asMatchPredicate();
    }
}
