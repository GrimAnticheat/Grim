package ac.grim.grimac.utils.common.arguments;

import ac.grim.grimac.platform.api.Platform;

import static ac.grim.grimac.utils.common.arguments.ArgumentUtils.*;

public class CommonGrimArguments {

    private final static SystemArgumentFactory FACTORY = SystemArgumentFactory.Builder.of("Grim")
            .optionModifier(builder -> builder.key("Grim" + builder.options().getKey()))
            .supportEnv()
            .build();

    public final static SystemArgument<Boolean> KICK_ON_TRANSACTION_ERRORS = FACTORY.create(string("KickOnTransactionTaskErrors", true));
    public final static SystemArgument<String> API_URL = FACTORY.create(string("APIUrl", "https://api.grim.ac/v1/server/"));
    public final static SystemArgument<String> PASTE_URL = FACTORY.create(string("PasteUrl", "https://paste.grim.ac/"));
    public final static SystemArgument<Platform> PLATFORM_OVERRIDE = FACTORY.create(platform("PlatformOverride"));
    public final static SystemArgument<Integer> URL_TIMEOUT = FACTORY.create(range("UrlTimeout", 10000, 1000, 60000));

    /**
     * Enables "Fast Bypass" mode for chat messages sent by GrimAC.
     * <p>
     * <b>BENEFIT:</b> Messages are sent directly as packets, significantly improving
     * performance and reducing server overhead especially when lots of alerts are being sent.
     * <p>
     * <b>TRADE-OFF:</b> This completely bypasses the platform's event system (e.g., Bukkit's chat events).
     * Other plugins will NOT be able to see, format, or cancel these messages.
     * <p>
     * This setting is opt-in (default: false) and requires a server restart to change.
     */
    public final static SystemArgument<Boolean> USE_CHAT_FAST_BYPASS = FACTORY.create(string("ChatFastBypass", true));

    /**
     * If true, players will be kicked when they try to connect from a proxy server with ViaVersion installed.
     * <p>
     * This setting is opt-out (default: true) and requires a server restart to change.
     */
    public final static SystemArgument<Boolean> KICK_ON_VIA_PROXY = FACTORY.create(string("KickOnViaProxy", true));

}
