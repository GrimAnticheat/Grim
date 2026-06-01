package ac.grim.grimac.platform.fabric.utils.message;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public interface IFabricMessageUtil {
    Component textLiteral(String message);
    void sendMessage(CommandSourceStack target, Component message, boolean overlay);

    /**
     * Sends a native text component to a player as a plain (non-overlay) system message.
     * <p>
     * Implemented per Minecraft version because the player wrapper historically used a
     * different NMS method per mapping family at this call site:
     * the intermediary variants call {@code ServerPlayer#displayClientMessage(component, false)}
     * while the official (26.x) variant calls {@code ServerPlayer#sendSystemMessage(component, false)}.
     * These are NOT a rename: both methods exist on {@code ServerPlayer} in both mapping sets, and
     * {@code displayClientMessage(c, actionBar)} is literally {@code this.sendSystemMessage(c, actionBar)}.
     * Their second boolean means different things ({@code actionBar} vs {@code bypassHiddenChat}); they
     * coincide ONLY at {@code false}, where both reach
     * {@code ClientboundSystemChatPacket(component, /*overlay=*&#47;false)} (verified against the 1.21.11
     * and 26.1 mapped sources). This method deliberately exposes NO boolean so the {@code false}
     * (system-message, non-overlay) semantics are baked in and each version keeps its own actual call.
     *
     * @param player     the recipient {@code ServerPlayer}
     * @param nativeText the native {@code net.minecraft.network.chat.Component} to send
     */
    void sendSystemMessageToPlayer(ServerPlayer player, Component nativeText);
}
