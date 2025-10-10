package ac.grim.grimac.checks.impl.prediction;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.checks.debug.AbstractDebugHandler;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import ac.grim.grimac.utils.lists.EvictingQueue;
import ac.grim.grimac.utils.math.Vector3dm;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class DebugHandler extends AbstractDebugHandler implements PostPredictionCheck {
    private static final Component GRAY_ARROW = MiniMessage.miniMessage().deserialize("<gray>→0.03→</gray>");
    private static final Component P_PREFIX = MiniMessage.miniMessage().deserialize("<reset>P: </reset>");
    private static final Component A_PREFIX = MiniMessage.miniMessage().deserialize("<reset>A: </reset>");
    private static final Component O_PREFIX = MiniMessage.miniMessage().deserialize("<reset>O: </reset>");

    private final Set<GrimPlayer> listeners = new CopyOnWriteArraySet<>(new HashSet<>());
    private boolean outputToConsole = false;
    private boolean enabledFlags = false;
    private boolean lastMovementIsFlag = false;

    private final EvictingQueue<Component> predicted = new EvictingQueue<>(5);
    private final EvictingQueue<Component> actually = new EvictingQueue<>(5);
    private final EvictingQueue<Component> offset = new EvictingQueue<>(5);

    public DebugHandler(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPredictionComplete(final PredictionComplete predictionComplete) {
        if (!predictionComplete.isChecked()) return;

        double offset = predictionComplete.getOffset();

        if (listeners.isEmpty() && !outputToConsole) return;
        if (player.predictedVelocity.vector.lengthSquared() == 0 && offset == 0) return;

        String color = pickColor(offset, offset);

        Vector3dm predicted = player.predictedVelocity.vector;
        Vector3dm actually = player.actualMovement;

        String xColor = pickColor(Math.abs(predicted.getX() - actually.getX()), offset);
        String yColor = pickColor(Math.abs(predicted.getY() - actually.getY()), offset);
        String zColor = pickColor(Math.abs(predicted.getZ() - actually.getZ()), offset);

        Component p = Component.empty()
                .append(P_PREFIX.color(NamedTextColor.NAMES.value(color)))
                .append(Component.text(predicted.getX()).color(NamedTextColor.NAMES.value(xColor)))
                .append(Component.space())
                .append(Component.text(predicted.getY()).color(NamedTextColor.NAMES.value(yColor)))
                .append(Component.space())
                .append(Component.text(predicted.getZ()).color(NamedTextColor.NAMES.value(zColor)));

        Component a = Component.empty()
                .append(A_PREFIX.color(NamedTextColor.NAMES.value(color)))
                .append(Component.text(actually.getX()).color(NamedTextColor.NAMES.value(xColor)))
                .append(Component.space())
                .append(Component.text(actually.getY()).color(NamedTextColor.NAMES.value(yColor)))
                .append(Component.space())
                .append(Component.text(actually.getZ()).color(NamedTextColor.NAMES.value(zColor)));

        String canSkipTick = (player.couldSkipTick + " ").substring(0, 1);
        String actualMovementSkip = (player.skippedTickInActualMovement + "").charAt(0) + " ";
        Component o = Component.empty()
                .append(Component.text(canSkipTick).color(NamedTextColor.GRAY))
                .append(GRAY_ARROW)
                .append(Component.text(actualMovementSkip).color(NamedTextColor.GRAY))
                .append(O_PREFIX.color(NamedTextColor.NAMES.value(color)))
                .append(Component.text(offset));

        String prefix = player.platformPlayer == null ? "null" : player.platformPlayer.getName() + " ";
        Component prefixComponent = Component.text(prefix);

        boolean thisFlag = !color.equals("gray") && !color.equals("green");
        if (enabledFlags) {
            if (lastMovementIsFlag) {
                this.predicted.clear();
                this.actually.clear();
                this.offset.clear();
            }
            this.predicted.add(p);
            this.actually.add(a);
            this.offset.add(o);
            lastMovementIsFlag = thisFlag;
        }

        if (thisFlag) {
            for (int i = 0; i < this.predicted.size(); i++) {
                player.user.sendMessage(this.predicted.get(i));
                player.user.sendMessage(this.actually.get(i));
                player.user.sendMessage(this.offset.get(i));
            }
        }

        for (GrimPlayer listener : listeners) {
            Component listenerPrefix = listener == getPlayer() ? Component.empty() : prefixComponent;
            listener.sendMessage(listenerPrefix.append(p));
            listener.sendMessage(listenerPrefix.append(a));
            listener.sendMessage(listenerPrefix.append(o));
        }

        listeners.removeIf(player -> player.platformPlayer != null && !player.platformPlayer.isOnline());

        if (outputToConsole) {
            Sender consoleSender = GrimAPI.INSTANCE.getPlatformServer().getConsoleSender();
            consoleSender.sendMessage(p);
            consoleSender.sendMessage(a);
            consoleSender.sendMessage(o);
        }
    }

    private String pickColor(double offset, double totalOffset) {
        if (player.getSetbackTeleportUtil().blockOffsets) return "gray";
        if (offset <= 0 || totalOffset <= 0) {
            return "gray";
        } else if (offset < 0.0001) {
            return "green";
        } else if (offset < 0.01) {
            return "yellow";
        } else {
            return "red";
        }
    }

    @Override
    public void toggleListener(GrimPlayer player) {
        if (!listeners.remove(player)) listeners.add(player);
    }

    @Override
    public boolean toggleConsoleOutput() {
        this.outputToConsole = !outputToConsole;
        return this.outputToConsole;
    }
}
