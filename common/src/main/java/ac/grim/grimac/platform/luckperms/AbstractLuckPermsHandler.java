package ac.grim.grimac.platform.luckperms;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.manager.init.stop.StoppableInitable;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.LogUtil;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.event.EventSubscription;
import net.luckperms.api.event.LuckPermsEvent;
import net.luckperms.api.event.context.ContextUpdateEvent;
import net.luckperms.api.event.group.GroupDataRecalculateEvent;
import net.luckperms.api.event.group.GroupDeleteEvent;
import net.luckperms.api.event.node.NodeAddEvent;
import net.luckperms.api.event.node.NodeRemoveEvent;
import net.luckperms.api.event.user.UserDataRecalculateEvent;
import net.luckperms.api.model.PermissionHolder;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.node.types.PermissionNode;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public abstract class AbstractLuckPermsHandler implements StoppableInitable {
    private List<EventSubscription<?>> subscriptions;
    private LuckPerms luckPerms;

    protected final void register(LuckPerms luckPerms) {
        this.luckPerms = luckPerms;
        subscriptions = new ArrayList<>();
        subscriptions.add(subscribe(luckPerms, NodeAddEvent.class, this::onNodeAdd));
        subscriptions.add(subscribe(luckPerms, NodeRemoveEvent.class, this::onNodeRemove));
        subscriptions.add(subscribe(luckPerms, UserDataRecalculateEvent.class, this::onUserDataRecalculate));
        subscriptions.add(subscribe(luckPerms, GroupDataRecalculateEvent.class, this::onGroupDataRecalculate));
        subscriptions.add(subscribe(luckPerms, GroupDeleteEvent.class, this::onGroupDelete));
        subscriptions.add(subscribe(luckPerms, ContextUpdateEvent.class, this::onContextUpdate));
        LogUtil.info("LuckPerms detected");
    }

    protected final boolean isRegistered() {
        return subscriptions != null;
    }

    protected abstract <T extends LuckPermsEvent> EventSubscription<T> subscribe(
            LuckPerms luckPerms,
            Class<T> eventClass,
            Consumer<? super T> handler
    );

    protected abstract UUID contextSubjectUuid(ContextUpdateEvent event);

    @Override
    public void stop() {
        if (subscriptions == null) {
            luckPerms = null;
            return;
        }
        for (EventSubscription<?> subscription : subscriptions) {
            subscription.close();
        }
        subscriptions = null;
        luckPerms = null;
    }

    private void onNodeAdd(NodeAddEvent event) {
        onNodeMutate(event.getTarget(), event.getNode());
    }

    private void onNodeRemove(NodeRemoveEvent event) {
        onNodeMutate(event.getTarget(), event.getNode());
    }

    private void onNodeMutate(PermissionHolder target, Node node) {
        if (target instanceof User user) {
            if (shouldRefreshUserForNode(node)) refreshGrimPlayerPermissions(user);
            return;
        }

        if (target instanceof Group group && shouldRefreshGroupForNode(node)) {
            refreshPlayersInheritingGroup(group);
        }
    }

    private void onUserDataRecalculate(UserDataRecalculateEvent event) {
        refreshGrimPlayerPermissions(event.getUser());
    }

    private void onGroupDataRecalculate(GroupDataRecalculateEvent event) {
        refreshPlayersInheritingGroup(event.getGroup());
    }

    private void onGroupDelete(GroupDeleteEvent event) {
        if (shouldRefreshHolderForNodes(event.getExistingData())) refreshTrackedPlayers();
    }

    private void onContextUpdate(ContextUpdateEvent event) {
        refreshGrimPlayerPermissions(contextSubjectUuid(event));
    }

    private boolean shouldRefreshUserForNode(Node node) {
        if (node instanceof PermissionNode permissionNode) return isGrimPermission(permissionNode);
        if (node instanceof InheritanceNode inheritanceNode) return inheritedGroupMayAffectGrimPermissions(inheritanceNode);
        return false;
    }

    private boolean shouldRefreshGroupForNode(Node node) {
        if (node instanceof PermissionNode permissionNode) return isGrimPermission(permissionNode);
        if (node instanceof InheritanceNode inheritanceNode) return inheritedGroupMayAffectGrimPermissions(inheritanceNode);
        return false;
    }

    private boolean shouldRefreshHolderForNodes(Iterable<? extends Node> nodes) {
        for (Node node : nodes) {
            if (shouldRefreshGroupForNode(node)) return true;
        }

        return false;
    }

    private static boolean isGrimPermission(PermissionNode node) {
        String permission = node.getPermission().toLowerCase(Locale.ROOT);
        return permission.equals("*") || permission.equals("grim") || permission.startsWith("grim.");
    }

    private boolean inheritedGroupMayAffectGrimPermissions(InheritanceNode node) {
        if (luckPerms == null) return true;
        Group group = luckPerms.getGroupManager().getGroup(node.getGroupName());
        // Refresh if the inherited group was deleted, or if that group can grant Grim permissions.
        return group == null || holderHasGrimPermission(group, new LinkedHashSet<>());
    }

    private static boolean holderHasGrimPermission(PermissionHolder holder, Set<String> seenGroups) {
        for (Node node : holder.getNodes()) {
            if (node instanceof PermissionNode permissionNode && isGrimPermission(permissionNode)) return true;
        }

        for (Group group : holder.getInheritedGroups(holder.getQueryOptions())) {
            if (!seenGroups.add(normalizeGroupName(group))) continue;
            if (holderHasGrimPermission(group, seenGroups)) return true;
        }

        return false;
    }

    private void refreshPlayersInheritingGroup(Group group) {
        LuckPerms luckPerms = this.luckPerms;
        if (luckPerms == null) return;

        String groupName = normalizeGroupName(group);
        Set<UUID> refreshed = new LinkedHashSet<>();
        for (GrimPlayer player : GrimAPI.INSTANCE.getPlayerDataManager().getEntries()) {
            if (player == null || player.uuid == null) continue;
            User user = luckPerms.getUserManager().getUser(player.uuid);
            if (user == null || !userInheritsGroup(user, groupName)) continue;
            if (!refreshed.add(player.uuid)) continue;
            player.updatePermissions();
        }
    }

    private static void refreshTrackedPlayers() {
        Set<UUID> refreshed = new LinkedHashSet<>();
        for (GrimPlayer player : GrimAPI.INSTANCE.getPlayerDataManager().getEntries()) {
            if (player == null || player.uuid == null) continue;
            if (!refreshed.add(player.uuid)) continue;
            player.updatePermissions();
        }
    }

    private static boolean userInheritsGroup(User user, String groupName) {
        for (Group group : user.getInheritedGroups(user.getQueryOptions())) {
            if (normalizeGroupName(group).equals(groupName)) return true;
        }

        return false;
    }

    private static String normalizeGroupName(Group group) {
        return group.getName().toLowerCase(Locale.ROOT);
    }

    private static void refreshGrimPlayerPermissions(UUID uuid) {
        if (uuid == null) return;
        GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(uuid);
        if (player == null) return;
        player.updatePermissions();
    }

    private static void refreshGrimPlayerPermissions(User user) {
        if (user == null) return;
        refreshGrimPlayerPermissions(user.getUniqueId());
    }
}
