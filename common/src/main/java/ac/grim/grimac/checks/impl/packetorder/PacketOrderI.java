package ac.grim.grimac.checks.impl.packetorder;

import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.api.storage.verbose.VerboseSchema;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import ac.grim.grimac.utils.nmsutil.BlockBreakSpeed;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;

import java.util.ArrayDeque;

@CheckData(name = "PacketOrderI", stableKey = "grim.packetorder.input_tick_order", experimental = true, verboseVersion = 1)
public class PacketOrderI extends Check implements PostPredictionCheck {
    public static final VerboseSchema V = VerboseSchema.of(
            "type:vi",
            "attacking:bool",
            "rightClicking:bool",
            "picking:bool",
            "releasing:bool",
            "digging:bool");

    static final int TYPE_INTERACT = 0;
    static final int TYPE_PLACE_USE = 1;
    static final int TYPE_RELEASE = 2;
    static final int TYPE_ATTACK = 3;

    public PacketOrderI(final GrimPlayer player) {
        super(player);
    }

    private boolean exemptPlacingWhileDigging;

    private boolean setback;
    private boolean digging; // for placing
    private final ArrayDeque<FlagData> flags = new ArrayDeque<>();

    static String typeName(int type) {
        return switch (type) {
            case TYPE_INTERACT -> "interact";
            case TYPE_PLACE_USE -> "place/use";
            case TYPE_RELEASE -> "release";
            case TYPE_ATTACK -> "attack";
            default -> "unknown";
        };
    }

    static String verbose(
            int type,
            boolean attacking,
            boolean rightClicking,
            boolean picking,
            boolean releasing,
            boolean digging) {
        return switch (type) {
            case TYPE_INTERACT, TYPE_PLACE_USE ->
                    "type=" + typeName(type) + ", releasing=" + releasing + ", digging=" + digging;
            case TYPE_RELEASE ->
                    "type=release, attacking=" + attacking
                            + ", rightClicking=" + rightClicking
                            + ", picking=" + picking
                            + ", digging=" + digging;
            case TYPE_ATTACK ->
                    "type=attack, rightClicking=" + rightClicking
                            + ", picking=" + picking
                            + ", releasing=" + releasing
                            + ", digging=" + digging;
            default -> "type=unknown";
        };
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            if (new WrapperPlayClientInteractEntity(event).getAction() == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
                onAttack(event);
            } else if (player.packetOrderProcessor.isReleasing() || player.packetOrderProcessor.isDigging()) {
                boolean releasing = player.packetOrderProcessor.isReleasing();
                boolean digging = player.packetOrderProcessor.isDigging();
                if (!player.canSkipTicks()) {
                    if (flagAndAlert(V.write(verbose())
                            .vi(TYPE_INTERACT)
                            .bool(false)
                            .bool(false)
                            .bool(false)
                            .bool(releasing)
                            .bool(digging)) && shouldModifyPackets()) {
                        event.setCancelled(true);
                        player.onPacketCancel();
                    }
                } else {
                    flags.add(new FlagData(TYPE_INTERACT, false, false, false, releasing, digging));
                }
            }
        }

        if (event.getPacketType() == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT || event.getPacketType() == PacketType.Play.Client.USE_ITEM) {
            if (player.packetOrderProcessor.isReleasing() || digging) {
                boolean releasing = player.packetOrderProcessor.isReleasing();
                if (!player.canSkipTicks()) {
                    if (flagAndAlert(V.write(verbose())
                            .vi(TYPE_PLACE_USE)
                            .bool(false)
                            .bool(false)
                            .bool(false)
                            .bool(releasing)
                            .bool(digging)) && shouldModifyPackets()) {
                        event.setCancelled(true);
                        player.onPacketCancel();
                    }
                } else {
                    flags.add(new FlagData(TYPE_PLACE_USE, false, false, false, releasing, digging));
                }
            }
        }

        if (event.getPacketType() == PacketType.Play.Client.ATTACK || event.getPacketType() == PacketType.Play.Client.SPECTATE_ENTITY) {
            onAttack(event);
        }

