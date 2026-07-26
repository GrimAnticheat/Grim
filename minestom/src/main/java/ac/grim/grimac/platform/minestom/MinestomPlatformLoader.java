package ac.grim.grimac.platform.minestom;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.api.plugin.GrimPlugin;
import ac.grim.grimac.platform.api.PlatformLoader;
import ac.grim.grimac.platform.api.PlatformServer;
import ac.grim.grimac.platform.api.command.CommandService;
import ac.grim.grimac.platform.api.manager.ItemResetHandler;
import ac.grim.grimac.platform.api.manager.MessagePlaceHolderManager;
import ac.grim.grimac.platform.api.manager.PermissionRegistrationManager;
import ac.grim.grimac.platform.api.manager.PlatformPluginManager;
import ac.grim.grimac.platform.api.player.PlatformPlayerFactory;
import ac.grim.grimac.platform.api.scheduler.PlatformScheduler;
import ac.grim.grimac.platform.api.sender.SenderFactory;
import ac.grim.grimac.platform.minestom.command.MinestomCommandService;
import ac.grim.grimac.platform.minestom.manager.MinestomItemResetHandler;
import ac.grim.grimac.platform.minestom.manager.MinestomMessagePlaceHolderManager;
import ac.grim.grimac.platform.minestom.manager.MinestomPermissionRegistrationManager;
import ac.grim.grimac.platform.minestom.manager.MinestomPlatformPluginManager;
import ac.grim.grimac.platform.minestom.player.MinestomPlatformPlayerFactory;
import ac.grim.grimac.platform.minestom.scheduler.MinestomPlatformScheduler;
import ac.grim.grimac.platform.minestom.sender.MinestomSenderFactory;
import com.github.retrooper.packetevents.PacketEventsAPI;

/**
 * Grim's {@link PlatformLoader} for Minestom — the object the monorepo glue constructs at
 * server startup to boot Grim. {@link #getPacketEvents()} returns the already-initialized
 * PacketEvents-Minestom API (Phase 1); {@link #boot()} runs Grim's load+start lifecycle.
 * <p>
 * TODO Phase 4: {@link #registerAPIService()} and the platform {@code Initable} tasks (event
 * manager, tick-end hook, alert/punishment bridge) are not wired yet — {@link #boot()} loads
 * Grim with no platform init tasks, enough to stand the platform up; the behavioural glue
 * (checks firing → alerts/punishments in the monorepo) lands in Phase 4.
 */
public final class MinestomPlatformLoader implements PlatformLoader {

    private final PacketEventsAPI<?> packetEvents;

    private final PlatformScheduler scheduler = new MinestomPlatformScheduler();
    private final MinestomSenderFactory senderFactory = new MinestomSenderFactory();
    private final ItemResetHandler itemResetHandler = new MinestomItemResetHandler();
    private final CommandService commandService = new MinestomCommandService();
    private final PlatformPlayerFactory platformPlayerFactory = new MinestomPlatformPlayerFactory();
    private final PlatformPluginManager pluginManager = new MinestomPlatformPluginManager();
    private final PlatformServer platformServer = new MinestomPlatformServer(senderFactory);
    private final MessagePlaceHolderManager messagePlaceHolderManager = new MinestomMessagePlaceHolderManager();
    private final PermissionRegistrationManager permissionManager = new MinestomPermissionRegistrationManager();
    private final GrimPlugin plugin = new MinestomGrimPlugin();

    public MinestomPlatformLoader(PacketEventsAPI<?> packetEvents) {
        this.packetEvents = packetEvents;
    }

    /** Loads and starts Grim on this platform. Call once after PacketEvents is initialized. */
    public void boot() {
        GrimAPI.INSTANCE.load(this);
        GrimAPI.INSTANCE.start();
    }

    /** Stops Grim. Call on server shutdown. */
    public void shutdown() {
        GrimAPI.INSTANCE.stop();
    }

    @Override
    public PlatformScheduler getScheduler() {
        return scheduler;
    }

    @Override
    public PlatformPlayerFactory getPlatformPlayerFactory() {
        return platformPlayerFactory;
    }

    @Override
    public PacketEventsAPI<?> getPacketEvents() {
        return packetEvents;
    }

    @Override
    public ItemResetHandler getItemResetHandler() {
        return itemResetHandler;
    }

    @Override
    public CommandService getCommandService() {
        return commandService;
    }

    @Override
    public SenderFactory<?> getSenderFactory() {
        return senderFactory;
    }

    @Override
    public GrimPlugin getPlugin() {
        return plugin;
    }

    @Override
    public PlatformPluginManager getPluginManager() {
        return pluginManager;
    }

    @Override
    public PlatformServer getPlatformServer() {
        return platformServer;
    }

    @Override
    public void registerAPIService() {
        // TODO Phase 4: bridge Grim's join/quit/reload/violation API events to the monorepo.
    }

    @Override
    public MessagePlaceHolderManager getMessagePlaceHolderManager() {
        return messagePlaceHolderManager;
    }

    @Override
    public PermissionRegistrationManager getPermissionManager() {
        return permissionManager;
    }
}
