package ac.grim.grimac.manager.datastore;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.api.plugin.GrimPlugin;
import ac.grim.grimac.internal.storage.retention.RetentionSweeper;
import ac.grim.grimac.platform.api.PlatformLoader;
import ac.grim.grimac.platform.api.PlatformServer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Map;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RetentionStartupTest {
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void productionStartupHonorsRetentionOptInAndStopsTheSchedule(boolean enabled, @TempDir Path directory)
            throws Exception {
        ConfigManager configuration = mock(ConfigManager.class, call -> {
            if (call.getMethod().getName().endsWith("Else")) {
                return call.getArgument(call.getArguments().length - 1);
            }
            return RETURNS_DEFAULTS.answer(call);
        });
        when(configuration.getMapElse(eq("database.routing"), anyMap())).thenReturn(Map.of(
                "session", "sqlite", "violation", "sqlite", "player-identity", "sqlite"));
        when(configuration.getMapElse(eq("database.retention"), anyMap())).thenReturn(Map.of("session", Map.of()));
        when(configuration.getBooleanElse("database.retention.session.enabled", false)).thenReturn(true);
        when(configuration.getLongElse("database.retention.session.max-age-days", 0L)).thenReturn(30L);
        when(configuration.getBooleanElse(RetentionSweeper.ENABLE_KEY, false)).thenReturn(enabled);
        GrimPlugin plugin = mock(GrimPlugin.class);
        when(plugin.getDataFolder()).thenReturn(directory.toFile());
        when(plugin.getLogger()).thenReturn(Logger.getLogger("RetentionStartupTest"));
        PlatformLoader loader = mock(PlatformLoader.class);
        PlatformServer server = mock(PlatformServer.class);
        when(loader.getPlatformServer()).thenReturn(server);
        when(loader.getPlugin()).thenReturn(plugin);
        when(server.getPlatformImplementationString()).thenReturn("test-server");

        Field configField = GrimAPI.INSTANCE.getConfigManager().getClass().getDeclaredField("config");
        configField.setAccessible(true);
        Object previousConfig = configField.get(GrimAPI.INSTANCE.getConfigManager());
        Field loaderField = GrimAPI.class.getDeclaredField("loader");
        loaderField.setAccessible(true);
        Object previousLoader = loaderField.get(GrimAPI.INSTANCE);
        DataStoreLifecycle lifecycle = new DataStoreLifecycle(plugin, GrimAPI.INSTANCE.getBackendRegistry());
        RetentionSweeper sweeper;
        try {
            configField.set(GrimAPI.INSTANCE.getConfigManager(), configuration);
            loaderField.set(GrimAPI.INSTANCE, loader);
            lifecycle.start();
            assertTrue(lifecycle.isLoaded(), "Production datastore startup must complete");
            Field sweeperField = DataStoreLifecycle.class.getDeclaredField("retentionSweeper");
            sweeperField.setAccessible(true);
            sweeper = (RetentionSweeper) sweeperField.get(lifecycle);
            assertNotNull(sweeper);
            assertEquals(enabled, sweeper.isScheduled());
        } finally {
            lifecycle.stop();
            configField.set(GrimAPI.INSTANCE.getConfigManager(), previousConfig);
            loaderField.set(GrimAPI.INSTANCE, previousLoader);
        }
        assertFalse(sweeper.isScheduled(), "Stopping Grim must release the retention scheduler");
    }
}
