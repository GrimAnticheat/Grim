package me.grim.bench.setup;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.PacketEventsAPI;
import com.github.retrooper.packetevents.injector.ChannelInjector;
import com.github.retrooper.packetevents.manager.player.PlayerManager;
import com.github.retrooper.packetevents.manager.protocol.ProtocolManager;
import com.github.retrooper.packetevents.manager.server.ServerManager;
import com.github.retrooper.packetevents.netty.NettyManager;
import com.github.retrooper.packetevents.protocol.ProtocolVersion;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.settings.PacketEventsSettings;
import com.github.retrooper.packetevents.util.LogManager;
import io.github.retrooper.packetevents.impl.netty.NettyManagerImpl;
import io.github.retrooper.packetevents.impl.netty.manager.protocol.ProtocolManagerAbstract;
import io.github.retrooper.packetevents.manager.server.ServerManagerImpl;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.logging.Level;

public class TestPacketEventsBuilder {

    public static final Logger LOGGER = LoggerFactory.getLogger("packetevents");

    private static PacketEventsAPI<Plugin> API_INSTANCE;

    public static void clearBuildCache() {
        API_INSTANCE = null;
    }

    public static PacketEventsAPI<Plugin> build(Plugin plugin) {
        if (API_INSTANCE == null) {
            API_INSTANCE = buildNoCache(plugin);
        }
        return API_INSTANCE;
    }

    public static PacketEventsAPI<Plugin> build(Plugin plugin, PacketEventsSettings settings) {
        if (API_INSTANCE == null) {
            API_INSTANCE = buildNoCache(plugin, settings);
        }
        return API_INSTANCE;
    }

    public static PacketEventsAPI<Plugin> buildNoCache(Plugin plugin) {
        return buildNoCache(plugin, new PacketEventsSettings());
    }

    public static PacketEventsAPI<Plugin> buildNoCache(Plugin plugin, PacketEventsSettings inSettings) {
        return new PacketEventsAPI<Plugin>() {
            private final PacketEventsSettings settings = inSettings;
            private final ProtocolManager protocolManager = new ProtocolManagerAbstract() {
                @Override
                public ProtocolVersion getPlatformVersion() {
                    return ProtocolVersion.UNKNOWN;
                }
            };
            private final ServerManager serverManager = new ServerManagerImpl();

            private final NettyManager nettyManager = new NettyManagerImpl();
            private final LogManager logManager = new LogManager() {
                @Override
                protected void log(Level level, @Nullable NamedTextColor color, String message) {
                    if (level == Level.SEVERE) {
                        LOGGER.error(message);
                    } else if (level == Level.WARNING) {
                        LOGGER.warn(message);
                    } else {
                        LOGGER.info(message);
                    }
                }
            };

            private boolean loaded;
            private boolean initialized;
            private boolean terminated;

            @Override
            public void load() {
                if (!loaded) {
                    //Resolve server version and cache
                    String id = plugin.getName().toLowerCase();
                    PacketEvents.IDENTIFIER = "pe-" + id;
                    PacketEvents.ENCODER_NAME = "pe-encoder-" + id;
                    PacketEvents.DECODER_NAME = "pe-decoder-" + id;
                    PacketEvents.CONNECTION_HANDLER_NAME = "pe-connection-handler-" + id;
                    PacketEvents.SERVER_CHANNEL_HANDLER_NAME = "pe-connection-initializer-" + id;
                    PacketEvents.TIMEOUT_HANDLER_NAME = "pe-timeout-handler-" + id;

                    if (!PacketType.isPrepared()) {
                        PacketType.prepare();
                    }

                    loaded = true;
                }
            }

            @Override
            public boolean isLoaded() {
                return loaded;
            }

            @Override
            public void init() {
                //Load if we haven't loaded already
                load();
                if (!initialized) {
                    initialized = true;
                }
            }

            @Override
            public boolean isInitialized() {
                return initialized;
            }

            @Override
            public void terminate() {
                if (initialized) {
                    initialized = false;
                    terminated = true;
                }
            }

            @Override
            public boolean isTerminated() {
                return terminated;
            }

            @Override
            public Plugin getPlugin() {
                return plugin;
            }

            @Override
            public ProtocolManager getProtocolManager() {
                return protocolManager;
            }

            @Override
            public ServerManager getServerManager() {
                return serverManager;
            }

            @Override
            public PlayerManager getPlayerManager() {
                return null;
            }

            @Override
            public PacketEventsSettings getSettings() {
                return settings;
            }

            @Override
            public NettyManager getNettyManager() {
                return nettyManager;
            }

            @Override
            public ChannelInjector getInjector() {
                return null;
            }

            @Override
            public LogManager getLogManager() {
                return logManager;
            }
        };
    }
}