        if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
            WrapperPlayClientPlayerDigging packet = new WrapperPlayClientPlayerDigging(event);

            switch (packet.getAction()) {
                case STAB:
                    onAttack(event);
                    break;
                case RELEASE_USE_ITEM:
                    if (player.packetOrderProcessor.isAttackingOrStabbing() || player.packetOrderProcessor.isRightClicking() || player.packetOrderProcessor.isPicking() || player.packetOrderProcessor.isDigging()) {
                        boolean attacking = player.packetOrderProcessor.isAttackingOrStabbing();
                        boolean rightClicking = player.packetOrderProcessor.isRightClicking();
                        boolean picking = player.packetOrderProcessor.isPicking();
                        boolean digging = player.packetOrderProcessor.isDigging();
                        if (!player.canSkipTicks()) {
                            if (flagAndAlert(V.write(verbose())
                                    .vi(TYPE_RELEASE)
                                    .bool(attacking)
                                    .bool(rightClicking)
                                    .bool(picking)
                                    .bool(false)
                                    .bool(digging))) {
                                setback = true;
                            }
                        } else {
                            flags.add(new FlagData(TYPE_RELEASE, attacking, rightClicking, picking, false, digging));
                            setback = true;
                        }
                    }
                    break;
                case START_DIGGING:
                    double damage = BlockBreakSpeed.getBlockDamage(player, player.compensatedWorld.getBlock(packet.getBlockPosition()));
                    if (damage >= 1 || damage <= 0 && player.gamemode == GameMode.CREATIVE) {
                        return;
                    }
                case CANCELLED_DIGGING, FINISHED_DIGGING:
                    if (exemptPlacingWhileDigging || player.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_7_10)) {
                        return;
                    }
                    digging = true;
            }
        }

        if (!player.cameraEntity.isSelf() || isTickPacket(event.getPacketType())) {
            digging = false;
        }
    }

    @Override
    public void onPredictionComplete(PredictionComplete predictionComplete) {
        if (!player.canSkipTicks()) {
            if (setback) {
                setbackIfAboveSetbackVL();
                setback = false;
            }
            return;
        }

        if (player.isTickingReliablyFor(3)) {
            for (FlagData data : flags) {
                if (flagAndAlert(V.write(verbose())
                        .vi(data.type())
                        .bool(data.attacking())
                        .bool(data.rightClicking())
                        .bool(data.picking())
                        .bool(data.releasing())
                        .bool(data.digging())) && setback) {
                    setbackIfAboveSetbackVL();
                    setback = false;
                }
            }
        }

        flags.clear();
        setback = false;
    }

    private void onAttack(PacketReceiveEvent event) {
        if (player.packetOrderProcessor.isRightClicking() || player.packetOrderProcessor.isPicking() || player.packetOrderProcessor.isReleasing() || player.packetOrderProcessor.isDigging()) {
            boolean rightClicking = player.packetOrderProcessor.isRightClicking();
            boolean picking = player.packetOrderProcessor.isPicking();
            boolean releasing = player.packetOrderProcessor.isReleasing();
            boolean digging = player.packetOrderProcessor.isDigging();
            if (!player.canSkipTicks()) {
                if (flagAndAlert(V.write(verbose())
                        .vi(TYPE_ATTACK)
                        .bool(false)
                        .bool(rightClicking)
                        .bool(picking)
                        .bool(releasing)
                        .bool(digging)) && shouldModifyPackets()) {
                    event.setCancelled(true);
                    player.onPacketCancel();
                }
            } else {
                flags.add(new FlagData(TYPE_ATTACK, false, rightClicking, picking, releasing, digging));
            }
        }
    }

    @Override
    public void onReload(ConfigManager config) {
        exemptPlacingWhileDigging = config.getBooleanElse(getConfigName() + ".exempt-placing-while-digging", false);
    }

    private record FlagData(
            int type,
            boolean attacking,
            boolean rightClicking,
            boolean picking,
            boolean releasing,
            boolean digging) {
    }
}
