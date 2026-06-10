package ac.grim.grimac.checks.impl.chat;

import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.api.storage.verbose.VerboseSchema;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.impl.multiactions.MultiActionsC;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientChatCommand;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientChatCommandUnsigned;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientChatMessage;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;
import java.util.regex.Pattern;

@CheckData(name = "ChatC", stableKey = "grim.chat.moving_while_chatting", verboseVersion = 1, description = "Moving while chatting", experimental = true)
public class ChatC extends Check implements PacketCheck {
    public static final VerboseSchema V = MultiActionsC.verboseSchema();

    public ChatC(GrimPlayer player) {
        super(player);
    }

    // optionally allow cheats like autogg
    private @Nullable Predicate<String> exemptRegex;

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.CHAT_MESSAGE) {
            check(new WrapperPlayClientChatMessage(event).getMessage(), event);
        }

        if (event.getPacketType() == PacketType.Play.Client.CHAT_COMMAND_UNSIGNED) {
            check("/" + new WrapperPlayClientChatCommandUnsigned(event).getCommand(), event);
        }

        if (event.getPacketType() == PacketType.Play.Client.CHAT_COMMAND) {
            check("/" + new WrapperPlayClientChatCommand(event).getCommand(), event);
        }
    }

    private void check(String message, PacketReceiveEvent event) {
        if (exemptRegex != null && exemptRegex.test(message)) {
            return;
        }

        boolean sprinting = MultiActionsC.isVerboseSprinting(player);
        boolean sneaking = MultiActionsC.isVerboseSneaking(player);
        boolean input = MultiActionsC.isVerboseInput(player);
        if ((sprinting || sneaking || input)
                && flagAndAlert(V.write(verbose()).bool(sprinting).bool(sneaking).bool(input))
                && shouldModifyPackets()) {
            event.setCancelled(true);
            player.onPacketCancel();
        }
    }

    @Override
    public void onReload(ConfigManager config) {
        String regexString = config.getStringElse(getConfigName() + ".exempt-regex", null);
        exemptRegex = regexString == null ? null : Pattern.compile(regexString).asMatchPredicate();
    }
}
