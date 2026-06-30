package ac.grim.grimac.platform.bukkit.initables;

import ac.grim.grimac.manager.init.start.StartableInitable;
import ac.grim.grimac.platform.bukkit.GrimACBukkitLoaderPlugin;
import ac.grim.grimac.platform.luckperms.AbstractLuckPermsHandler;
import ac.grim.grimac.utils.anticheat.LogUtil;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.event.EventSubscription;
import net.luckperms.api.event.LuckPermsEvent;
import net.luckperms.api.event.context.ContextUpdateEvent;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.function.Consumer;

public class BukkitLuckPermsHandler extends AbstractLuckPermsHandler implements StartableInitable {

    @Override
    public void start() {
        try {
            register(LuckPermsProvider.get());
        } catch (IllegalStateException e) {
            LogUtil.warn("LuckPerms detected but its API was not available for the permission refresh hook");
        } catch (Exception e) {
            LogUtil.error("Error when initializing LuckPerms hook", e);
        }
    }

    @Override
    protected <T extends LuckPermsEvent> EventSubscription<T> subscribe(
            LuckPerms luckPerms,
            Class<T> eventClass,
            Consumer<? super T> handler
    ) {
        return luckPerms.getEventBus().subscribe(GrimACBukkitLoaderPlugin.LOADER, eventClass, handler);
    }

    @Override
    protected UUID contextSubjectUuid(ContextUpdateEvent event) {
        return event.getSubject(Player.class)
                .map(Player::getUniqueId)
                .orElse(null);
    }
}
