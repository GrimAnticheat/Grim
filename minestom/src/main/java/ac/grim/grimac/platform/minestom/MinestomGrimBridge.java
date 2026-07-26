package ac.grim.grimac.platform.minestom;

import net.minestom.server.entity.Player;

import java.util.function.BiPredicate;

/**
 * Injectable seam between the (monorepo-agnostic) {@code grim-minestom} platform module and the
 * consuming monorepo. {@code grim-minestom} must never depend on {@code net.onthepixel.*}, so the
 * monorepo glue installs its integrations here <b>before</b> Grim boots, and the platform SPI reads
 * them back through the static accessors.
 *
 * <p>Currently holds only the permission check (routed to the monorepo's custom group system). More
 * hooks (alert sink, command registrar) can be added the same way as Phase 4 grows.
 *
 * <p>Default before installation: deny all permissions — the safe default that keeps Grim admin
 * perms (alerts/bypass/commands) locked so no player is silently exempted.
 */
public final class MinestomGrimBridge {

    private static volatile BiPredicate<Player, String> permissionCheck = (player, node) -> false;

    private MinestomGrimBridge() {
    }

    /** Installs the permission resolver. {@code null} resets to deny-all. */
    public static void setPermissionCheck(BiPredicate<Player, String> check) {
        permissionCheck = (check != null) ? check : (player, node) -> false;
    }

    /** Whether {@code player} holds {@code node}, per the installed resolver (deny-all until set). */
    public static boolean hasPermission(Player player, String node) {
        return permissionCheck.test(player, node);
    }
}
